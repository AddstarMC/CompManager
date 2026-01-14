package au.com.addstar.comp.commands;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.notifications.NotificationManager;
import au.com.addstar.comp.services.PlotBackupService;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.whitelist.WhitelistHandler;
import au.com.addstar.monolith.command.RootCommandDispatcher;

public final class CompAdminCommand extends RootCommandDispatcher {
	public CompAdminCommand(WhitelistHandler whitelist, CompManager manager, NotificationManager notificationManager, ConfirmationManager confirmations, PlotBackupService backupService, Messages messages) {
		super("Gives access to all comp administration commands");
		
		registerCommand(new WhitelistCommand(whitelist));
		registerCommand(new ReloadCommand(manager, notificationManager));
		registerCommand(new StateCommand(manager));
		registerCommand(new TallyVotesCommand(manager, confirmations));
		registerCommand(new BackupCommand(manager, backupService, messages));
	}
}
