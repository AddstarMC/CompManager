package au.com.addstar.comp.lobby.commands;

import org.bukkit.plugin.Plugin;

import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.commands.signs.SignCommand;
import au.com.addstar.comp.lobby.services.CompetitionDialogService;
import au.com.addstar.comp.lobby.services.CompetitionJoinService;
import au.com.addstar.comp.lobby.services.CompetitionViewService;
import au.com.addstar.comp.lobby.signs.SignManager;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.whitelist.WhitelistHandler;
import au.com.addstar.monolith.command.RootCommandDispatcher;

public final class CompAdminCommand extends RootCommandDispatcher {
	public CompAdminCommand(WhitelistHandler whitelist, CompManager manager, RedisManager redis, SignManager signManager, Messages messages, ConfirmationManager confirmationManager, Plugin plugin) {
		super("Gives access to all comp administration commands");
		
		// Create services for commands
		CompetitionJoinService joinService = new CompetitionJoinService(confirmationManager, messages);
		CompetitionViewService viewService = new CompetitionViewService(messages);
		CompetitionDialogService dialogService = new CompetitionDialogService();
		
		registerCommand(new WhitelistCommand(whitelist));
		registerCommand(new ListCommand(manager));
		registerCommand(new ReloadCommand(manager));
		registerCommand(new ReloadServersCommand(manager, redis));
		registerCommand(new CompDebugCommand(redis));
		registerCommand(new SignCommand(signManager, manager));
		registerCommand(new InfoCommand(manager, messages));
		registerCommand(new StateCommand(manager, messages));
		registerCommand(new JoinCommand(manager, joinService, messages));
		registerCommand(new ViewCommand(manager, viewService, messages));
		registerCommand(new DialogCommand(manager, dialogService, joinService, viewService, messages, plugin));
		registerCommand(new BackupCommand(manager, redis, messages));
		registerCommand(new ResetCommand(manager, redis, messages));
	}
}
