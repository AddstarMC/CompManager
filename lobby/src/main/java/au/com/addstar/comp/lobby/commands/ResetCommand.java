package au.com.addstar.comp.lobby.commands;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

/**
 * Command for triggering plot resets on remote competition servers via Redis.
 */
public class ResetCommand implements ICommand {
	private final CompManager manager;
	private final RedisManager redis;
	private final Messages messages;
	
	public ResetCommand(CompManager manager, RedisManager redis, Messages messages) {
		this.manager = manager;
		this.redis = redis;
		this.messages = messages;
	}
	
	@Override
	public String getName() {
		return "reset";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.reset";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <serverId>";
	}

	@Override
	public String getDescription() {
		return "Triggers a plot reset on the specified competition server";
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
			sender.sendMessage(messages.get("reset.server-not-found")
				.replace("{serverId}", args[0]));
			return true;
		}
		
		// Check if server is online
		if (!server.isOnline()) {
			sender.sendMessage(messages.get("reset.server-offline")
				.replace("{serverId}", server.getId()));
			return true;
		}
		
		// Check if server has a competition
		if (server.getCurrentComp() == null) {
			sender.sendMessage(messages.get("reset.no-competition")
				.replace("{serverId}", server.getId()));
			return true;
		}
		
		// Send reset command via Redis
		redis.sendCommand(server.getId(), "reset");
		sender.sendMessage(messages.get("reset.command-sent")
			.replace("{serverId}", server.getId())
			.replace("{theme}", server.getCurrentComp().getTheme())
			.replace("{compId}", String.valueOf(server.getCurrentComp().getCompId())));
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		// Could provide server ID autocomplete here if needed
		return null;
	}
}
