package au.com.addstar.comp.lobby.commands;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

/**
 * Command for triggering plot backups on remote competition servers via Redis.
 */
public class BackupCommand implements ICommand {
	private final CompManager manager;
	private final RedisManager redis;
	
	public BackupCommand(CompManager manager, RedisManager redis) {
		this.manager = manager;
		this.redis = redis;
	}
	
	@Override
	public String getName() {
		return "backup";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.backup";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <serverId>";
	}

	@Override
	public String getDescription() {
		return "Triggers a plot backup on the specified competition server";
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
		
		// Resolve server by identifier (server ID or comp ID)
		CompServer server = manager.findServerByIdentifier(args[0]);
		if (server == null) {
			sender.sendMessage(ChatColor.RED + "Server not found: " + args[0]);
			return true;
		}
		
		// Check if server is online
		if (!server.isOnline()) {
			sender.sendMessage(ChatColor.RED + "Server " + server.getId() + " is currently offline.");
			return true;
		}
		
		// Check if server has a competition
		if (server.getCurrentComp() == null) {
			sender.sendMessage(ChatColor.RED + "Server " + server.getId() + " does not have an active competition.");
			return true;
		}
		
		// Send backup command via Redis
		redis.sendCommand(server.getId(), "backup");
		sender.sendMessage(ChatColor.GOLD + "Backup command sent to server " + server.getId() + 
			" for competition: " + server.getCurrentComp().getTheme() + " (ID: " + server.getCurrentComp().getCompId() + ")");
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		// Could provide server ID autocomplete here if needed
		return null;
	}
}
