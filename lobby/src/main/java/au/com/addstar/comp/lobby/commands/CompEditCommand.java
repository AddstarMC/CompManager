package au.com.addstar.comp.lobby.commands;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.comp.prizes.BasePrize;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

/**
 * Command to edit an existing competition
 * Usage: /compadmin comp edit <compid> [--theme <theme>] [--startdate <date>] [--enddate <date>] 
 *        [--voteend <date>] [--maxentrants <number>] [--votetype <type>] [--state <state>] 
 *        [--firstprize <prize>] [--secondprize <prize>] [--participationprize <prize>]
 * Note: String values with spaces can be quoted using single (') or double (") quotes.
 * Example: --theme "My Competition Theme"
 */
public class CompEditCommand implements ICommand {
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private final CompManager manager;
	private final CompBackendManager backend;
	private final RedisManager redis;
	
	public CompEditCommand(CompManager manager, CompBackendManager backend, RedisManager redis) {
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
		return "comp.admin.comp.edit";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <compid> [--theme <theme>] [--startdate <date>] [--enddate <date>] [--voteend <date>] [--maxentrants <number>] [--votetype <type>] [--state <state>] [--firstprize <prize>] [--secondprize <prize>] [--participationprize <prize>]";
	}

	@Override
	public String getDescription() {
		return "Edits an existing competition. String values with spaces can be quoted using single (') or double (\") quotes.";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
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
		
		// Load existing competition
		Bukkit.getScheduler().runTaskAsynchronously(LobbyPlugin.instance, () -> {
			try {
				Competition comp = backend.load(compId);
				if (comp == null) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Competition with ID " + compId + " not found");
					});
					return;
				}
				
				// Parse and apply flags
				try {
					applyFlags(parser, comp);
				} catch (IllegalArgumentException e) {
					Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
						sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
					});
					return;
				}
				
				// Save to database
				backend.update(comp);
				
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
					sender.sendMessage(ChatColor.GREEN + "Competition #" + compId + " updated successfully");
				});
			} catch (SQLException e) {
				Bukkit.getScheduler().runTask(LobbyPlugin.instance, () -> {
					sender.sendMessage(ChatColor.RED + "Failed to update competition: " + e.getMessage());
				});
			}
		});
		
		return true;
	}
	
	private void applyFlags(CommandFlagParser parser, Competition comp) throws IllegalArgumentException {
		// Apply theme if specified
		String theme = parser.getFlag("theme");
		if (theme != null) {
			comp.setTheme(theme);
		}
		
		// Apply dates if specified
		String startDateStr = parser.getFlag("startdate");
		if (startDateStr != null) {
			comp.setStartDate(parseDate(startDateStr));
		}
		
		String endDateStr = parser.getFlag("enddate");
		if (endDateStr != null) {
			comp.setEndDate(parseDate(endDateStr));
		}
		
		String voteEndStr = parser.getFlag("voteend");
		if (voteEndStr != null) {
			comp.setVoteEndDate(parseDate(voteEndStr));
		}
		
		// Apply max entrants if specified
		String maxEntrantsStr = parser.getFlag("maxentrants");
		if (maxEntrantsStr != null) {
			try {
				int maxEntrants = Integer.parseInt(maxEntrantsStr);
				if (maxEntrants <= 0) {
					throw new IllegalArgumentException("Max entrants must be positive");
				}
				comp.setMaxEntrants(maxEntrants);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid max entrants: " + maxEntrantsStr);
			}
		}
		
		// Apply vote type if specified
		String voteType = parser.getFlag("votetype");
		if (voteType != null) {
			comp.setVotingStrategy(voteType);
		}
		
		// Apply state if specified
		String stateStr = parser.getFlag("state");
		if (stateStr != null) {
			CompState state = parseState(stateStr);
			if (state == null) {
				comp.setAutoState();
			} else {
				comp.setState(state);
			}
		}
		
		// Apply prizes if specified
		String firstPrizeStr = parser.getFlag("firstprize");
		if (firstPrizeStr != null) {
			comp.setFirstPrize(parsePrize(firstPrizeStr));
		}
		
		String secondPrizeStr = parser.getFlag("secondprize");
		if (secondPrizeStr != null) {
			comp.setSecondPrize(parsePrize(secondPrizeStr));
		}
		
		String participationPrizeStr = parser.getFlag("participationprize");
		if (participationPrizeStr != null) {
			comp.setParticipationPrize(parsePrize(participationPrizeStr));
		}
		
		CompUtils.validateCompetitionDates(comp);
	}
	
	private long parseDate(String dateStr) throws IllegalArgumentException {
		try {
			LocalDateTime dateTime = LocalDateTime.parse(dateStr, DATE_FORMATTER);
			return java.sql.Timestamp.valueOf(dateTime).getTime();
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd HH:mm:ss");
		}
	}
	
	private CompState parseState(String stateStr) throws IllegalArgumentException {
		switch (stateStr.toLowerCase()) {
		case "open":
			return CompState.Open;
		case "closed":
		case "close":
			return CompState.Closed;
		case "voting":
		case "vote":
			return CompState.Voting;
		case "visit":
		case "visiting":
			return CompState.Visit;
		case "auto":
			return null; // Auto state
		default:
			throw new IllegalArgumentException("Unknown state: " + stateStr + ". Should be open, closed, voting, visit, or auto");
		}
	}
	
	private BasePrize parsePrize(String prizeStr) throws IllegalArgumentException {
		try {
			return BasePrize.parsePrize(prizeStr);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid prize format: " + e.getMessage());
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Suggest competition IDs - could load from database, but for now return null
			return null;
		}
		
		// Suggest flags
		String lastArg = args[args.length - 1];
		if (lastArg.startsWith("--")) {
			String flagPrefix = lastArg.substring(2).toLowerCase();
			java.util.List<String> flags = java.util.Arrays.asList(
				"theme", "startdate", "enddate", "voteend", "maxentrants", "votetype", 
				"state", "firstprize", "secondprize", "participationprize"
			);
			return au.com.addstar.monolith.Monolith.matchStrings(flagPrefix, flags);
		} else if (args.length >= 2 && args[args.length - 2].startsWith("--")) {
			String flagName = args[args.length - 2].substring(2).toLowerCase();
			if ("state".equals(flagName)) {
				return au.com.addstar.monolith.Monolith.matchStrings(lastArg, 
					java.util.Arrays.asList("open", "closed", "voting", "visit", "auto"));
			} else if ("votetype".equals(flagName)) {
				return au.com.addstar.monolith.Monolith.matchStrings(lastArg, 
					java.util.Arrays.asList("likedislike", "ranked"));
			}
		}
		
		return null;
	}
}
