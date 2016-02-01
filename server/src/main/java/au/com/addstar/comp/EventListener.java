package au.com.addstar.comp;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import com.plotsquared.bukkit.events.PlayerClaimPlotEvent;

import au.com.addstar.comp.whitelist.WhitelistHandler;

public class EventListener implements Listener {
	private final WhitelistHandler whitelist;
	private final Logger logger;
	private final CompManager manager;
	
	public EventListener(WhitelistHandler whitelist, Logger logger, CompManager manager) {
		this.whitelist = whitelist;
		this.logger = logger;
		this.manager = manager;
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
	
	// Handle comp running join checks
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerJoin(PlayerLoginEvent event) {
		// Comp must not be closed
		if (manager.getCurrentComp() != null && manager.getCurrentComp().getState() != CompState.Closed) {
			return;
		}
		
		if (event.getPlayer().hasPermission("comp.enter.bypass")) {
			return;
		}
		
		// TODO: Customizable messages
		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "placeholder: comp not running");
	}
	
	// Plot claim limitations
	@EventHandler
	public void onPlayerClaim(PlayerClaimPlotEvent event) {
		if (!manager.isCompRunning()) {
			// TODO: Customizable messages
			event.getPlayer().sendMessage("placeholder: comp not running");
			event.setCancelled(true);
		}
	}
	
	// Prevent interactions when comp is not running
	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onInteract(PlayerInteractEvent event) {
		if (manager.isCompRunning()) {
			return;
		}
		
		if (event.getPlayer().hasPermission("comp.build.bypass")) {
			return;
		}
		
		// Allow interact events that are physical, or just plain old interacts (no breaking or placing)
		if (event.getAction() == Action.PHYSICAL || (event.getItem() == null && event.getAction() != Action.LEFT_CLICK_BLOCK)) {
			return;
		}
		
		event.setCancelled(true);
	}
}
