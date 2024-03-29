package au.com.addstar.comp;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;
import com.plotsquared.core.configuration.caption.Caption;
import com.plotsquared.core.configuration.caption.CaptionHolder;
import com.plotsquared.core.configuration.caption.CaptionUtility;
import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.events.PlayerClaimPlotEvent;
import com.plotsquared.core.events.Result;
import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import au.com.addstar.comp.util.Messages;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.whitelist.WhitelistHandler;

public class EventListener implements Listener {
	private final WhitelistHandler whitelist;
	private final Logger logger;
	private final CompManager manager;
	private final P2Bridge bridge;
	private final Messages messages;
	
	public EventListener(WhitelistHandler whitelist, Logger logger, CompManager manager, P2Bridge bridge, Messages messages) {
		this.whitelist = whitelist;
		this.logger = logger;
		this.manager = manager;
		this.bridge = bridge;
		this.messages = messages;
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
		
		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, messages.get("join.denied.not-running"));
	}
	
	// Take players to their plot on join
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
		// Comp must not be closed
		if (manager.getCurrentComp() == null || manager.getCurrentComp().getState() == CompState.Closed) {
			return;
		}
		
		Plot plot = bridge.getPlot(event.getPlayer().getUniqueId());
		// Only do this if entered
		if (plot == null) {
			return;
		}
		plot.getHome(location -> {

			World world = Bukkit.getWorld(location.getWorldName());
			if (world == null) {
				logger.warning("Failed to teleport " + event.getPlayer().getName() + " to their plot. Invalid world " + location.getWorld());
				return;
			}

			// Make it so they go to their plot
			event.setSpawnLocation(
				new org.bukkit.Location(
					world,
					location.getX() + 0.5,
					location.getY(),
					location.getZ() + 0.5,
					location.getYaw(),
					location.getPitch()
				)
			);
		});
	}
	
	// Plot claim limitations
	@Subscribe
	public void onPlayerClaim(PlayerClaimPlotEvent event) {
		// Check comp is running
		if (!manager.isCompRunning()) {
			event.getPlotPlayer().sendMessage(StaticCaption.of(messages.get("join.denied.not-running")));
			event.setEventResult(Result.DENY);
			return;
		}
		
		// Check if the player is whitelisted
		boolean isWhitelisted = false;
		try {
			Player player = Bukkit.getPlayer(event.getPlotPlayer().getUUID());
			if(player != null) {
				isWhitelisted = whitelist.isWhitelisted(player);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to check whitelist.", e);
		}
		
		if (!isWhitelisted) {
			event.getPlotPlayer().sendMessage(StaticCaption.of(messages.get("join.denined.whitelist")));
			event.setEventResult(Result.DENY);
			return;
		}
		
		// Check for no other plots
		if (bridge.getPlot(event.getPlotPlayer().getUUID()) != null) {
			event.getPlotPlayer().sendMessage(StaticCaption.of(messages.get("join.denied.already-entered")));
			event.setEventResult(Result.DENY);
			return;
		}
		
		// Check the max size
		if (manager.getCurrentComp().getMaxEntrants() - bridge.getUsedPlotCount() <= 1) {
			event.getPlotPlayer().sendMessage(StaticCaption.of(messages.get("join.denined.full")));
			event.setEventResult(Result.DENY);
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
