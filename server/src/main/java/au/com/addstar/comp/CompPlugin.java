package au.com.addstar.comp;

import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.PS;
import com.lambdaworks.redis.RedisException;

import au.com.addstar.comp.commands.CompAdminCommand;
import au.com.addstar.comp.commands.JoinCommand;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.query.*;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.whitelist.WhitelistHandler;

public class CompPlugin extends JavaPlugin {
	private DatabaseManager databaseManager;
	private WhitelistHandler whitelistHandler;
	private CompManager compManager;
	private RedisManager redisManager;
	private P2Bridge bridge;
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
		
		databaseManager = new DatabaseManager(this);
		try {
			databaseManager.initialize();
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to initialize database connection", e);
			return;
		}
		
		redisManager = new RedisManager(getConfig().getConfigurationSection("redis"));
		try {
			redisManager.initialize();
		} catch (RedisException e) {
			getLogger().log(Level.SEVERE, "Failed to initialize redis connection", e);
			return;
		}
		
		// Initialize other modules
		whitelistHandler = new WhitelistHandler(databaseManager.getPool());
		bridge = new P2Bridge(PS.get());
		compManager = new CompManager(new CompBackendManager(databaseManager), bridge, getLogger());
		
		// Register commands
		new CompAdminCommand(whitelistHandler, compManager).registerAs(getCommand("compadmin"));
		new JoinCommand(compManager).registerAs(getCommand("compjoin"));
		registerQueryHandlers();
		
		// Start listeners
		Bukkit.getPluginManager().registerEvents(new EventListener(whitelistHandler, getLogger(), compManager, bridge), this);
		
		// Load the comp
		compManager.reloadCurrentComp();
	}
	
	private void registerQueryHandlers() {
		redisManager.registerQueryHandler(new QueryEntrantCount(bridge), "entrant_count");
		redisManager.registerQueryHandler(new QueryIsEntrant(bridge), "is_entrant");
		redisManager.registerQueryHandler(new QueryPing(), "ping");
	}
	
	@Override
	public void onDisable() {
		databaseManager.shutdown();
	}
	
	public CompManager getCompManager() {
		return compManager;
	}
}
