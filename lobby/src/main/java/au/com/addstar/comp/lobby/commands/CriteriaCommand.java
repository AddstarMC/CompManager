package au.com.addstar.comp.lobby.commands;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandDispatcher;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

/**
 * Command dispatcher for criteria management commands
 */
public class CriteriaCommand extends CommandDispatcher implements ICommand {
	public CriteriaCommand(CompManager manager, CompBackendManager backend, RedisManager redis) {
		super("Provides access to criteria management commands");
		
		registerCommand(new CriteriaAddCommand(manager, backend, redis));
		registerCommand(new CriteriaEditCommand(manager, backend, redis));
		registerCommand(new CriteriaDeleteCommand(manager, backend, redis));
		registerCommand(new CriteriaListCommand(backend));
	}
	
	@Override
	public String getName() {
		return "criteria";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.criteria";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <command> ...";
	}

	@Override
	public String getDescription() {
		return "Provides access to criteria management commands";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		return dispatchCommand(sender, parent, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return tabComplete(sender, parent, label, args);
	}
}
