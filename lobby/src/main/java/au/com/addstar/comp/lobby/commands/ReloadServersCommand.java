package au.com.addstar.comp.lobby.commands;

import java.util.Collection;
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
 * Command for triggering reloads on all build servers via Redis.
 */
public class ReloadServersCommand implements ICommand {
	private final CompManager manager;
	private final RedisManager redis;
	
	public ReloadServersCommand(CompManager manager, RedisManager redis) {
		this.manager = manager;
		this.redis = redis;
	}
	
	@Override
	public String getName() {
		return "reloadservers";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.reloadservers";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label;
	}

	@Override
	public String getDescription() {
		return "Reloads all build servers (sends reload command to each server)";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		Collection<CompServer> servers = manager.getServers();
		
		if (servers.isEmpty()) {
			sender.sendMessage(ChatColor.YELLOW + "No build servers found.");
			return true;
		}
		
		int reloadedCount = 0;
		for (CompServer server : servers) {
			redis.sendCommand(server.getId(), "reload");
			reloadedCount++;
		}
		
		sender.sendMessage(ChatColor.GOLD + "Sent reload command to " + reloadedCount + " build server(s).");
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
