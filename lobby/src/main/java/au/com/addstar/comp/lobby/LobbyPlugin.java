package au.com.addstar.comp.lobby;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.lambdaworks.redis.RedisException;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.lobby.commands.CompAdminCommand;
import au.com.addstar.comp.lobby.signs.SignListener;
import au.com.addstar.comp.lobby.signs.SignManager;
import au.com.addstar.comp.lobby.signs.SignRefresher;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.whitelist.WhitelistHandler;

public class LobbyPlugin extends JavaPlugin {
	private DatabaseManager databaseManager;
	private WhitelistHandler whitelistHandler;
	private CompManager compManager;
	private RedisManager redisManager;
	private SignManager signManager;
	private Messages messages;
	
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
		compManager = new CompManager(new CompBackendManager(databaseManager), redisManager, this);
		compManager.reload();
		signManager = new SignManager(new File(getDataFolder(), "signs.yml"), compManager, messages);
		try {
			signManager.load();
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to load signs", e);
			// No need to stop because of that
		}
		
		// Register commands
		new CompAdminCommand(whitelistHandler, compManager, redisManager, signManager, messages).registerAs(getCommand("compadmin"));

		// Register listeners
		Bukkit.getPluginManager().registerEvents(new SignListener(signManager), this);
		redisManager.setCommandReceiver(new CommandHandler(compManager));
		
		// Register tasks
		// TODO: Make refresh interval configurable
		Bukkit.getScheduler().runTaskTimer(this, new SignRefresher(signManager), 0, 200);
		
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
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
