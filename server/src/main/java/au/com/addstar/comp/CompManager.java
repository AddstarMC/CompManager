package au.com.addstar.comp;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

import com.google.common.base.Optional;

public class CompManager {
	private final CompBackendManager backend;
	private final Logger logger;
	
	private Competition currentComp;
	
	public CompManager(CompBackendManager backend, Logger logger) {
		this.backend = backend;
		this.logger = logger;
	}
	
	/**
	 * Loads the current comp from the database.
	 * This method will block waiting for the result
	 */
	public void reloadCurrentComp() {
		try {
			Optional<Integer> compID = backend.getCompID(Bukkit.getServerName());
			if (compID.isPresent()) {
				currentComp = backend.load(compID.get());
				logger.info("Current Competition: " + currentComp.getTheme());
			} else {
				currentComp = null;
				logger.info("Current Competition: None");
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to load the current competition for this server", e);
		}
	}
	
	/**
	 * Gets the currently selected competition.
	 * The comp may or may not be running
	 * @return A Competition object or null
	 */
	public Competition getCurrentComp() {
		return currentComp;
	}
	
	/**
	 * Checks if a competition is currently running
	 * @return True if {#link getCurrentComp()} is not null and {@link Competition#isRunning()} is true
	 */
	public boolean isCompRunning() {
		if (currentComp == null) {
			return false;
		}
		
		return currentComp.isRunning();
	}
	
	/**
	 * Sets the currently selected competition
	 * @param comp The competition or null
	 */
	public void setCurrentComp(Competition comp) {
		currentComp = comp;
	}
}
