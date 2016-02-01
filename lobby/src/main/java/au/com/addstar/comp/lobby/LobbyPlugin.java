package au.com.addstar.comp.lobby;

import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.lobby.commands.CompAdminCommand;
import au.com.addstar.comp.whitelist.WhitelistHandler;

public class LobbyPlugin extends JavaPlugin {
	private DatabaseManager databaseManager;
	private WhitelistHandler whitelistHandler;
	private CompManager compManager;
	
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
		compManager = new CompManager(new CompBackendManager(databaseManager), getLogger());
		compManager.reload();
		
		// Register commands
		new CompAdminCommand(whitelistHandler, compManager).registerAs(getCommand("compadmin"));
	}
	
	@Override
	public void onDisable() {
		databaseManager.shutdown();
	}
	
	/**
	 * Gets the competiton manager
	 * @return The CompManager
	 */
	public CompManager getManager() {
		return compManager;
	}
}
