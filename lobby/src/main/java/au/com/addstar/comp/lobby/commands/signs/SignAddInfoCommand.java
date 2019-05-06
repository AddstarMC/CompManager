package au.com.addstar.comp.lobby.commands.signs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Function;

import au.com.addstar.comp.lobby.signs.InfoSign.InfoType;
import au.com.addstar.comp.lobby.signs.BaseSign;
import au.com.addstar.comp.lobby.signs.SignManager;
import au.com.addstar.monolith.Monolith;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

public class SignAddInfoCommand implements ICommand {
	private final SignManager manager;
	
	public SignAddInfoCommand(SignManager manager) {
		this.manager = manager;
	}
	
	@Override
	public String getName() {
		return "addinfo";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return null;
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label + " <serverId> <type>";
	}

	@Override
	public String getDescription() {
		return "Adds an info sign for the specified server.";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.of(CommandSenderType.Player);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if (args.length != 2) {
			return false;
		}
		
		String serverId = args[0];
		InfoType type;
		
		switch (args[1].toLowerCase()) {
		case "plotsleft":
		case "plotsremaining":
			type = InfoType.PlotsLeft;
			break;
		case "plotsused":
			type = InfoType.PlotsUsedTotal;
			break;
		case "timeend":
			type = InfoType.TimeEnd;
			break;
		case "timeleft":
		case "timeremaining":
			type = InfoType.TimeLeft;
			break;
		case "timeboth":
			type = InfoType.TimeEndLeft;
			break;
		default:
			BadArgumentException e = new BadArgumentException(1, "Unknown info sign type");
			e.addInfo("Available options: plotsleft, plotsused, timeend, timeleft, timeboth");
			throw e;
		}
		
		manager.addPendingSign((Player)sender, makeCreationFunction(serverId, type));
		sender.sendMessage(ChatColor.GRAY + "Left click the sign you want to use");
		
		return true;
	}
	
	private Function<Block, BaseSign> makeCreationFunction(final String serverId, final InfoType type) {
		return new Function<Block, BaseSign>() {
			@Override
			public BaseSign apply(Block block) {
				return manager.makeInfoSign(serverId, type, block);
			}
		};
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		if (args.length == 2) {
			return Monolith.matchStrings(args[1], Arrays.asList("PlotsLeft", "PlotsUsed", "TimeLeft", "TimeEnd", "TimeBoth"));
		}
		
		return null;
	}
}
