package au.com.addstar.comp.lobby;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.redis.RedisManager;

public class CompManager {
	private final CompBackendManager backend;
	private final RedisManager redis;
	private final Plugin plugin;
	
	private Map<String, CompServer> servers;
	
	public CompManager(CompBackendManager backend, RedisManager redis, Plugin plugin) {
		this.backend = backend;
		this.redis = redis;
		this.plugin = plugin;
		
		servers = Maps.newHashMap();
	}
	
	/**
	 * Removes all key-pairs where the key is not in the provided set 
	 * @param map The existing map
	 * @param keys The new set of keys
	 */
	private void retainAll(Map<String, ?> map, Set<String> keys) {
		Iterator<String> keyIterator = map.keySet().iterator();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			if (!keys.contains(key)) {
				keyIterator.remove();
			}
		}
	}
	
	/**
	 * Loads all competitions for each known server
	 */
	public void reload() {
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
			plugin.getLogger().log(Level.SEVERE, "Unable to load competitions", e);
		}
	}
	
	/**
	 * Gets all known comp server ids. The servers must have checked in for them to be
	 * present in this set. 
	 * @return An unmodifiable set of server ids.
	 */
	public Set<String> getServerIds() {
		return Collections.unmodifiableSet(servers.keySet());
	}
	
	/**
	 * Gets a server by id. The server must have checked in for it to be
	 * present in this set.
	 * @param id the id of the server
	 * @return The server or null
	 */
	public CompServer getServer(String id) {
		return servers.get(id);
	}
	
	/**
	 * Gets all known servers. The servers must have checked in for them to be
	 * present in this set. 
	 * @return An unmodifiable collection of servers
	 */
	public Collection<CompServer> getServers() {
		return Collections.unmodifiableCollection(servers.values());
	}
}
