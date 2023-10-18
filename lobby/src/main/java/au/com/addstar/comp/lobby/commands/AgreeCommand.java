package au.com.addstar.comp.lobby.commands;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.util.Messages;

public class AgreeCommand implements TabExecutor {
	private final ConfirmationManager confirmationManager;
	private final Messages messages;
	
	public AgreeCommand(ConfirmationManager confirmationManager, Messages messages) {
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
			sender.sendMessage(ChatColor.RED + "Only players are allowed to use this");
			return true;
		}
		
		Player player = (Player)sender;
		
		if (confirmationManager.hasPendingConfirmations(player)) {
			confirmationManager.tryConfirm(player, StringUtils.join(args, ' '));
		} else {
			sender.sendMessage(messages.get("confirm.none"));
		}
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}
}
