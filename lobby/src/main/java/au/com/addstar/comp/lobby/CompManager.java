package au.com.addstar.comp.lobby;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.redis.RedisManager;

import javax.annotation.Nullable;

public class CompManager {
	private final CompBackendManager backend;
	private final RedisManager redis;
	private final Plugin plugin;
	private final Messages messages;

	/**
	 * Cached values from the broadcast-settings section of the config.yml file
	 */
	private ConfigurationSection broadcastSettings;

	private final Map<String, CompServer> servers;

	public CompManager(CompBackendManager backend, RedisManager redis, Plugin plugin, Messages messages) {
		this.backend = backend;
		this.redis = redis;
		this.plugin = plugin;
		this.messages = messages;

		broadcastSettings = plugin.getConfig().getConfigurationSection("broadcast-settings");
		servers = Maps.newHashMap();
	}

	/**
	 * Removes all key-pairs where the key is not in the provided set
	 *
	 * @param map  The existing map
	 * @param keys The new set of keys
	 */
	private void retainAll(Map<String, ?> map, Set<String> keys) {
		map.keySet().removeIf(key -> !keys.contains(key));
	}

	@Nullable
	public Set<String> getAllOfflineServers(){
		try {
			Map<String, Optional<Integer>> map = backend.getServerComps();
			return map.keySet();
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to get all offline servers", e);
		}
		return null;
	}
	/**
	 * Loads all competitions for each known server
	 * Also reloads the config.yml file and messages.lang
	 *
	 * @param loadConfigAndMessages True to reload the config and messages
	 */
	public void reload(Boolean loadConfigAndMessages) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					Map<String, Optional<Integer>> map = backend.getServerComps();
					retainAll(servers, map.keySet());

					for (String serverId : map.keySet()) {
						CompServer server = servers.get(serverId);
						if (server == null) {
							server = new CompServer(serverId, plugin, redis, backend);
							servers.put(serverId, server);
						}

						// Load in the competition object
						Optional<Integer> compId = map.get(serverId);
						server.currentComp = null;
						if (compId.isPresent()) {
							Competition comp = backend.load(compId.get());
							if (comp != null) {
								server.currentComp = comp;
							}
						}
					}

				} catch (SQLException e) {
					plugin.getLogger().log(Level.SEVERE, "Unable to load competitions: "+ e.getMessage());
				}

				// Reload the config
				try {
					if (loadConfigAndMessages) {
						plugin.reloadConfig();
						plugin.getLogger().log(Level.INFO, "Reloaded config.yml");
					}
				} catch (Exception e) {
					plugin.getLogger().log(Level.SEVERE, "Unable to reload the config", e);
				}

				broadcastSettings = plugin.getConfig().getConfigurationSection("broadcast-settings");

				// Log the broadcast settings
				plugin.getLogger().log(Level.INFO, "Broadcast interval running: " +
					getGlobalBroadcastRunningMin() + " to " + getGlobalBroadcastRunningMax() + " minutes");

				plugin.getLogger().log(Level.INFO, "Broadcast interval voting: " +
					getGlobalBroadcastVotingMin() + " to " + getGlobalBroadcastVotingMax() + " minutes");

				// Reload the messages
				try {
					if (loadConfigAndMessages) {
						messages.reload();
						plugin.getLogger().log(Level.INFO, "Reloaded messages.lang");
					}
				} catch (IOException e) {
					plugin.getLogger().log(Level.SEVERE, "Unable to reload messages", e);
				}
			}
		});
	}

	/**
	 * Gets all known comp server ids. The servers must have checked in for them to be
	 * present in this set.
	 *
	 * @return An unmodifiable set of server ids.
	 */
	public Set<String> getServerIds() {
		return Collections.unmodifiableSet(servers.keySet());
	}

	/**
	 * Gets a server by id. The server must have checked in for it to be
	 * present in this set.
	 *
	 * @param id the id of the server
	 * @return The server or null
	 */
	public CompServer getServer(String id) {
		return servers.get(id);
	}

	/**
	 * Gets all known servers. The servers must have checked in for them to be
	 * present in this set.
	 *
	 * @return An unmodifiable collection of servers
	 */
	public Collection<CompServer> getServers() {
		return Collections.unmodifiableCollection(servers.values());
	}

	/**
	 * Minimum minutes between broadcasting that a comp is running
	 *
	 * @return Interval, in minutes
	 */
	public int getGlobalBroadcastRunningMin() {
		return stringToInt(broadcastSettings, "global-broadcast-running-min", 90);
	}

	/**
	 * Maximum minutes between broadcasting that a comp is running
	 *
	 * @return Interval, in minutes
	 */
	public int getGlobalBroadcastRunningMax() {
		return stringToInt(broadcastSettings, "global-broadcast-running-max", 240);
	}

	/**
	 * Minimum minutes between broadcasting that a comp is in the voting state
	 *
	 * @return Interval, in minutes
	 */
	public int getGlobalBroadcastVotingMin() {
		return stringToInt(broadcastSettings, "global-broadcast-voting-min", 90);
	}

	/**
	 * Maximum minutes between broadcasting that a comp is in the voting state
	 *
	 * @return Interval, in minutes
	 */
	public int getGlobalBroadcastVotingMax() {
		return stringToInt(broadcastSettings, "global-broadcast-voting-max", 180);
	}

	/**
	 * Gets a message and replaces arguments
	 *
	 * @param id        The id in the message file
	 * @param arguments An array of String,Object pairs. This array must have an even length
	 * @return The formatted string
	 */
	public String getMessage(String id, Object... arguments) {
		return messages.get(id, arguments);
	}

	/**
	 * Lookup the config value and convert to an integer
	 *
	 * @param configSettings Configuration section
	 * @param keyName        Setting to find
	 * @param defaultValue   Default value if not found or not numeric
	 * @return Configuration value
	 */
	private int stringToInt(ConfigurationSection configSettings, String keyName, int defaultValue) {
		if (configSettings == null) {
			plugin.getLogger().log(Level.WARNING, "Config section is null; cannot get setting for " + keyName);
			return defaultValue;
		}

		String valueText = configSettings.getString(keyName, Integer.toString(defaultValue));
		if (Strings.isNullOrEmpty(valueText))
			return defaultValue;

		try {
			return Integer.parseInt(valueText);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
