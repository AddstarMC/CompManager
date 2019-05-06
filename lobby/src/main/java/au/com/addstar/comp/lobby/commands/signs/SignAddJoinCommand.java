package au.com.addstar.comp.lobby.commands.signs;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Function;

import au.com.addstar.comp.lobby.signs.BaseSign;
import au.com.addstar.comp.lobby.signs.SignManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;

public class SignAddJoinCommand implements ICommand {
	private final SignManager manager;
	
	public SignAddJoinCommand(SignManager manager) {
		this.manager = manager;
	}
	
	@Override
	public String getName() {
		return "addjoin";
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
		return label + " <serverId>";
	}

	@Override
	public String getDescription() {
		return "Adds a join sign for the specified server.";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.of(CommandSenderType.Player);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if (args.length != 1) {
			return false;
		}
		
		String serverId = args[0];
		
		manager.addPendingSign((Player)sender, makeCreationFunction(serverId));
		sender.sendMessage(ChatColor.GRAY + "Left click the sign you want to use");
		
		return true;
	}
	
	private Function<Block, BaseSign> makeCreationFunction(final String serverId) {
		return new Function<Block, BaseSign>() {
			@Override
			public BaseSign apply(Block block) {
				return manager.makeJoinSign(serverId, block);
			}
		};
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
