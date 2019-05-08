package au.com.addstar.comp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.com.addstar.comp.gui.Hotbar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.intellectualsites.plotsquared.api.PlotAPI;
import com.lambdaworks.redis.RedisException;

import au.com.addstar.comp.commands.AgreeCommand;
import au.com.addstar.comp.commands.CompAdminCommand;
import au.com.addstar.comp.commands.CompInfoCommand;
import au.com.addstar.comp.commands.JoinCommand;
import au.com.addstar.comp.commands.VoteCommand;
import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.notifications.NotificationManager;
import au.com.addstar.comp.query.*;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.redis.RedisQueryTimeoutTask;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.whitelist.WhitelistHandler;

@SuppressWarnings("unused")
public class CompPlugin extends JavaPlugin {

	public static CompPlugin instance;
	private DatabaseManager databaseManager;
	private WhitelistHandler whitelistHandler;
	private CompManager compManager;
	private RedisManager redisManager;
	private P2Bridge bridge;
	private ConfirmationManager confirmationManager;
	private NotificationManager notificationManager;
	public Messages messages;
	private RemoteJoinManager remoteJoinManager;
	private static HashMap<Player, Hotbar> currentHotbars = new HashMap<>();
	private static String serverName;

	public static HashMap<Player, Hotbar> getCurrentHotbars()
	{
		return currentHotbars;
	}


	public static String getServerName() {
		return serverName;
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
		instance = this;
		serverName = getConfig().getString("server-id","null");
		if(serverName == "null"){
			getLogger().log(Level.WARNING,"You have no server-id configured - please update config");
		}
		databaseManager = new DatabaseManager(this);
		try {
			databaseManager.initialize(this.getDataFolder());
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to initialize database connection", e);
			return;
		}
		
		redisManager = new RedisManager(getConfig().getConfigurationSection("redis"),CompPlugin.serverName);
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
		try{
			PlotAPI api = new PlotAPI();
			bridge = new P2Bridge(api);
		}catch (Exception e){
			e.printStackTrace();
			Logger.getAnonymousLogger().warning("Disabling as PlotSqaured not available");
			onDisable();
		}
		if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new CompPlaceHolder(this).register();
		}
		compManager = new CompManager(new CompServerBackendManager(databaseManager), whitelistHandler, bridge, redisManager, getLogger());
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
		new CompAdminCommand(whitelistHandler, compManager, notificationManager, confirmationManager).registerAs(getCommand("compadmin"));
		new JoinCommand(compManager, confirmationManager, messages).registerAs(getCommand("compjoin"));
		new AgreeCommand(confirmationManager, messages).registerAs(getCommand("compagree"));
		new CompInfoCommand(compManager, messages).registerAs(getCommand("compinfo"));
		new VoteCommand(compManager, bridge, messages).registerAs(getCommand("compvote"));
		registerQueryHandlers();
		
		// Start listeners
		Bukkit.getPluginManager().registerEvents(new EventListener(whitelistHandler, getLogger(), compManager, bridge, messages), this);
		Bukkit.getPluginManager().registerEvents(new HotbarListener(this), this);
		Bukkit.getScheduler().runTaskTimer(this, () -> {
			confirmationManager.expireConfirmations();
			remoteJoinManager.expireHandlers();
		}, 20, 20);
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new CompTimer(compManager, notificationManager), 10, 10);
		Bukkit.getScheduler().runTaskTimer(this, new RedisQueryTimeoutTask(redisManager), 20, 20);
		redisManager.setCommandReceiver(new CommandHandler(compManager));
		
		// Load the comp
		compManager.reloadCurrentComp();
		instance = this;
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

	public static void setHotbar(Hotbar bar){
		if(bar == null){
			instance.getLogger().info( "Hotbar was null");
			return;
		}
		Player player = bar.getPlayer();
		if (currentHotbars.containsKey(player)) {
			(currentHotbars.get(player)).close();
			instance.getLogger().info("Player has existing hotbar we are removing ");
		}
		bar.getPlayer().getInventory().clear();
		currentHotbars.put(player, bar);
		instance.getLogger().info( "Hotbar added to " +player.getDisplayName());
		bar.showHotbar();
		player.sendMessage("Use the hotbar to select your action and then click.");
	}

	public static void removeHotbar(Player player)
	{
		if (player != null) {
			player.getInventory().clear();
		}
		currentHotbars.remove(player);
	}


	public P2Bridge getBridge() {
		return bridge;
	}
}
