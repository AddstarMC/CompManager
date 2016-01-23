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
}
