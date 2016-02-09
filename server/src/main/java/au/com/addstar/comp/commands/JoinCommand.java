package au.com.addstar.comp.commands;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.confirmations.Confirmation;
import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.entry.EnterHandler;
import au.com.addstar.comp.entry.EntryDeniedException;

public class JoinCommand implements TabExecutor {
	private final CompManager manager;
	private final ConfirmationManager confirmationManager;
	
	public JoinCommand(CompManager manager, ConfirmationManager confirmationManager) {
		this.manager = manager;
		this.confirmationManager = confirmationManager;
	}
	
	public void registerAs(PluginCommand command) {
		command.setExecutor(this);
		command.setTabCompleter(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Only players are allowed to use this");
			return true;
		}
		
		try {
			EnterHandler handler = manager.enterComp((Player)sender);
			sender.sendMessage("Placeholder: We will display comp info here as well as the rules");
			sender.sendMessage("Please use '/agree " + manager.getCurrentComp().getTheme() + "' to join the comp");
			
			// TODO: Customize messages
			Confirmation<EnterHandler> confirmation = Confirmation.builder(handler)
					.expiresIn(20, TimeUnit.SECONDS)
					.withAcceptMessage("Placeholder: You have joined the comp")
					.withExpireMessage("Placeholder: You did not accept quick enough")
					.withRequiredToken(manager.getCurrentComp().getTheme())
					.withTokenFailMessage("Placeholder: You did not enter the correct phase")
					.build();
			
			confirmationManager.addConfirmation((Player)sender, confirmation);
		} catch (EntryDeniedException e) {
			// TODO: Customize messages
			sender.sendMessage("Placeholder: denied " + e.getReason());
		}
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}
}
