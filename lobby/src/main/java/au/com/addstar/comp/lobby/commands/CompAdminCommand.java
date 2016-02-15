package au.com.addstar.comp.lobby.commands;

import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.commands.signs.SignCommand;
import au.com.addstar.comp.lobby.signs.SignManager;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.whitelist.WhitelistHandler;
import au.com.addstar.monolith.command.RootCommandDispatcher;

public final class CompAdminCommand extends RootCommandDispatcher {
	public CompAdminCommand(WhitelistHandler whitelist, CompManager manager, RedisManager redis, SignManager signManager) {
		super("Gives access to all comp administration commands");
		
		registerCommand(new WhitelistCommand(whitelist));
		registerCommand(new ListCommand(manager));
		registerCommand(new ReloadCommand(manager));
		registerCommand(new CompDebugCommand(redis));
		registerCommand(new SignCommand(signManager, manager));
		registerCommand(new InfoCommand(manager));
	}
}
