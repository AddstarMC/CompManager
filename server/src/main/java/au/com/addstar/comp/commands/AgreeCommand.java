package au.com.addstar.comp.commands;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import au.com.addstar.comp.confirmations.ConfirmationManager;

public class AgreeCommand implements TabExecutor {
	private final ConfirmationManager confirmationManager;
	
	public AgreeCommand(ConfirmationManager confirmationManager) {
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
		
		Player player = (Player)sender;
		
		if (confirmationManager.hasPendingConfirmations(player)) {
			confirmationManager.tryConfirm(player, StringUtils.join(args, ' '));
		} else {
			// TODO: Customizable messages
			sender.sendMessage("Placeholder: nothing to confirm");
		}
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}
}
