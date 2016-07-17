package au.com.addstar.comp.lobby.commands;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.command.CommandSender;

import com.google.common.collect.Maps;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

public class ListCommand implements ICommand {
	private final CompManager manager;
	
	public ListCommand(CompManager manager) {
		this.manager = manager;
	}
	
	@Override
	public String getName() {
		return "list";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.list";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label;
	}

	@Override
	public String getDescription() {
		return "Lists all set competitions";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		Collection<CompServer> servers = manager.getServers();
		
		Map<String, Competition> comps = Maps.newHashMap();
		for (CompServer server : servers) {
			if (server.getCurrentComp() != null) {
				comps.put(server.getId(), server.getCurrentComp());
			}
		}
		
		sender.sendMessage(ChatColor.GOLD + "Competitions:");
		if (comps.isEmpty()) {
			sender.sendMessage(ChatColor.ITALIC + "None");
		} else {
			for (Entry<String, Competition> entry : comps.entrySet()) {
				String stateString;
				switch (entry.getValue().getState()) {
				case Open:
					stateString = ChatColor.GREEN + "Open";
					break;
				case Voting:
					stateString = ChatColor.YELLOW + "Voting";
					break;
				case Visit:
					stateString = ChatColor.YELLOW + "Visit";
					break;
				default:
				case Closed:
					stateString = ChatColor.RED + "Closed";
					break;
				}
				
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
					String.format("&7- [%s&7] &f#%d &7&o%s &7on &e%s", stateString, entry.getValue().getCompId(), entry.getValue().getTheme(), entry.getKey()))
				);
			}
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
