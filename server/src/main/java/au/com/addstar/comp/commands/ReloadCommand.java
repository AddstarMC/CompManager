package au.com.addstar.comp.commands;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.notifications.NotificationManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

public class ReloadCommand implements ICommand {
	private final CompManager manager;
	private final NotificationManager notificationManager;
	
	public ReloadCommand(CompManager manager, NotificationManager notificationManager) {
		this.manager = manager;
		this.notificationManager = notificationManager;
	}
	
	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.reload";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label;
	}

	@Override
	public String getDescription() {
		return "Reloads the current comp";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		manager.reloadCurrentComp();
		
		sender.sendMessage(ChatColor.GOLD + "Reloaded Current Competition");
		
		try {
			notificationManager.reload();
		} catch (IOException e) {
			e.printStackTrace();
			sender.sendMessage(ChatColor.RED + "Failed to reload notifications. Check console");
		}
		
		// TODO: Kick out people that shouldnt be there
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
