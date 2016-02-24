package au.com.addstar.comp;

import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.intellectualcrafters.plot.object.Plot;

import au.com.addstar.comp.entry.EnterHandler;
import au.com.addstar.comp.entry.EntryDeniedException;
import au.com.addstar.comp.entry.EntryDeniedException.Reason;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.whitelist.WhitelistHandler;

public class CompManager {
	/**
	 * A predicate you can use to check if a player has entered the comp
	 */
	public final Predicate<Player> Entrant;
	/**
	 * A predicate you can use to check if a player has not entered the comp 
	 */
	public final Predicate<Player> NonEntrant;
	
	private final CompBackendManager backend;
	private final P2Bridge bridge;
	private final Logger logger;
	private final WhitelistHandler whitelist;
	
	private Competition currentComp;
	
	public CompManager(CompBackendManager backend, WhitelistHandler whitelist, P2Bridge bridge, Logger logger) {
		this.backend = backend;
		this.whitelist = whitelist;
		this.bridge = bridge;
		this.logger = logger;
		
		Entrant = new Predicate<Player>() {
			@Override
			public boolean apply(Player player) {
				if (currentComp != null && currentComp.getState() != CompState.Closed) {
					return CompManager.this.bridge.getPlot(player.getUniqueId()) != null;
				} else {
					return false;
				}
			}
		};
		
		NonEntrant = Predicates.not(Entrant);
	}
	
	/**
	 * Loads the current comp from the database.
	 * This method will block waiting for the result
	 */
	public void reloadCurrentComp() {
		try {
			Optional<Integer> compID = backend.getCompID(Bukkit.getServerName());
			if (compID.isPresent()) {
				currentComp = backend.load(compID.get());
				logger.info("Current Competition: " + currentComp.getTheme());
			} else {
				currentComp = null;
				logger.info("Current Competition: None");
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to load the current competition for this server", e);
		}
	}
	
	/**
	 * Gets the currently selected competition.
	 * The comp may or may not be running
	 * @return A Competition object or null
	 */
	public Competition getCurrentComp() {
		return currentComp;
	}
	
	/**
	 * Checks if a competition is currently running
	 * @return True if {#link getCurrentComp()} is not null and {@link Competition#isRunning()} is true
	 */
	public boolean isCompRunning() {
		if (currentComp == null) {
			return false;
		}
		
		return currentComp.isRunning();
	}
	
	/**
	 * Sets the currently selected competition
	 * @param comp The competition or null
	 */
	public void setCurrentComp(Competition comp) {
		currentComp = comp;
	}
	
	/**
	 * Checks if a player has entered this comp
	 * @param player The player to check
	 * @return True if they have a plot
	 */
	public boolean hasEntered(OfflinePlayer player) {
		return bridge.getPlot(player.getUniqueId()) != null;
	}
	
	/**
	 * Checks if the current comp is full
	 * @return True if its full
	 */
	public boolean isFull() {
		if (currentComp == null) {
			return false;
		}
		
		if (bridge.getUsedPlotCount() >= currentComp.getMaxEntrants()) {
			return true;
		} else {
			return false;
		}
	}
	
	public CompState getState() {
		if (currentComp != null) {
			return currentComp.getState();
		} else {
			return CompState.Closed;
		}
	}
	
	// Plots reserved for players entering the comp (but not finished entering)
	private Set<Plot> reservedPlots = Sets.newHashSet();
	
	/**
	 * Tries to enter a player into the current comp.
	 * @param player The player to enter
	 * @return Returns a handler to continue or abort the entry process (for handling rule acceptance, etc.)
	 * @throws EntryDeniedException Thrown if the player cannot enter the comp
	 */
	public EnterHandler enterComp(OfflinePlayer player) throws EntryDeniedException {
		if (!isCompRunning()) {
			throw new EntryDeniedException(Reason.NotRunning, "No comp running");
		}
		
		if (hasEntered(player)) {
			throw new EntryDeniedException(Reason.AlreadyEntered, player.getName() + " is already entered");
		}
		
		try {
			if (!whitelist.isWhitelisted(player)) {
				throw new EntryDeniedException(Reason.Whitelist, player.getName() + " is not whitelisted");
			}
		} catch (SQLException e) {
			// Reject for safety just incase they arent actually whitelisted
			logger.log(Level.SEVERE, "Unable to check whitelist status for " + player.getName(), e);
			throw new EntryDeniedException(Reason.Whitelist, "Whitelist error");
		}
		
		if (bridge.getUsedPlotCount() >= currentComp.getMaxEntrants()) {
			throw new EntryDeniedException(Reason.Full, "Comp is full");
		}
		
		// Find a plot for them
		Plot target = null;
		for (Plot plot : bridge.getOrderedPlots(currentComp.getMaxEntrants())) {
			// Must not have an owner, or be reserved
			if (!plot.hasOwner() && !reservedPlots.contains(plot)) {
				target = plot;
				break;
			}
		}
		
		// Can happen if there are reserved plots
		if (target == null) {
			throw new EntryDeniedException(Reason.Full, "Comp is full");
		}
		
		reservedPlots.add(target);
		
		return new EnterHandlerImpl(currentComp, target, player);
	}
	
	private class EnterHandlerImpl extends EnterHandler {
		public EnterHandlerImpl(Competition comp, Plot plot, OfflinePlayer player) {
			super(comp, plot, player);
		}

		@Override
		public void complete() {
			// Assign the plot
			bridge.claim(getPlot(), getPlayer(), true);
			reservedPlots.remove(getPlot());
		}
		
		@Override
		public void abort() {
			reservedPlots.remove(getPlot());
			// No further action required
		}
	}
}
