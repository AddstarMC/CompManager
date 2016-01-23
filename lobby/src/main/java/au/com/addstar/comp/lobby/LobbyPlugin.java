package au.com.addstar.comp.lobby;

import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.comp.database.DatabaseManager;

public class LobbyPlugin extends JavaPlugin {
	private DatabaseManager databaseManager;
	
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
	}
	
	@Override
	public void onDisable() {
		databaseManager.shutdown();
	}
}
