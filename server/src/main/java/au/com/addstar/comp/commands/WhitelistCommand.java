package au.com.addstar.comp.commands;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.whitelist.WhitelistHandler;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.PlayerDefinition;
import net.md_5.bungee.api.ChatColor;

final class WhitelistCommand implements ICommand {
	private final WhitelistHandler handler;
	
	public WhitelistCommand(WhitelistHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public String getName() {
		return "whitelist";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.whitelist.admin";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <add|remove|check> <player>";
	}

	@Override
	public String getDescription() {
		return "Provides manual control over the whitelist";
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
		
		// Select command
		FutureCallback<PlayerDefinition> handler;
		switch (args[0].toLowerCase()) {
		case "add":
			handler = new AddHandler(sender);
			break;
		case "remove":
			handler = new RemoveHandler(sender);
			break;
		case "check":
			handler = new CheckHandler(sender);
			break;
		default:
			sender.sendMessage(ChatColor.RED + "Unknown option " + args[0]);
			return true;
		}
		
		// Parse the player
		ListenableFuture<PlayerDefinition> future = Lookup.lookupPlayerName(args[1]);
		Futures.addCallback(future, handler);
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		List<String> results = Lists.newArrayList();
		String input = args[args.length-1].toLowerCase();
		
		switch (args.length) {
		case 1: // Option
			if ("add".startsWith(input)) {
				results.add("add");
			}
			if ("remove".startsWith(input)) {
				results.add("remove");
			}
			if ("check".startsWith(input)) {
				results.add("check");
			}
			break;
		case 2: // Player
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getName().toLowerCase().startsWith(input)) {
					results.add(player.getName());
				}
			}
			break;
		}
		
		return results;
	}
	
	/**
	 * Base handler for all commands
	 */
	private abstract class BaseHandler implements FutureCallback<PlayerDefinition> {
		protected final CommandSender sender;
		public BaseHandler(CommandSender sender) {
			this.sender = sender;
		}
		
		@Override
		public void onFailure(Throwable error) {
			sender.sendMessage(ChatColor.RED + "Unable to lookup player: " + error.getMessage());
			System.err.println("Failed to lookup player:");
			error.printStackTrace();
		}
	}
	
	/**
	 * Handler for adding players to the whitelist
	 */
	private class AddHandler extends BaseHandler {
		public AddHandler(CommandSender sender) {
			super(sender);
		}
		
		@Override
		public void onSuccess(PlayerDefinition player) {
			try {
				handler.add(player.getUniqueId());
				sender.sendMessage(ChatColor.GREEN + player.getName() + " has been added to the whitelist");
			} catch (SQLException e) {
				sender.sendMessage(ChatColor.RED + "An error occured writing to the whitelist");
				System.err.println("Failed to add " + player.getName() + " to the comp whitelist:");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Handler for removing players from the whitelist
	 */
	private class RemoveHandler extends BaseHandler {
		public RemoveHandler(CommandSender sender) {
			super(sender);
		}
		
		@Override
		public void onSuccess(PlayerDefinition player) {
			try {
				handler.remove(player.getUniqueId());
				sender.sendMessage(ChatColor.GREEN + player.getName() + " has been removed from the whitelist");
			} catch (SQLException e) {
				sender.sendMessage(ChatColor.RED + "An error occured writing to the whitelist");
				System.err.println("Failed to remove " + player.getName() + " from the comp whitelist:");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Handler for checking if players are on the whitelist
	 */
	private class CheckHandler extends BaseHandler {
		public CheckHandler(CommandSender sender) {
			super(sender);
		}
		
		@Override
		public void onSuccess(PlayerDefinition player) {
			try {
				if (handler.isWhitelisted(player.getUniqueId())) {
					sender.sendMessage(player.getName() + " is whitelisted");
				} else {
					sender.sendMessage(player.getName() + " is not whitelisted");
				}
			} catch (SQLException e) {
				sender.sendMessage(ChatColor.RED + "An error occured reading from the whitelist");
				System.err.println("Failed to check the comp whitelist:");
				e.printStackTrace();
			}
		}
	}
}
