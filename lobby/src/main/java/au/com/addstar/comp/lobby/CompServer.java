package au.com.addstar.comp.lobby;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.redis.RedisManager;

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
	
	CompServer(String serverId, Plugin plugin, RedisManager redis, CompBackendManager backend) {
		this.plugin = plugin;
		this.redis = redis;
		this.backend = backend;
		this.serverId = serverId;
	}
	
	/**
	 * Gets the id of this server
	 * @return The id
	 */
	public String getId() {
		return serverId;
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
	 * @throws SQLException Thrown if an error occurs in the backend
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
		
		return Futures.transform(future, new Function<String, Boolean>() {
			@Override
			public Boolean apply(String input) {
				return Boolean.valueOf(input);
			}
		});
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
		
		return Futures.transform(future, new Function<String, Integer>() {
			@Override
			public Integer apply(String input) {
				return Integer.valueOf(input);
			}
		});
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
}
