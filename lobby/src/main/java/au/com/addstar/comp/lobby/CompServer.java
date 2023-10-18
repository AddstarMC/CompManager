package au.com.addstar.comp.lobby;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.confirmations.Confirmable;
import au.com.addstar.comp.entry.EntryDeniedException;
import au.com.addstar.comp.lobby.entry.RemoteEnterFuture;
import au.com.addstar.comp.redis.QueryException;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.Messages;

/**
 * Provides access to query data from a
 * competition server.
 */
public class CompServer {
	private final Plugin plugin;
	private final RedisManager redis;
	private final CompBackendManager backend;
	
	private final String serverId;
	Competition currentComp;
	private boolean isOnline;
	
	CompServer(String serverId, Plugin plugin, RedisManager redis, CompBackendManager backend) {
		this.plugin = plugin;
		this.redis = redis;
		this.backend = backend;
		this.serverId = serverId;
		
		isOnline = false;
	}
	
	/**
	 * Gets the id of this server
	 * @return The id
	 */
	public String getId() {
		return serverId;
	}
	
	/**
	 * Checks the online state of this server.
	 * @return True if the server is online
	 */
	public boolean isOnline() {
		return isOnline;
	}
	
	/**
	 * Sets the online state of this server. false by default
	 * @param isOnline True if the server is online
	 */
	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}
	
	/**
	 * Gets the currently selected competition for this server.
	 * This comp may or may not be running
	 * @return The current comp
	 */
	public Competition getCurrentComp() {
		return currentComp;
	}
	
	/**
	 * Updates the comp settings and notifies the server
	 */
	public void updateComp() {
		if (currentComp == null) {
			return;
		}
		
		try {
			backend.update(currentComp);
			redis.sendCommand(serverId, "reload");
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to update comp for " + serverId, e);
		}
	}
	
	/**
	 * Reloads this servers comp info
	 */
	public void reload() {
		try {
			currentComp = null;
			
			Optional<Integer> compId = backend.getCompID(serverId);
			if (compId.isPresent()) {
				Competition comp = backend.load(compId.get());
				if (comp != null) {
					currentComp = comp;
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to load comp info for server " + serverId, e);
		}
	}
	
	/**
	 * Checks if a player is entered into this servers active comp
	 * @param playerId The uuid of the player to check
	 * @return A future that returns true if they are entered
	 */
	public ListenableFuture<Boolean> isEntrant(UUID playerId) {
		ListenableFuture<String> future = redis.query(serverId, "is_entrant", playerId.toString());
		
		return Futures.transform(future, Boolean::valueOf, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
	}
	
	/**
	 * Checks if a player is entered into this servers active comp
	 * @param player The player to check
	 * @return A future that returns true if they are entered
	 */
	public ListenableFuture<Boolean> isEntrant(OfflinePlayer player) {
		return isEntrant(player.getUniqueId());
	}
	
	/**
	 * Gets the number of entrants in this comp
	 * @return A future that returns the number of entrants
	 */
	public ListenableFuture<Integer> getEntrantCount() {
		ListenableFuture<String> future = redis.query(serverId, "entrant_count");
		
		return Futures.transform(future, Integer::valueOf, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
	}
	
	/**
	 * Pings the server to see if it is online
	 * @return A Future
	 */
	public ListenableFuture<?> ping() {
		return redis.query(serverId, "ping");
	}
	
	/**
	 * Sends a player to this server (if able)
	 * @param player The player to send
	 */
	public void send(Player player) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(serverId);
		
		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}
	
	/**
	 * Attempts to join a player into a comp
	 * @param player The player to join into the comp
	 * @param messages The messages object for displaying status to the player
	 * @return A Future to get the confirmation thing, or the error
	 * @throws EntryDeniedException Thrown through the Future. Thrown if the player is unable to join the comp
	 * @throws QueryException Thrown through the Future. Thrown if there is a redis error
	 */
	public ListenableFuture<Confirmable> joinComp(OfflinePlayer player, Messages messages) {
		ListenableFuture<String> rawFuture = redis.query(serverId, "join_begin", player.getUniqueId().toString());
		
		RemoteEnterFuture future = new RemoteEnterFuture(this, player.getUniqueId(), redis, messages);
		Futures.addCallback(rawFuture, future, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
		
		return future;
	}
}
