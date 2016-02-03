package au.com.addstar.comp;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import com.plotsquared.bukkit.events.PlayerClaimPlotEvent;

import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.whitelist.WhitelistHandler;

public class EventListener implements Listener {
	private final WhitelistHandler whitelist;
	private final Logger logger;
	private final CompManager manager;
	private final P2Bridge bridge;
	
	public EventListener(WhitelistHandler whitelist, Logger logger, CompManager manager, P2Bridge bridge) {
		this.whitelist = whitelist;
		this.logger = logger;
		this.manager = manager;
		this.bridge = bridge;
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
		// Check comp is running
		if (!manager.isCompRunning()) {
			// TODO: Customizable messages
			event.getPlayer().sendMessage("placeholder: comp not running");
			event.setCancelled(true);
			return;
		}
		
		// Check if the player is whitelisted
		boolean isWhitelisted = false;
		try {
			isWhitelisted = whitelist.isWhitelisted(event.getPlayer());
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to check whitelist.", e);
		}
		
		if (!isWhitelisted) {
			// TODO: Customizable messages
			event.getPlayer().sendMessage("placeholder: not whitelisted");
			event.setCancelled(true);
			return;
		}
		
		// Check for no other plots
		if (bridge.getPlot(event.getPlayer().getUniqueId()) != null) {
			// TODO: Customizable messages
			event.getPlayer().sendMessage("placeholder: already entered");
			event.setCancelled(true);
			return;
		}
		
		// Check the max size
		if (manager.getCurrentComp().getMaxEntrants() - bridge.getUsedPlotCount() <= 1) {
			// TODO: Customizable messages
			event.getPlayer().sendMessage("placeholder: full");
			event.setCancelled(true);
			return;
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
