package au.com.addstar.comp;

import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.comp.commands.CompAdminCommand;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.whitelist.WhitelistHandler;

public class CompPlugin extends JavaPlugin {
	private DatabaseManager databaseManager;
	private WhitelistHandler whitelistHandler;
	
	private Competition currentComp;
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
		
		databaseManager = new DatabaseManager(this);
		try {
			databaseManager.initialize();
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to initialize database connection", e);
		}
		
		// Initialize other modules
		whitelistHandler = new WhitelistHandler(databaseManager.getPool());
		
		// Register commands
		new CompAdminCommand(whitelistHandler).registerAs(getCommand("compadmin"));
		
		// Start listeners
		Bukkit.getPluginManager().registerEvents(new EventListener(whitelistHandler, getLogger()), this);
	}
	
	@Override
	public void onDisable() {
		databaseManager.shutdown();
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
	 * Sets the currently selected competition
	 * @param comp The competition or null
	 */
	public void setCurrentComp(Competition comp) {
		currentComp = comp;
	}
}
