package au.com.addstar.comp.lobby;

import java.util.Map;
import java.util.Set;

import au.com.addstar.comp.Competition;

public class CompManager {
	/**
	 * Gets the currently selected competition for a server.
	 * The competition may or may not be currently running
	 * @param serverId The id of the server
	 * @return A Competition object or null
	 */
	public Competition getCurrentComp(String serverId) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * Gets all currently selected competitions on all servers
	 * @return A map of server id, to comp. 
	 */
	public Map<String, Competition> getCurrentComps() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * Gets all known comp server ids. The servers must have checked in for them to be
	 * present in this set. 
	 * @return An unmodifiable set of server ids.
	 */
	public Set<String> getServerIds() {
		throw new UnsupportedOperationException("Not yet implemented");
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
