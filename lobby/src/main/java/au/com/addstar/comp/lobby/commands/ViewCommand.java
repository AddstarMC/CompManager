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
import au.com.addstar.comp.lobby.services.CompetitionViewService;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

/**
 * Command for viewing/visiting build competitions.
 * Usage: /comp view <serverId|compId> [playerName]
 * Alias: visit
 */
public class ViewCommand implements ICommand {
	private static final String PERMISSION_VIEW = "comp.view";
	private static final String PERMISSION_VIEW_OTHERS = "comp.view.others";
	
	private final CompManager manager;
	private final CompetitionViewService viewService;
	private final Messages messages;
	
	public ViewCommand(CompManager manager, CompetitionViewService viewService, Messages messages) {
		this.manager = manager;
		this.viewService = viewService;
		this.messages = messages;
	}
	
	@Override
	public String getName() {
		return "view";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "visit" };
	}

	@Override
	public String getPermission() {
		// Permission is checked dynamically based on whether playerName is provided
		return PERMISSION_VIEW;
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		if (sender.hasPermission(PERMISSION_VIEW_OTHERS)) {
			return label + " <serverId|compId> [playerName]";
		}
		return label + " <serverId|compId>";
	}

	@Override
	public String getDescription() {
		return "View a build competition without joining it";
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
		boolean viewingOthers = args.length >= 2;
		
		if (viewingOthers) {
			// Viewing for another player - requires comp.view.others permission
			if (!sender.hasPermission(PERMISSION_VIEW_OTHERS)) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to send other players to view a competition");
				return true;
			}
			
			// Resolve target player (must be online for view to work)
			targetPlayer = Bukkit.getPlayer(args[1]);
			if (targetPlayer == null) {
				sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
				return true;
			}
		} else {
			// Viewing self - requires comp.view permission and must be a player
			if (!sender.hasPermission(PERMISSION_VIEW)) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to view competitions.");
				return true;
			}
			
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "Only players can view competitions. Use '" + label + " <serverId|compId> <playerName>' to send other players to view a competition.");
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
		
		// Attempt to view the competition
		boolean success = viewService.viewCompetition(targetPlayer, server);
		
		// Provide feedback to sender
		if (viewingOthers) {
			if (success) {
				sender.sendMessage(ChatColor.GREEN + "Sent " + targetPlayer.getName() + " to view the competition.");
			} else {
				sender.sendMessage(ChatColor.RED + "Failed to send " + targetPlayer.getName() + " to view the competition.");
			}
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
		} else if (args.length == 2 && sender.hasPermission(PERMISSION_VIEW_OTHERS)) {
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
