package au.com.addstar.comp.lobby.commands.signs;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.signs.SignManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandDispatcher;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

public class SignCommand extends CommandDispatcher implements ICommand {
	public SignCommand(SignManager manager, CompManager compManager) {
		super("Provides access to sign commands");
		
		registerCommand(new SignAddInfoCommand(manager));
		registerCommand(new SignAddJoinCommand(manager));
		registerCommand(new SignAddVisitCommand(manager));
	}
	
	@Override
	public String getName() {
		return "sign";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.sign";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <command> ...";
	}

	@Override
	public String getDescription() {
		return "Provides access to sign commands";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.of(CommandSenderType.Player);
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
