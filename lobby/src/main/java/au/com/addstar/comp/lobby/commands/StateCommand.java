package au.com.addstar.comp.lobby.commands;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.Monolith;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

public class StateCommand implements ICommand {
	private final CompManager manager;
	private final Messages messages;
	
	public StateCommand(CompManager manager, Messages messages) {
		this.manager = manager;
		this.messages = messages;
	}
	
	@Override
	public String getName() {
		return "state";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.state";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <server> <state>";
	}

	@Override
	public String getDescription() {
		return "Changes the state of a competition";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if (args.length != 2) {
			return false;
		}
		
		CompServer server = manager.getServer(args[0]);
		if (server == null) {
			sender.sendMessage(messages.get("info.unknown-server"));
			return true;
		}
		
		Competition comp = server.getCurrentComp();
		if (comp == null) {
			sender.sendMessage(ChatColor.RED + "There is no active comp on this server.");
			return true;
		}
		
		// Parse the new state
		CompState state;
		switch (args[1].toLowerCase()) {
		case "open":
			state = CompState.Open;
			break;
		case "close":
		case "closed":
			state = CompState.Closed;
			break;
		case "vote":
		case "voting":
			state = CompState.Voting;
			break;
		case "visit":
		case "visiting":
			state = CompState.Visit;
			break;
		case "auto":
			state = null;
			break;
		default:
			throw new BadArgumentException(1, "Unknown state. Should be open, closed, voting, visit, or auto");
		}
		
		boolean save = false;
		// Automatic state
		if (state == null) {
			if (comp.isAutomatic()) {
				sender.sendMessage(ChatColor.GREEN + "This comp is already in automatic mode on " + server.getId());
			} else {
				comp.setAutoState();
				save = true;
				sender.sendMessage(ChatColor.GREEN + "Updated state to Automatic on " + server.getId());
			}
		} else {
			if (!comp.isAutomatic() && comp.getState() == state) {
				sender.sendMessage(ChatColor.GREEN + "This comp is already set to " + state.name() + " on " + server.getId());
			} else {
				comp.setState(state);
				save = true;
				sender.sendMessage(ChatColor.GREEN + "Updated state to " + state.name() + " on " + server.getId());
			}
		}
		
		if (save) {
			server.updateComp();
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 1) {
			return Monolith.matchStrings(args[0], manager.getServerIds());
		} else if (args.length == 2) {
			return Monolith.matchStrings(args[1], Arrays.asList("open", "close", "closed", "vote", "voting", "visit", "visiting", "auto"));
		}
		
		return null;
	}
}
