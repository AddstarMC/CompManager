package au.com.addstar.comp;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
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
import au.com.addstar.comp.redis.RedisQueryTimeoutTask;
import au.com.addstar.comp.util.Messages;
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
	private Messages messages;
	private RemoteJoinManager remoteJoinManager;
	
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
		messages = new Messages(new File(getDataFolder(), "messages.lang"), getResource("messages.lang"));
		try {
			messages.reload();
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to load messages", e);
			return;
		}
		
		whitelistHandler = new WhitelistHandler(databaseManager.getPool());
		bridge = new P2Bridge(PS.get());
		compManager = new CompManager(new CompBackendManager(databaseManager), whitelistHandler, bridge, redisManager, getLogger());
		confirmationManager = new ConfirmationManager();
		
		File notificationsFile = new File(getDataFolder(), "notifications.yml");
		if (!notificationsFile.exists()) {
			saveResource("notifications.yml", false);
		}
		notificationManager = new NotificationManager(notificationsFile, compManager);
		try {
			notificationManager.reload();
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "Failed to load notifications", e);
			// Not a critical failure, just continue
		}
		remoteJoinManager = new RemoteJoinManager(compManager, TimeUnit.SECONDS.toMillis(30)); // TODO: Configurable timeout
		
		// Register commands
		new CompAdminCommand(whitelistHandler, compManager, notificationManager).registerAs(getCommand("compadmin"));
		new JoinCommand(compManager, confirmationManager, messages).registerAs(getCommand("compjoin"));
		new AgreeCommand(confirmationManager, messages).registerAs(getCommand("compagree"));
		new CompInfoCommand(compManager, messages).registerAs(getCommand("compinfo"));
		registerQueryHandlers();
		
		// Start listeners
		Bukkit.getPluginManager().registerEvents(new EventListener(whitelistHandler, getLogger(), compManager, bridge, messages), this);
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			@Override
			public void run() {
				confirmationManager.expireConfirmations();
				remoteJoinManager.expireHandlers();
			}
		}, 20, 20);
		Bukkit.getScheduler().runTaskTimer(this, new CompTimer(compManager, notificationManager), 10, 10);
		Bukkit.getScheduler().runTaskTimer(this, new RedisQueryTimeoutTask(redisManager), 20, 20);
		redisManager.setCommandReceiver(new CommandHandler(compManager));
		
		// Load the comp
		compManager.reloadCurrentComp();
	}
	
	private void registerQueryHandlers() {
		redisManager.registerQueryHandler(new QueryEntrantCount(bridge), "entrant_count");
		redisManager.registerQueryHandler(new QueryIsEntrant(bridge), "is_entrant");
		redisManager.registerQueryHandler(new QueryPing(), "ping");
		redisManager.registerQueryHandler(remoteJoinManager, "join_begin", "join_confirm", "join_abort");
	}
	
	@Override
	public void onDisable() {
		databaseManager.shutdown();
	}
	
	public CompManager getCompManager() {
		return compManager;
	}
}
