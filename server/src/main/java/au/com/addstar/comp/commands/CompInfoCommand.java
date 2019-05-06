package au.com.addstar.comp.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.util.Messages;

public class CompInfoCommand implements CommandExecutor {
	private final CompManager compManager;
	private final Messages messages;
	
	public CompInfoCommand(CompManager compManager, Messages messages) {
		this.compManager = compManager;
		this.messages = messages;
	}
	
	public void registerAs(PluginCommand command) {
		command.setExecutor(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Competition comp = compManager.getCurrentComp();
		
		if (comp == null) {
			sender.sendMessage(messages.get("info.none"));
			return true;
		}
		
		String stateString = messages.get("state." + comp.getState().name().toLowerCase());
		long timeEnd = compManager.getTimeEnd();
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
}
