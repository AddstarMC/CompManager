package au.com.addstar.comp.lobby.commands;

import java.util.EnumSet;
import java.util.List;
import org.bukkit.command.CommandSender;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

public class InfoCommand implements ICommand {
	private final CompManager manager;
	private final Messages messages;
	
	public InfoCommand(CompManager manager, Messages messages) {
		this.manager = manager;
		this.messages = messages;
	}
	
	@Override
	public String getName() {
		return "info";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.info";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <server>";
	}

	@Override
	public String getDescription() {
		return "Views information about a competition";
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
		
		CompServer server = manager.getServer(args[0]);
		if (server == null) {
			sender.sendMessage(messages.get("info.unknown-server"));
			return true;
		}
		
		Competition comp = server.getCurrentComp();
		
		if (comp == null) {
			sender.sendMessage(messages.get("info.none"));
			return true;
		}
		
		String stateString = messages.get("state." + comp.getState().name().toLowerCase());
		long timeEnd;
		switch (comp.getState()) {
		case Open:
			timeEnd = comp.getEndDate();
			break;
		case Voting:
			timeEnd = comp.getVoteEndDate();
			break;
		default:
		case Closed:
			timeEnd = -1;
			break;
		}
		
		sender.sendMessage(messages.get("info.header", "theme", comp.getTheme(), "state", stateString));
		
		// Display the end time
		if (comp.isAutomatic() && timeEnd != -1) {
			long remaining = timeEnd - System.currentTimeMillis();
			sender.sendMessage(messages.get("info.ends." + comp.getState().name().toLowerCase(), "time", CompUtils.formatDate(timeEnd), "timeleft", CompUtils.formatTimeRemaining(remaining)));
		}
		
		// Display prize if any
		if (comp.getFirstPrize() != null || comp.getSecondPrize() != null || comp.getParticipationPrize() != null) {
			sender.sendMessage(messages.get("info.prize", 
				"prize1",
				(comp.getFirstPrize() != null ? comp.getFirstPrize().toHumanReadable() : "none"),
				"prize2",
				(comp.getSecondPrize() != null ? comp.getSecondPrize().toHumanReadable() : "none"),
				"prize3",
				(comp.getParticipationPrize() != null ? comp.getParticipationPrize().toHumanReadable() : "none")
				)
			);
		}
		
		// Display criteria
		if (!comp.getCriteria().isEmpty()) {
			sender.sendMessage(messages.get("info.criteria.header"));
			for (BaseCriterion criterion : comp.getCriteria()) {
				sender.sendMessage(messages.get("info.criteria.format", "name", criterion.getName(), "description", criterion.getDescription()));
			}
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
