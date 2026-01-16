package au.com.addstar.comp.lobby.commands;

import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;
import com.google.common.collect.Maps;

/**
 * Command to list competitions
 * Usage: /compadmin comp list [--all]
 */
public class CompListCommand implements ICommand {
	private final CompManager manager;
	private final CompBackendManager backend;
	
	public CompListCommand(CompManager manager, CompBackendManager backend) {
		this.manager = manager;
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
		return "comp.admin.comp.list";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " [--all]";
	}

	@Override
	public String getDescription() {
		return "Lists competitions (use --all to show all competitions in database)";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		CommandFlagParser parser = CommandFlagParser.parse(args);
		boolean showAll = parser.hasFlag("all");
		
		if (showAll) {
			// Show all competitions from database
			Bukkit.getScheduler().runTaskAsynchronously(LobbyPlugin.instance, () -> {
				try {
					List<Competition> allComps = backend.getAll();
					
					// Build map of server assignments
					Map<Integer, String> serverAssignments = Maps.newHashMap();
					Collection<CompServer> servers = manager.getServers();
					for (CompServer server : servers) {
						Competition comp = server.getCurrentComp();
						if (comp != null) {
							serverAssignments.put(comp.getCompId(), server.getId());
						}
					}
					
					final Map<Integer, String> finalAssignments = serverAssignments;
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.GOLD + "All Competitions:");
						if (allComps.isEmpty()) {
							sender.sendMessage(ChatColor.ITALIC + "None");
						} else {
							for (Competition comp : allComps) {
								String stateString = formatState(comp);
								String assignment = finalAssignments.containsKey(comp.getCompId()) 
									? " on " + ChatColor.YELLOW + finalAssignments.get(comp.getCompId())
									: ChatColor.GRAY + " (unassigned)";
								
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
									String.format("&7- [%s&7] &f#%d &7&o%s%s", 
										stateString, comp.getCompId(), comp.getTheme(), assignment))
								);
							}
						}
					});
				} catch (SQLException e) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Failed to load competitions: " + e.getMessage());
					});
				}
			});
		} else {
			// Show only assigned competitions (original behavior)
			Collection<CompServer> servers = manager.getServers();
			
			Map<String, Competition> comps = Maps.newHashMap();
			for (CompServer server : servers) {
				if (server.getCurrentComp() != null) {
					comps.put(server.getId(), server.getCurrentComp());
				}
			}
			
			sender.sendMessage(ChatColor.GOLD + "Competitions:");
			if (comps.isEmpty()) {
				sender.sendMessage(ChatColor.ITALIC + "None");
			} else {
				for (Entry<String, Competition> entry : comps.entrySet()) {
					String stateString = formatState(entry.getValue());
					
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
						String.format("&7- [%s&7] &f#%d &7&o%s &7on &e%s", 
							stateString, entry.getValue().getCompId(), entry.getValue().getTheme(), entry.getKey()))
					);
				}
			}
		}
		
		return true;
	}
	
	private String formatState(Competition comp) {
		switch (comp.getState()) {
		case Open:
			return ChatColor.GREEN + "Open";
		case Voting:
			return ChatColor.YELLOW + "Voting";
		case Visit:
			return ChatColor.YELLOW + "Visit";
		default:
		case Closed:
			return ChatColor.RED + "Closed";
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1 && args[0].startsWith("--")) {
			return au.com.addstar.monolith.Monolith.matchStrings(args[0].substring(2), 
				java.util.Arrays.asList("all"));
		}
		return null;
	}
}
