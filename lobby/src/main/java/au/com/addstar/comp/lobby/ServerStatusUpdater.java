package au.com.addstar.comp.lobby;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Simple periodic task to update the online status
 * of servers by pinging them
 */
public class ServerStatusUpdater implements Runnable {
	private final CompManager manager;
	
	public ServerStatusUpdater(CompManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void run() {
		for (final CompServer server : manager.getServers()) {
			ListenableFuture<?> future = server.ping();
			Futures.addCallback(future, new FutureCallback<Object>() {
				@Override
				public void onSuccess(Object error) {
					server.setOnline(true);
				}
				
				@Override
				public void onFailure(@NotNull Throwable error) {
					server.setOnline(false);
				}
			}, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
		}
	}
}
