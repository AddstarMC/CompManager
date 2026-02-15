package au.com.addstar.comp.lobby.commands;

import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

/**
 * Command to delete a criterion
 * Usage: /compadmin criteria delete <criteriaid> [--confirm]
 */
public class CriteriaDeleteCommand implements ICommand {
	private final CompManager manager;
	private final CompBackendManager backend;
	private final RedisManager redis;
	
	public CriteriaDeleteCommand(CompManager manager, CompBackendManager backend, RedisManager redis) {
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
		return "comp.admin.criteria.delete";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <criteriaid> [--confirm]";
	}

	@Override
	public String getDescription() {
		return "Deletes a criterion (requires --confirm flag)";
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
		
		// Criteria ID is required as first positional argument
		String criteriaIdStr = parser.getPositionalArg(0);
		if (criteriaIdStr == null) {
			return false;
		}
		
		int criteriaId;
		try {
			criteriaId = Integer.parseInt(criteriaIdStr);
		} catch (NumberFormatException e) {
			throw new BadArgumentException(0, "Invalid criteria ID: " + criteriaIdStr);
		}
		
		// Check for confirmation
		if (!parser.hasFlag("confirm")) {
			sender.sendMessage(ChatColor.RED + "This will permanently delete criterion #" + criteriaId);
			sender.sendMessage(ChatColor.YELLOW + "To confirm, use: " + label + " " + criteriaId + " --confirm");
			return true;
		}
		
		// Get criterion info before deletion to find affected servers
		Bukkit.getScheduler().runTaskAsynchronously(LobbyPlugin.instance, () -> {
			try {
				Map.Entry<Integer, BaseCriterion> entry = backend.getCriterion(criteriaId);
				if (entry == null) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Criterion with ID " + criteriaId + " not found");
					});
					return;
				}
				
				int compId = entry.getKey();
				
				// Delete the criterion
				backend.deleteCriterion(criteriaId);
				
				// Reload lobby cache
				manager.reload(false);
				
				// Send reload to affected build servers
				Collection<CompServer> servers = manager.getServers();
				for (CompServer server : servers) {
					Competition serverComp = server.getCurrentComp();
					if (serverComp != null && serverComp.getCompId() == compId) {
						redis.sendCommand(server.getId(), "reload");
					}
				}
				
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.GREEN + "Criterion #" + criteriaId + " deleted successfully");
				});
			} catch (SQLException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Failed to delete criterion: " + e.getMessage());
				});
			}
		});
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Suggest criteria IDs - could load from database, but for now return null
			return null;
		} else if (args.length == 2 && args[1].startsWith("--")) {
			return au.com.addstar.monolith.Monolith.matchStrings(args[1].substring(2), 
				java.util.Arrays.asList("confirm"));
		}
		return null;
	}
}
