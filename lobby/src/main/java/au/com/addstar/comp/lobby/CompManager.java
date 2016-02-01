package au.com.addstar.comp.lobby;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import au.com.addstar.comp.CompBackendManager;
import au.com.addstar.comp.Competition;

public class CompManager {
	private final CompBackendManager backend;
	private final Logger logger;
	
	private Map<String, Competition> competitions;
	
	public CompManager(CompBackendManager backend, Logger logger) {
		this.backend = backend;
		this.logger = logger;
		
		competitions = Maps.newHashMap();
	}
	
	/**
	 * Loads all competitions for each known server
	 */
	public void reload() {
		try {
			Map<String, Integer> map = backend.getServerComps();
			competitions.clear();
			
			for (Entry<String, Integer> entry : map.entrySet()) {
				Competition comp = backend.load(entry.getValue());
				if (comp != null) {
					competitions.put(entry.getKey().toLowerCase(), comp);
					logger.info("Competition on " + entry.getKey());
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Unable to load competitions", e);
		}
	}
	
	/**
	 * Gets the currently selected competition for a server.
	 * The competition may or may not be currently running
	 * @param serverId The id of the server
	 * @return A Competition object or null
	 */
	public Competition getCurrentComp(String serverId) {
		return competitions.get(serverId.toLowerCase());
	}
	
	/**
	 * Gets all currently selected competitions on all servers
	 * @return A map of server id, to comp. 
	 */
	public Map<String, Competition> getCurrentComps() {
		return Collections.unmodifiableMap(competitions);
	}
	
	/**
	 * Gets all known comp server ids. The servers must have checked in for them to be
	 * present in this set. 
	 * @return An unmodifiable set of server ids.
	 */
	public Set<String> getServerIds() {
		return Collections.unmodifiableSet(competitions.keySet());
	}
	
	/**
	 * Gets a server by id. The server must have checked in for it to be
	 * present in this set.
	 * @param id the id of the server
	 * @return The server or null
	 */
	public CompServer getServer(String id) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * Gets all known servers. The servers must have checked in for them to be
	 * present in this set. 
	 * @return An unmodifiable set of servers
	 */
	public Set<CompServer> getAllServers() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
