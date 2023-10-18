package au.com.addstar.comp.lobby.entry;

import java.util.UUID;

import au.com.addstar.comp.lobby.LobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.confirmations.Confirmable;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.Messages;

public class RemoteEnterHandler implements Confirmable {
	private final CompServer server;
	private final UUID playerId;
	private final RedisManager redisManager;
	private final Messages messages;
	
	public RemoteEnterHandler(CompServer server, UUID playerId, RedisManager redisManager, Messages messages) {
		this.server = server;
		this.playerId = playerId;
		this.redisManager = redisManager;
		this.messages = messages;
	}
	
	@Override
	public void confirm() {
		ListenableFuture<String> future = redisManager.query(server.getId(), "join_confirm", playerId.toString());
		
		Futures.addCallback(future, new FutureCallback<String>() {
			@Override
			public void onSuccess(String returnValue) {
				boolean success = Boolean.parseBoolean(returnValue);
				
				Player player = Bukkit.getPlayer(playerId);
				if (player == null) {
					return;
				}
				
				if (success) {
					// Teleport the player to the plot
					
					player.sendMessage(messages.get("join.done", "theme", server.getCurrentComp().getTheme()));
					server.send(player);
					// TODO: Teleport too
				} else {
					player.sendMessage(messages.get("join.denied.timeout"));
				}
			}
			@Override
			public void onFailure(@NotNull Throwable throwable) {
				throwable.printStackTrace();
			}
		}, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
	}

	@Override
	public void abort() {
		redisManager.query(server.getId(), "join_abort", playerId.toString());
	}
}
