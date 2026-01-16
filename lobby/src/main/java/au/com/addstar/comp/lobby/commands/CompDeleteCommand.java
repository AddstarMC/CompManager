package au.com.addstar.comp.lobby.commands;

import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

/**
 * Command to delete a competition
 * Usage: /compadmin comp delete <compid> [--confirm]
 */
public class CompDeleteCommand implements ICommand {
	private final CompManager manager;
	private final CompBackendManager backend;
	private final RedisManager redis;
	
	public CompDeleteCommand(CompManager manager, CompBackendManager backend, RedisManager redis) {
		this.manager = manager;
		this.backend = backend;
		this.redis = redis;
	}
	
	@Override
	public String getName() {
		return "delete";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.comp.delete";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <compid> [--confirm]";
	}

	@Override
	public String getDescription() {
		return "Deletes a competition (requires --confirm flag)";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if (args.length < 1) {
			return false;
		}
		
		CommandFlagParser parser = CommandFlagParser.parse(args);
		
		// Competition ID is required as first positional argument
		String compIdStr = parser.getPositionalArg(0);
		if (compIdStr == null) {
			return false;
		}
		
		int compId;
		try {
			compId = Integer.parseInt(compIdStr);
		} catch (NumberFormatException e) {
			throw new BadArgumentException(0, "Invalid competition ID: " + compIdStr);
		}
		
		// Check for confirmation
		if (!parser.hasFlag("confirm")) {
			sender.sendMessage(ChatColor.RED + "This will permanently delete competition #" + compId);
			sender.sendMessage(ChatColor.YELLOW + "To confirm, use: " + label + " " + compId + " --confirm");
			return true;
		}
		
		// Check if competition is assigned to any server
		Bukkit.getScheduler().runTaskAsynchronously(LobbyPlugin.instance, () -> {
			try {
				Competition comp = backend.load(compId);
				if (comp == null) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Competition with ID " + compId + " not found");
					});
					return;
				}
				
				// Find servers with this competition
				Collection<CompServer> affectedServers = findServersWithComp(compId);
				if (!affectedServers.isEmpty()) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.YELLOW + "Competition #" + compId + " is assigned to " + affectedServers.size() + " server(s):");
						for (CompServer server : affectedServers) {
							sender.sendMessage(ChatColor.GRAY + "  - " + server.getId());
						}
						sender.sendMessage(ChatColor.RED + "Deleting...");
					});
				}
				
				// Delete the competition
				backend.delete(compId);
				
				// Reload lobby cache
				manager.reload(false);
				
				// Send reload to affected build servers
				for (CompServer server : affectedServers) {
					redis.sendCommand(server.getId(), "reload");
				}
				
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.GREEN + "Competition #" + compId + " deleted successfully");
				});
			} catch (SQLException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Failed to delete competition: " + e.getMessage());
				});
			}
		});
		
		return true;
	}
	
	private Collection<CompServer> findServersWithComp(int compId) {
		java.util.List<CompServer> servers = new java.util.ArrayList<>();
		Collection<CompServer> allServers = manager.getServers();
		for (CompServer server : allServers) {
			Competition comp = server.getCurrentComp();
			if (comp != null && comp.getCompId() == compId) {
				servers.add(server);
			}
		}
		return servers;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Suggest competition IDs - could load from database, but for now return null
			return null;
		} else if (args.length == 2 && args[1].startsWith("--")) {
			return au.com.addstar.monolith.Monolith.matchStrings(args[1].substring(2), 
				java.util.Arrays.asList("confirm"));
		}
		return null;
	}
}
