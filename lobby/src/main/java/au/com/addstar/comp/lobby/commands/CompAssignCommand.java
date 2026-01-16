package au.com.addstar.comp.lobby.commands;

import java.sql.SQLException;
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
 * Command to assign a competition to a server
 * Usage: /compadmin comp assign <serverid> <compid>
 */
public class CompAssignCommand implements ICommand {
	private final CompManager manager;
	private final CompBackendManager backend;
	private final RedisManager redis;
	
	public CompAssignCommand(CompManager manager, CompBackendManager backend, RedisManager redis) {
		this.manager = manager;
		this.backend = backend;
		this.redis = redis;
	}
	
	@Override
	public String getName() {
		return "assign";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.comp.assign";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <serverid> <compid>";
	}

	@Override
	public String getDescription() {
		return "Assigns a competition to a server";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if (args.length != 2) {
			return false;
		}
		
		String serverId = args[0];
		String compIdStr = args[1];
		
		int compId;
		try {
			compId = Integer.parseInt(compIdStr);
		} catch (NumberFormatException e) {
			throw new BadArgumentException(1, "Invalid competition ID: " + compIdStr);
		}
		
		// Validate server exists (check if it's in the known servers or database)
		CompServer server = manager.getServer(serverId);
		if (server == null) {
			// Server might not have checked in yet, but we can still assign
			// Just warn the user
			sender.sendMessage(ChatColor.YELLOW + "Warning: Server '" + serverId + "' is not currently online. Assignment will still be saved.");
		}
		
		// Load and validate competition
		Bukkit.getScheduler().runTaskAsynchronously(LobbyPlugin.instance, () -> {
			try {
				Competition comp = backend.load(compId);
				if (comp == null) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Competition with ID " + compId + " not found");
					});
					return;
				}
				
				// Assign competition to server
				backend.setComp(serverId, comp);
				
				// Reload lobby cache to pick up the new assignment
				manager.reload(false);
				
				// Send reload command to the server
				redis.sendCommand(serverId, "reload");
				
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.GREEN + "Competition #" + compId + " (" + comp.getTheme() + ") assigned to server " + serverId);
				});
			} catch (SQLException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Failed to assign competition: " + e.getMessage());
				});
			}
		});
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Suggest server IDs
			return au.com.addstar.monolith.Monolith.matchStrings(args[0], manager.getServerIds());
		} else if (args.length == 2) {
			// Could suggest competition IDs, but for now return null
			return null;
		}
		return null;
	}
}
