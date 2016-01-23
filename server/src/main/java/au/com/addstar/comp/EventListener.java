package au.com.addstar.comp;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import au.com.addstar.comp.whitelist.WhitelistHandler;

public class EventListener implements Listener {
	private final WhitelistHandler whitelist;
	private final Logger logger;
	
	public EventListener(WhitelistHandler whitelist, Logger logger) {
		this.whitelist = whitelist;
		this.logger = logger;
	}
	
	// Whitelist check
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
		boolean isWhitelisted = false;
		try {
			isWhitelisted = whitelist.isWhitelisted(event.getUniqueId());
			
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to check whitelist.", e);
		}
		
		if (!isWhitelisted) {
			// TODO: Customizable messages
			event.disallow(Result.KICK_WHITELIST, "placeholder: whitelist kick");
			logger.info(String.format("%s was denied access. They are not on the whitelist", event.getName()));
		}
	}
}
