package au.com.addstar.comp.lobby.commands;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.services.CompetitionJoinService;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

/**
 * Command for joining build competitions.
 * Usage: /comp join <serverId|compId> [playerName]
 */
public class JoinCommand implements ICommand {
	private static final String PERMISSION_JOIN = "comp.join";
	private static final String PERMISSION_JOIN_OTHERS = "comp.join.others";
	
	private final CompManager manager;
	private final CompetitionJoinService joinService;
	private final Messages messages;
	
	public JoinCommand(CompManager manager, CompetitionJoinService joinService, Messages messages) {
		this.manager = manager;
		this.joinService = joinService;
		this.messages = messages;
	}
	
	@Override
	public String getName() {
		return "join";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		// Permission is checked dynamically based on whether playerName is provided
		return PERMISSION_JOIN;
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		if (sender.hasPermission(PERMISSION_JOIN_OTHERS)) {
			return label + " <serverId|compId> [playerName]";
		}
		return label + " <serverId|compId>";
	}

	@Override
	public String getDescription() {
		return "Join a build competition as a participant and creates your plot if you don't have one";
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
		
		// Determine target player
		Player targetPlayer;
		boolean joiningOthers = args.length >= 2;
		
		if (joiningOthers) {
			// Joining another player - requires comp.join.others permission
			if (!sender.hasPermission(PERMISSION_JOIN_OTHERS)) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to join other players.");
				return true;
			}
			
			// Resolve target player (must be online for join to work)
			targetPlayer = Bukkit.getPlayer(args[1]);
			if (targetPlayer == null) {
				sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
				return true;
			}
		} else {
			// Joining self - requires comp.join permission and must be a player
			if (!sender.hasPermission(PERMISSION_JOIN)) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to join competitions.");
				return true;
			}
			
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "Only players can join competitions. Use '" + label + " <serverId|compId> <playerName>' to join other players.");
				return true;
			}
			
			targetPlayer = (Player) sender;
		}
		
		// Resolve server by identifier (server ID or comp ID)
		CompServer server = manager.findServerByIdentifier(args[0]);
		if (server == null) {
			sender.sendMessage(messages.get("info.unknown-server"));
			return true;
		}
		
		if (server.getCurrentComp() == null) {
			sender.sendMessage(messages.get("info.none"));
			return true;
		}
		
		// Initiate join process
		// The service handles all validation, error messages, and async callbacks
		joinService.initiateJoin(targetPlayer, server);
		
		// If joining others, notify the sender
		if (joiningOthers) {
			sender.sendMessage(ChatColor.GREEN + "Initiated join process for " + targetPlayer.getName() + ".");
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			// Tab complete server IDs and comp IDs
			List<String> completions = new ArrayList<>();
			
			// Add server IDs
			completions.addAll(manager.getServerIds());
			
			// Add comp IDs
			for (CompServer server : manager.getServers()) {
				if (server.getCurrentComp() != null) {
					completions.add(String.valueOf(server.getCurrentComp().getCompId()));
				}
			}
			
			// Filter by what the user has typed
			String input = args[0].toLowerCase();
			completions.removeIf(completion -> !completion.toLowerCase().startsWith(input));
			
			return completions;
		} else if (args.length == 2 && sender.hasPermission(PERMISSION_JOIN_OTHERS)) {
			// Tab complete player names
			List<String> completions = new ArrayList<>();
			String input = args[1].toLowerCase();
			
			for (Player player : Bukkit.getOnlinePlayers()) {
				String name = player.getName();
				if (name.toLowerCase().startsWith(input)) {
					completions.add(name);
				}
			}
			
			return completions;
		}
		
		return null;
	}
}
