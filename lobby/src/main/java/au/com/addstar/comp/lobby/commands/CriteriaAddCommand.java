package au.com.addstar.comp.lobby.commands;

import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.criterions.BlockIncludeCriterion;
import au.com.addstar.comp.criterions.TextCriterion;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

/**
 * Command to add a criterion to a competition
 * Usage: /compadmin criteria add <compid> <name> <type> [--description <desc>] [--data <data>]
 */
public class CriteriaAddCommand implements ICommand {
	private final CompManager manager;
	private final CompBackendManager backend;
	private final RedisManager redis;
	
	public CriteriaAddCommand(CompManager manager, CompBackendManager backend, RedisManager redis) {
		this.manager = manager;
		this.backend = backend;
		this.redis = redis;
	}
	
	@Override
	public String getName() {
		return "add";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.criteria.add";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <compid> <name> <type> [--description <desc>] [--data <data>]";
	}

	@Override
	public String getDescription() {
		return "Adds a criterion to a competition";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		CommandFlagParser parser = CommandFlagParser.parse(args);
		
		// Competition ID, name, and type are required as positional arguments
		String compIdStr = parser.getPositionalArg(0);
		String name = parser.getPositionalArg(1);
		String typeStr = parser.getPositionalArg(2);
		
		if (compIdStr == null || name == null || typeStr == null) {
			return false;
		}
		
		int compId;
		try {
			compId = Integer.parseInt(compIdStr);
		} catch (NumberFormatException e) {
			throw new BadArgumentException(0, "Invalid competition ID: " + compIdStr);
		}
		
		// Validate and create criterion
		BaseCriterion criterion = createCriterion(typeStr, parser);
		if (criterion == null) {
			throw new BadArgumentException(2, "Invalid criterion type: " + typeStr + ". Valid types: text, block, includeBlock");
		}
		
		criterion.setName(name);
		
		String description = parser.getFlag("description");
		if (description != null) {
			criterion.setDescription(description);
		}
		
		// Load competition and add criterion
		Bukkit.getScheduler().runTaskAsynchronously(LobbyPlugin.instance, () -> {
			try {
				Competition comp = backend.load(compId);
				if (comp == null) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Competition with ID " + compId + " not found");
					});
					return;
				}
				
				// Add criterion to database
				int criteriaId = backend.addCriterion(comp, criterion);
				
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
					sender.sendMessage(ChatColor.GREEN + "Criterion added successfully with ID: " + criteriaId);
					sender.sendMessage(ChatColor.GRAY + "Name: " + criterion.getName() + ", Type: " + typeStr);
				});
			} catch (SQLException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Failed to add criterion: " + e.getMessage());
				});
			} catch (IllegalArgumentException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
				});
			}
		});
		
		return true;
	}
	
	private BaseCriterion createCriterion(String type, CommandFlagParser parser) throws IllegalArgumentException {
		String normalizedType = type.toLowerCase();
		BaseCriterion criterion;
		
		switch (normalizedType) {
		case "text":
			criterion = new TextCriterion();
			break;
		case "block":
		case "includeblock":
			criterion = new BlockIncludeCriterion();
			// For block criteria, data is required (JSON format)
			String data = parser.getFlag("data");
			if (data != null) {
				criterion.load(data);
			}
			break;
		default:
			return null;
		}
		
		return criterion;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Suggest competition IDs - could load from database, but for now return null
			return null;
		} else if (args.length == 2) {
			// Suggest criterion name
			return null;
		} else if (args.length == 3) {
			// Suggest criterion types
			return au.com.addstar.monolith.Monolith.matchStrings(args[2], 
				java.util.Arrays.asList("text", "block", "includeBlock"));
		} else if (args.length >= 4 && args[args.length - 1].startsWith("--")) {
			// Suggest flags
			String flagPrefix = args[args.length - 1].substring(2).toLowerCase();
			return au.com.addstar.monolith.Monolith.matchStrings(flagPrefix, 
				java.util.Arrays.asList("description", "data"));
		}
		return null;
	}
}
