package au.com.addstar.comp.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.entry.EnterHandler;
import au.com.addstar.comp.entry.EntryDeniedException;

public class JoinCommand implements TabExecutor {
	private final CompManager manager;
	
	public JoinCommand(CompManager manager) {
		this.manager = manager;
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
			// TODO: Add confirmation and rule acceptance
			handler.complete();
			// TODO: Customize messages
			sender.sendMessage("Placeholder: You have joined the comp");
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
