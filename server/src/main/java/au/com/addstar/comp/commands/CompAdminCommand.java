package au.com.addstar.comp.commands;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.notifications.NotificationManager;
import au.com.addstar.comp.whitelist.WhitelistHandler;
import au.com.addstar.monolith.command.RootCommandDispatcher;

public final class CompAdminCommand extends RootCommandDispatcher {
	public CompAdminCommand(WhitelistHandler whitelist, CompManager manager, NotificationManager notificationManager) {
		super("Gives access to all comp administration commands");
		
		registerCommand(new WhitelistCommand(whitelist));
		registerCommand(new ReloadCommand(manager, notificationManager));
		registerCommand(new StateCommand(manager));
	}
}
