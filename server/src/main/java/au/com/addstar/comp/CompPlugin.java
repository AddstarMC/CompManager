package au.com.addstar.comp;

import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.PS;
import com.lambdaworks.redis.RedisException;

import au.com.addstar.comp.commands.AgreeCommand;
import au.com.addstar.comp.commands.CompAdminCommand;
import au.com.addstar.comp.commands.CompInfoCommand;
import au.com.addstar.comp.commands.JoinCommand;
import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.notifications.NotificationManager;
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
	private ConfirmationManager confirmationManager;
	private NotificationManager notificationManager;
	
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
		confirmationManager = new ConfirmationManager();
		notificationManager = new NotificationManager();
		
		// Register commands
		new CompAdminCommand(whitelistHandler, compManager).registerAs(getCommand("compadmin"));
		new JoinCommand(compManager, confirmationManager).registerAs(getCommand("compjoin"));
		new AgreeCommand(confirmationManager).registerAs(getCommand("compagree"));
		new CompInfoCommand(compManager).registerAs(getCommand("compinfo"));
		registerQueryHandlers();
		
		// Start listeners
		Bukkit.getPluginManager().registerEvents(new EventListener(whitelistHandler, getLogger(), compManager, bridge), this);
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			@Override
			public void run() {
				confirmationManager.expireConfirmations();
			}
		}, 20, 20);
		Bukkit.getScheduler().runTaskTimer(this, new CompTimer(compManager, notificationManager), 60, 60);
		
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
