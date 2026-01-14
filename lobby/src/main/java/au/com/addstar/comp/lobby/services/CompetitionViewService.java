package au.com.addstar.comp.lobby.services;

import org.bukkit.entity.Player;

import au.com.addstar.comp.CompState;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.util.Messages;

/**
 * Service for handling competition view logic.
 */
public class CompetitionViewService {
	private final Messages messages;
	
	public CompetitionViewService(Messages messages) {
		this.messages = messages;
	}
	
	/**
	 * Allows a player to view a competition by teleporting them to the competition server.
	 * Validates that the server and competition exist and are accessible.
	 * 
	 * @param player The player attempting to view
	 * @param server The competition server
	 * @return True if the view was successful, false if validation failed
	 */
	public boolean viewCompetition(Player player, CompServer server) {
		// Validate server and competition state
		if (server == null || server.getCurrentComp() == null || !server.isOnline()) {
			player.sendMessage(messages.get("visit.denied.closed"));
			return false;
		}
		
		if (server.getCurrentComp().getState() == CompState.Closed) {
			player.sendMessage(messages.get("visit.denied.closed"));
			return false;
		}
		
		// Teleport the player to the competition server
		server.send(player);
		return true;
	}
}
