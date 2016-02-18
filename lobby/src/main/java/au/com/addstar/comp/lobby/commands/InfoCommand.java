package au.com.addstar.comp.lobby.commands;

import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.command.CommandSender;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

public class InfoCommand implements ICommand {
	private final CompManager manager;
	
	public InfoCommand(CompManager manager) {
		this.manager = manager;
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

	private final SimpleDateFormat endFormat = new SimpleDateFormat("d MMM h:ma");
	private static final String TIME_LEFT_FORMAT_LONG = "d'd 'H'h'";
	private static final String TIME_LEFT_FORMAT_SHORT = "'H'h 'm'm'";
	
	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if (args.length < 1) {
			return false;
		}
		
		CompServer server = manager.getServer(args[0]);
		if (server == null) {
			sender.sendMessage(ChatColor.RED + "Unknown server or no comp running");
			return true;
		}
		
		Competition comp = server.getCurrentComp();
		
		if (comp == null) {
			sender.sendMessage(ChatColor.GRAY + "There is no current competition on " + server.getId());
			return true;
		}
		
		String stateString;
		long timeEnd;
		switch (comp.getState()) {
		case Open:
			stateString = ChatColor.GREEN.toString() + ChatColor.BOLD + "OPEN";
			timeEnd = comp.getEndDate();
			break;
		case Voting:
			stateString = ChatColor.AQUA.toString() + ChatColor.BOLD + "VOTING";
			timeEnd = -1; // TODO: Voting end time
			break;
		default:
		case Closed:
			stateString = ChatColor.RED.toString() + ChatColor.BOLD + "CLOSED";
			timeEnd = -1;
			break;
		}
		
		sender.sendMessage(ChatColor.BOLD + comp.getTheme() + " " + stateString);
		
		// Display the end time
		if (comp.isAutomatic() && timeEnd != -1) {
			String timeEndString = endFormat.format(timeEnd);
			long remaining = timeEnd - System.currentTimeMillis();
			
			String timeRemainString;
			if (remaining < TimeUnit.DAYS.toMillis(1)) {
				timeRemainString = DurationFormatUtils.formatDuration(remaining, TIME_LEFT_FORMAT_SHORT);
			} else {
				timeRemainString = DurationFormatUtils.formatDuration(remaining, TIME_LEFT_FORMAT_LONG);
			}
			
			switch (comp.getState()) {
			case Open:
				sender.sendMessage(ChatColor.GRAY + "Ends " + ChatColor.YELLOW + ChatColor.BOLD + timeEndString + ChatColor.GRAY + " (in " + ChatColor.YELLOW + ChatColor.BOLD + timeRemainString + ChatColor.GRAY + ")");
				break;
			case Voting:
				sender.sendMessage(ChatColor.GRAY + "Closes " + ChatColor.YELLOW + ChatColor.BOLD + timeEndString + ChatColor.GRAY + " (in " + ChatColor.YELLOW + ChatColor.BOLD + timeRemainString + ChatColor.GRAY + ")");
				break;
			default:
				break;
			}
		}
		
		// Display prize if any
		if (comp.getFirstPrize() != null) {
			sender.sendMessage(ChatColor.GRAY + "Prize " + ChatColor.YELLOW + ChatColor.BOLD + comp.getFirstPrize().toHumanReadable());
		}
		
		// Display criteria
		if (!comp.getCriteria().isEmpty()) {
			sender.sendMessage(ChatColor.GOLD + "Criteria:");
			for (BaseCriterion criterion : comp.getCriteria()) {
				sender.sendMessage("\u25C9 " + ChatColor.YELLOW + criterion.getName());
				if (criterion.getDescription() != null) {
					sender.sendMessage("  " + ChatColor.GRAY + ChatColor.ITALIC + criterion.getDescription());
				}
			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
