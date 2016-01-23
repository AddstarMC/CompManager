package au.com.addstar.comp.database;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class DatabaseManager {
	private final Plugin plugin;
	private ConnectionPool pool;
	private BukkitTask expireTask;
	
	public DatabaseManager(Plugin plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Initializes the database connection pool and tests it
	 * @throws IOException Thrown if the connection cannot be initialized
	 */
	public void initialize() throws IOException {
		ConfigurationSection config = plugin.getConfig().getConfigurationSection("database");
		String url = String.format(
				"jdbc:mysql://%s:%d/%s",
				config.getString("host", "localhost"),
				config.getInt("port", 3306),
				config.getString("database", "comp")
				);
		
		pool = new ConnectionPool(url, config.getString("username", "username"), config.getString("password", "password"));
		
		try {
			ConnectionHandler connection = pool.getConnection();
			connection.release();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		
		expireTask = Bukkit.getScheduler().runTaskTimer(plugin, new ExpireTask(), 600, 600); // 30 second delay
	}
	
	/**
	 * Cleans up the database connections
	 */
	public void shutdown() {
		pool.closeConnections();
		expireTask.cancel();
	}
	
	/**
	 * Gets the connection pool
	 * @return The ConnectionPool
	 */
	public ConnectionPool getPool() {
		return pool;
	}
	
	private class ExpireTask implements Runnable {
		@Override
		public void run() {
			pool.removeExpired();
		}
	}
}
