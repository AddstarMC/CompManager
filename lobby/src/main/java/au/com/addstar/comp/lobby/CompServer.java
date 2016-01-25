package au.com.addstar.comp.lobby;

import java.util.UUID;

import org.bukkit.OfflinePlayer;

import au.com.addstar.comp.Competition;

/**
 * Provides access to query data from a
 * competition server.
 */
public class CompServer {
	/**
	 * Gets the id of this server
	 * @return The id
	 */
	public String getId() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * Gets the currently selected competition for this server.
	 * This comp may or may not be running
	 * @return The current comp
	 */
	public Competition getCurrentComp() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * Checks if a player is entered into this servers active comp
	 * @param playerId The uuid of the player to check
	 * @return True if they are entered
	 */
	public boolean isEntrant(UUID playerId) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * Checks if a player is entered into this servers active comp
	 * @param player The player to check
	 * @return True if they are entered
	 */
	public boolean isEntrant(OfflinePlayer player) {
		return isEntrant(player.getUniqueId());
	}
}
