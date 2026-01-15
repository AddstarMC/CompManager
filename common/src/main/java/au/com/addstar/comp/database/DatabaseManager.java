package au.com.addstar.comp.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
		try (Connection connection = pool.getConnection()) {
			// Connection test - connection is automatically closed
		} catch (SQLException e) {
			throw new IOException(e);
		}
		
		// Run migrations after connection is established
		try {
			runMigrations();
		} catch (SQLException e) {
			plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to run database migrations", e);
			throw new IOException("Database migration failed", e);
		}
	}
	
	/**
	 * Runs database migrations to ensure schema is up to date
	 * @throws SQLException Thrown if a migration fails
	 */
	private void runMigrations() throws SQLException {
		try (Connection connection = pool.getConnection()) {
			// Check and create plot_entries table if it doesn't exist
			if (!tableExists(connection, "plot_entries")) {
				plugin.getLogger().info("Creating plot_entries table...");
				createPlotEntriesTable(connection);
				plugin.getLogger().info("plot_entries table created successfully");
			}
		}
	}
	
	/**
	 * Checks if a table exists in the database
	 * @param connection The database connection
	 * @param tableName The name of the table to check
	 * @return True if the table exists, false otherwise
	 * @throws SQLException Thrown if an error occurs checking the table
	 */
	private boolean tableExists(Connection connection, String tableName) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		try (ResultSet rs = metaData.getTables(null, null, tableName, null)) {
			return rs.next();
		}
	}
	
	/**
	 * Creates the plot_entries table
	 * @param connection The database connection
	 * @throws SQLException Thrown if table creation fails
	 */
	private void createPlotEntriesTable(Connection connection) throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS `plot_entries` (" +
			"`CompID`    int(11)     NOT NULL," +
			"`UUID`      char(36)    NOT NULL," +
			"`PlotID`    varchar(10) NOT NULL," +
			"`EntryDate` datetime    NOT NULL," +
			"PRIMARY KEY (`CompID`, `UUID`)," +
			"KEY `CompID` (`CompID`)," +
			"CONSTRAINT `fk_plot_entries_comp` FOREIGN KEY (`CompID`) REFERENCES `comps` (`ID`) ON DELETE CASCADE" +
			") ENGINE = InnoDB DEFAULT CHARSET = latin1";
		
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
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
