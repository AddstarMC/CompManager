package au.com.addstar.comp.lobby.commands;

import au.com.addstar.comp.whitelist.WhitelistHandler;
import au.com.addstar.monolith.command.RootCommandDispatcher;

public final class CompAdminCommand extends RootCommandDispatcher {
	public CompAdminCommand(WhitelistHandler whitelist) {
		super("Gives access to all comp administration commands");
		
		registerCommand(new WhitelistCommand(whitelist));
	}
}
