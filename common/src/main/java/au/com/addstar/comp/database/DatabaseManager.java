package au.com.addstar.comp.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class DatabaseManager {
	private final Plugin plugin;
	private HikariConnectionPool pool;

	public DatabaseManager(Plugin plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Initializes the database connection pool and tests it
	 * @throws IOException Thrown if the connection cannot be initialized
	 */
	public void initialize(File saveDir) throws IOException {
		ConfigurationSection config = plugin.getConfig().getConfigurationSection("database");
		pool = new HikariConnectionPool(config,saveDir);
		try {
			Connection connection = pool.getConnection();
			connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * Cleans up the database connections
	 */
	public void shutdown() {
		pool.closeConnections();
	}
	
	/**
	 * Gets the connection pool
	 * @return The HikariConnectionPool
	 */
	public HikariConnectionPool getPool() {
		return pool;
	}

}
