package au.com.addstar.comp.lobby.commands;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.services.CompetitionDialogService;
import au.com.addstar.comp.lobby.services.CompetitionJoinService;
import au.com.addstar.comp.lobby.services.CompetitionViewService;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

import com.viaversion.viaversion.api.Via;
/**
 * Command for showing competition dialogs with Join/View options.
 * Usage: /compadmin dialog <serverId|compId> [playerName]
 */
public class DialogCommand implements ICommand {
	private static final String PERMISSION_DIALOG = "comp.admin.dialog";
	private static final String PERMISSION_DIALOG_OTHERS = "comp.admin.dialog.others";
	private static final int MIN_PROTOCOL_VERSION = 771; // Minecraft 1.21.6
	
	private final CompManager manager;
	private final CompetitionDialogService dialogService;
	private final CompetitionJoinService joinService;
	private final CompetitionViewService viewService;
	private final Messages messages;
	private final Plugin plugin;
	
	public DialogCommand(CompManager manager, CompetitionDialogService dialogService, 
	                     CompetitionJoinService joinService, CompetitionViewService viewService, 
	                     Messages messages, Plugin plugin) {
		this.manager = manager;
		this.dialogService = dialogService;
		this.joinService = joinService;
		this.viewService = viewService;
		this.messages = messages;
		this.plugin = plugin;
	}
	
	@Override
	public String getName() {
		return "dialog";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		// Return null to bypass Monolith's permission check
		// All permission checks are handled in onCommand()
		return null;
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		if (sender.hasPermission(PERMISSION_DIALOG_OTHERS)) {
			return label + " <serverId|compId> [playerName]";
		}
		return label + " <serverId|compId>";
	}

	@Override
	public String getDescription() {
		return "Show a dialog to join or view a competition";
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
		boolean showingOthers = args.length >= 2;
		
		if (showingOthers) {
			// Showing dialog to another player
			if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.isOp() && !sender.hasPermission(PERMISSION_DIALOG_OTHERS)) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to show the competition dialog to other players.");
				return true;
			}
			
			// Resolve target player (must be online for dialog to work)
			targetPlayer = Bukkit.getPlayer(args[1]);
			if (targetPlayer == null) {
				sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
				return true;
			}
		} else {
			// Showing dialog to self
			if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.isOp() && !sender.hasPermission(PERMISSION_DIALOG)) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to run this command");
				return true;
			}
			
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "Only players can view the competition dialog. Use '" + label + " <serverId|compId> <playerName>' to show the competition dialog to other players.");
				return true;
			}
			
			targetPlayer = (Player) sender;
		}
		
		// Check player protocol version via ViaVersion (if available)
		if (!checkProtocolVersion(targetPlayer)) {
			targetPlayer.sendMessage(ChatColor.RED + "Please update your Minecraft version to 1.21.6 or newer to use this feature.");
			if (showingOthers) {
				sender.sendMessage(ChatColor.RED + "Player " + targetPlayer.getName() + " is on an older Minecraft version and cannot view dialogs.");
			}
			return true;
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
		
		// Create and show dialog
		try {
			var dialog = dialogService.createCompetitionDialog(
				server.getCurrentComp(), 
				server, 
				joinService, 
				viewService, 
				targetPlayer
			);
			targetPlayer.showDialog(dialog);
			
			// Provide feedback to sender if showing to others
			if (showingOthers) {
				sender.sendMessage(ChatColor.GREEN + "Showed competition dialog to " + targetPlayer.getName() + ".");
			}
		} catch (Exception e) {
			plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to show dialog to " + targetPlayer.getName(), e);
			sender.sendMessage(ChatColor.RED + "Failed to show dialog. Please try again later.");
			if (showingOthers) {
				targetPlayer.sendMessage(ChatColor.RED + "Failed to show competition dialog.");
			}
		}
		
		return true;
	}

	/**
	 * Checks if the player's protocol version is at least the minimum required version.
	 * If ViaVersion is not available, assumes the player is on a compatible version.
	 * 
	 * @param player The player to check
	 * @return True if the player's version is compatible, false otherwise
	 */
	private boolean checkProtocolVersion(Player player) {
		try {
			// Check if ViaVersion is available
			Plugin viaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion");
			if (viaVersion == null || !viaVersion.isEnabled()) {
				// ViaVersion not present, assume compatible version
				return true;
			}
			
			@SuppressWarnings("unchecked")
			int playerVersion = ((ViaAPI<Player>) Via.getAPI()).getPlayerVersion(player);

			// Check if version meets minimum requirement (771 = 1.21.6)
			return playerVersion >= MIN_PROTOCOL_VERSION;
		} catch (Exception e) {
			// If ViaVersion API fails for any reason, assume compatible version
			// This allows the feature to work even if ViaVersion has issues
			plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to check protocol version for " + player.getName() + ", assuming compatible", e);
			return true;
		}
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
		} else if (args.length == 2 && sender.hasPermission(PERMISSION_DIALOG_OTHERS)) {
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
