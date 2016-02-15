package au.com.addstar.comp.commands;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.criterions.BaseCriterion;
import net.md_5.bungee.api.ChatColor;

public class CompInfoCommand implements CommandExecutor {
	private final CompManager compManager;
	
	public CompInfoCommand(CompManager compManager) {
		this.compManager = compManager;
	}
	
	public void registerAs(PluginCommand command) {
		command.setExecutor(this);
	}
	
	private final SimpleDateFormat endFormat = new SimpleDateFormat("d MMM h:ma");
	private static final String TIME_LEFT_FORMAT_LONG = "d'd 'H'h'";
	private static final String TIME_LEFT_FORMAT_SHORT = "'H'h 'm'm'";
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Competition comp = compManager.getCurrentComp();
		
		if (comp == null) {
			sender.sendMessage(ChatColor.GRAY + "There is no current competition");
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
		if (timeEnd != -1) {
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
		
		return true;
	}
}
