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
 * Command to edit an existing criterion
 * Usage: /compadmin criteria edit <criteriaid> [--name <name>] [--description <desc>] [--type <type>] [--data <data>]
 * Note: String values with spaces can be quoted using single (') or double (") quotes.
 * Example: --name "Details & Polish"
 */
public class CriteriaEditCommand implements ICommand {
	private final CompManager manager;
	private final CompBackendManager backend;
	private final RedisManager redis;
	
	public CriteriaEditCommand(CompManager manager, CompBackendManager backend, RedisManager redis) {
		this.manager = manager;
		this.backend = backend;
		this.redis = redis;
	}
	
	@Override
	public String getName() {
		return "edit";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.criteria.edit";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <criteriaid> [--name <name>] [--description <desc>] [--type <type>] [--data <data>]";
	}

	@Override
	public String getDescription() {
		return "Edits an existing criterion. String values with spaces can be quoted using single (') or double (\") quotes.";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
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
		
		// Load existing criterion
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
				BaseCriterion criterion = entry.getValue();
				
				// Apply flag updates
				boolean updated = false;
				try {
					updated = applyFlags(parser, criterion);
				} catch (IllegalArgumentException e) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
					});
					return;
				}
				
				if (!updated) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.YELLOW + "No changes specified");
					});
					return;
				}
				
				// Update in database
				backend.updateCriterion(criteriaId, criterion);
				
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
					sender.sendMessage(ChatColor.GREEN + "Criterion #" + criteriaId + " updated successfully");
				});
			} catch (SQLException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Failed to update criterion: " + e.getMessage());
				});
			}
		});
		
		return true;
	}
	
	private boolean applyFlags(CommandFlagParser parser, BaseCriterion criterion) throws IllegalArgumentException {
		boolean updated = false;
		
		// Update name if specified
		String name = parser.getFlag("name");
		if (name != null) {
			criterion.setName(name);
			updated = true;
		}
		
		// Update description if specified
		String description = parser.getFlag("description");
		if (description != null) {
			criterion.setDescription(description);
			updated = true;
		}
		
		// Update type if specified (requires creating new criterion instance)
		String typeStr = parser.getFlag("type");
		if (typeStr != null) {
			// Type change requires special handling - we'd need to create a new instance
			// For now, just validate the type
			String normalizedType = typeStr.toLowerCase();
			if (!normalizedType.equals("text") && !normalizedType.equals("block") && !normalizedType.equals("includeblock")) {
				throw new IllegalArgumentException("Invalid criterion type: " + typeStr);
			}
			// Note: Changing type would require more complex logic to preserve data
			// For now, we'll just validate but not actually change the type
			throw new IllegalArgumentException("Changing criterion type is not yet supported. Please delete and recreate the criterion.");
		}
		
		// Update data if specified
		String data = parser.getFlag("data");
		if (data != null) {
			criterion.load(data);
			updated = true;
		}
		
		return updated;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Suggest criteria IDs - could load from database, but for now return null
			return null;
		} else if (args.length >= 2 && args[args.length - 1].startsWith("--")) {
			// Suggest flags
			String flagPrefix = args[args.length - 1].substring(2).toLowerCase();
			return au.com.addstar.monolith.Monolith.matchStrings(flagPrefix, 
				java.util.Arrays.asList("name", "description", "type", "data"));
		} else if (args.length >= 3 && args[args.length - 2].startsWith("--")) {
			String flagName = args[args.length - 2].substring(2).toLowerCase();
			if ("type".equals(flagName)) {
				return au.com.addstar.monolith.Monolith.matchStrings(args[args.length - 1], 
					java.util.Arrays.asList("text", "block", "includeBlock"));
			}
		}
		return null;
	}
}
