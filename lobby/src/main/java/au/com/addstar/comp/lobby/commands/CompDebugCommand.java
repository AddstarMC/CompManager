package au.com.addstar.comp.lobby.commands;

import java.util.EnumSet;
import java.util.List;

import au.com.addstar.comp.lobby.LobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;

public class CompDebugCommand implements ICommand {
	private final RedisManager redis;
	
	public CompDebugCommand(RedisManager redis) {
		this.redis = redis;
	}
	
	@Override
	public String getName() {
		return "debug";
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
		return label;
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(final CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		if(args.length > 0 ) {
			sender.sendMessage("Querying " + args[0]);
			final ListenableFuture<String> future = redis.query(args[0], "entrant_count");
			Futures.addCallback(future, new FutureCallback<String>() {
				@Override
				public void onSuccess(String result) {
					sender.sendMessage("Query result: " + result);
				}

				@Override
				public void onFailure(@NotNull Throwable error) {
					sender.sendMessage("Query errored: " + error);
				}
			}, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
		} else
			return false;
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}

}
