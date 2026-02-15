package au.com.addstar.comp.lobby.commands;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

/**
 * Command to list criteria for a competition
 * Usage: /compadmin criteria list <compid>
 */
public class CriteriaListCommand implements ICommand {
	private final CompBackendManager backend;
	
	public CriteriaListCommand(CompBackendManager backend) {
		this.backend = backend;
	}
	
	@Override
	public String getName() {
		return "list";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.criteria.list";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <compid>";
	}

	@Override
	public String getDescription() {
		return "Lists all criteria for a competition";
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
		
		String compIdStr = args[0];
		int compId;
		try {
			compId = Integer.parseInt(compIdStr);
		} catch (NumberFormatException e) {
			throw new BadArgumentException(0, "Invalid competition ID: " + compIdStr);
		}
		
		// Load competition and list criteria with IDs
		Bukkit.getScheduler().runTaskAsynchronously(LobbyPlugin.instance, () -> {
			try {
				Competition comp = backend.load(compId);
				if (comp == null) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Competition with ID " + compId + " not found");
					});
					return;
				}
				
				// Get criteria with IDs from database
				java.util.List<Map.Entry<Integer, CompBackendManager.CriterionInfo>> criteria = backend.getCriteriaWithIds(compId);
				
				java.util.List<String> criteriaLines = new java.util.ArrayList<>();
				for (Map.Entry<Integer, CompBackendManager.CriterionInfo> entry : criteria) {
					int criteriaId = entry.getKey();
					CompBackendManager.CriterionInfo info = entry.getValue();
					
					String description = info.getDescription();
					if (description == null || description.isEmpty()) {
						description = "(no description)";
					}
					
					criteriaLines.add(ChatColor.translateAlternateColorCodes('&', 
						String.format("&7- &f#%d &e%s &7(&e%s&7): &7%s", 
							criteriaId, info.getName(), info.getType(), description))
					);
				}
				
				final java.util.List<String> finalLines = criteriaLines;
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.GOLD + "Criteria for Competition #" + compId + " (" + comp.getTheme() + "):");
					if (finalLines.isEmpty()) {
						sender.sendMessage(ChatColor.ITALIC + "No criteria defined");
					} else {
						for (String line : finalLines) {
							sender.sendMessage(line);
						}
					}
				});
			} catch (SQLException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Failed to load criteria: " + e.getMessage());
				});
			}
		});
		
		return true;
	}
	

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Suggest competition IDs - could load from database, but for now return null
			return null;
		}
		return null;
	}
}
