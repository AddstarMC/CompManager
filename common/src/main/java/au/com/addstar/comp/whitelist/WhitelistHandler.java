package au.com.addstar.comp.whitelist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import au.com.addstar.comp.database.ConnectionHandler;
import au.com.addstar.comp.database.ConnectionPool;
import au.com.addstar.comp.database.StatementKey;

/**
 * Provides access to check and change whitelist state for players
 */
public class WhitelistHandler {
	private static final String TABLE = "whitelist";
	
	private static final StatementKey STATEMENT_GET;
	private static final StatementKey STATEMENT_ADD;
	private static final StatementKey STATEMENT_REMOVE;
	
	static {
		STATEMENT_GET = new StatementKey("SELECT 1 FROM `" + TABLE + "` WHERE `uuid`=? LIMIT 1;");
		STATEMENT_ADD = new StatementKey("INSERT INTO `" + TABLE + "` VALUES(?);");
		STATEMENT_REMOVE = new StatementKey("DELETE FROM `" + TABLE + "` WHERE `uuid`=?;");
	}
	
	private final ConnectionPool pool;
	
	public WhitelistHandler(ConnectionPool pool) {
		this.pool = pool;
	}
	
	/**
	 * Removes the dashes in the uuid output
	 * @param id The input uuid
	 * @return The string equivalent version
	 */
	private String idToString(UUID id) {
		return id.toString().replace("-", "");
	}
	
	/**
	 * Checks if a player uuid is on the whitelist for comp
	 * @param playerId The uuid of the player being checked
	 * @return True if they are whitelisted
	 * @throw SQLException Thrown if an SQLException occurs when querying the database
	 */
	public boolean isWhitelisted(UUID playerId) throws SQLException {
		ConnectionHandler handler = pool.getConnection();
		try {
			try (ResultSet result = handler.executeQuery(STATEMENT_GET, idToString(playerId))) {
				return result.next();
			}
		} finally {
			handler.release();
		}
	}
	
	/**
	 * Checks if a player is on the whitelist for comp
	 * @param player The player to check
	 * @return True if they are whitelisted
	 * @throws SQLException Thrown if an error occurs when querying the database
	 */
	public boolean isWhitelisted(OfflinePlayer player) throws SQLException {
		return isWhitelisted(player.getUniqueId());
	}
	
	/**
	 * Adds a player to the whitelist
	 * @param playerId The uuid of the player
	 * @throws SQLException Thrown if an error occurs when querying the database
	 */
	public void add(UUID playerId) throws SQLException {
		ConnectionHandler handler = pool.getConnection();
		try {
			handler.executeUpdate(STATEMENT_ADD, idToString(playerId));
		} finally {
			handler.release();
		}
	}
	
	/**
	 * Adds a player to the whitelist
	 * @param player The player to add
	 * @throws SQLException Thrown if an error occurs when querying the database
	 */
	public void add(OfflinePlayer player) throws SQLException {
		add(player.getUniqueId());
	}
	
	/**
	 * Removes a player from the whitelist
	 * @param playerId The uuid of the player
	 * @throws SQLException Thrown if an error occurs when querying the database
	 */
	public void remove(UUID playerId) throws SQLException {
		ConnectionHandler handler = pool.getConnection();
		try {
			handler.executeUpdate(STATEMENT_REMOVE, idToString(playerId));
		} finally {
			handler.release();
		}
	}
	
	/**
	 * Removes a player from the whitelist
	 * @param player The player to remove
	 * @throws SQLException Thrown if an error occurs when querying the database
	 */
	public void remove(OfflinePlayer player) throws SQLException {
		remove(player.getUniqueId());
	}
}
