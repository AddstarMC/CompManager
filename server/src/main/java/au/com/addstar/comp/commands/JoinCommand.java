package au.com.addstar.comp.commands;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.confirmations.Confirmation;
import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.entry.EnterHandler;
import au.com.addstar.comp.entry.EntryDeniedException;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.util.Messages;

public class JoinCommand implements TabExecutor {
	private final CompManager manager;
	private final ConfirmationManager confirmationManager;
	private final Messages messages;
	
	public JoinCommand(CompManager manager, ConfirmationManager confirmationManager, Messages messages) {
		this.manager = manager;
		this.confirmationManager = confirmationManager;
		this.messages = messages;
	}
	
	public void registerAs(PluginCommand command) {
		command.setExecutor(this);
		command.setTabCompleter(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(messages.get("command.require.player"));
			return true;
		}
		
		
		try {
			EnterHandler handler = manager.enterComp((Player)sender);
			
			Competition comp = manager.getCurrentComp();
			// Show the comp information
			sender.sendMessage(messages.get("join.prompt.header", "theme", comp.getTheme()));
			sender.sendMessage(messages.get("join.prompt.theme", "theme", comp.getTheme()));
			
			// Display prize
			if (comp.getFirstPrize() != null ) {
				String firstPrize = comp.getFirstPrize().toHumanReadable();
				String secondPrize;
				if (comp.getSecondPrize() != null) {
					secondPrize = comp.getSecondPrize().toHumanReadable();
				} else {
					secondPrize = "none";
				}
				
				sender.sendMessage(messages.get("join.prompt.prize", "prize1", firstPrize, "prize2", secondPrize));
			}
			sender.sendMessage(messages.get("join.prompt.ends", "time", CompUtils.formatDate(comp.getEndDate()), "timeleft", CompUtils.formatTimeRemaining(comp.getEndDate() - System.currentTimeMillis())));
			// Display criteria
			if (!comp.getCriteria().isEmpty()) {
				sender.sendMessage(messages.get("join.prompt.criteria.header"));
				for (BaseCriterion criterion : comp.getCriteria()) {
					sender.sendMessage(messages.get("join.prompt.criteria.format", "name", criterion.getName(), "description", criterion.getDescription()));
				}
			}
			
			sender.sendMessage(messages.get("join.prompt.footer", "theme", comp.getTheme()));
			
			Confirmation<EnterHandler> confirmation = Confirmation.builder(handler)
					.expiresIn(20, TimeUnit.SECONDS)
					.withAcceptMessage(messages.get("join.done", "theme", comp.getTheme()))
					.withExpireMessage(messages.get("join.denied.timeout"))
					.withRequiredToken(manager.getCurrentComp().getTheme())
					.withTokenFailMessage(messages.get("join.denied.token"))
					.build();
			
			confirmationManager.addConfirmation((Player)sender, confirmation);
		} catch (EntryDeniedException e) {
			switch (e.getReason()) {
			case NotRunning:
				sender.sendMessage(messages.get("join.denied.not-running"));
				break;
			case AlreadyEntered:
				sender.sendMessage(messages.get("join.denied.already-entered"));
				break;
			case Full:
				sender.sendMessage(messages.get("join.denied.full"));
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}
}
