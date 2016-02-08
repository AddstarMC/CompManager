package au.com.addstar.comp.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;

public class P2Bridge {
	private final PS plugin;
	
	public P2Bridge(PS plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Gets the plot of a player
	 * @param playerId The UUID of the player
	 * @return The Plot they own, or null
	 */
	public Plot getPlot(UUID playerId) {
		for (Plot plot : plugin.getPlots()) {
			if (plot.hasOwner() && plot.isOwner(playerId)) {
				return plot;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets all plots that are owned
	 * @return The owned plots
	 */
	public List<Plot> getUsedPlots() {
		List<Plot> plots = Lists.newArrayList();
		for (Plot plot : plugin.getPlots()) {
			if (plot.hasOwner()) {
				plots.add(plot);
			}
		}
		
		return plots;
	}
	
	/**
	 * Gets the number of used plots
	 * @return Number of owned plots
	 */
	public int getUsedPlotCount() {
		int count = 0;
		for (Plot plot : plugin.getPlots()) {
			if (plot.hasOwner()) {
				++count;
			}
		}
		return count;
	}
	
	/**
	 * Gets all the players that own a plot
	 * @return A set of player ids
	 */
	public Set<UUID> getOwners() {
		Set<UUID> owners = Sets.newHashSet();
		for (Plot plot : plugin.getPlots()) {
			if (plot.hasOwner()) {
				owners.addAll(plot.getOwners());
			}
		}
		
		return owners;
	}
	
	/**
	 * Claims a plot for a player.
	 * @param plot The plot to claim
	 * @param player The player to claim it
	 * @param teleport If true, the player (when online) will be teleported to the plot
	 * @throws IllegalArgumentException Thrown if the plot already has an owner
	 */
	public void claim(Plot plot, OfflinePlayer player, boolean teleport) throws IllegalArgumentException {
		Preconditions.checkArgument(!plot.hasOwner());
		Preconditions.checkState(Bukkit.isPrimaryThread(), "This must be done on the server thread");
		
		MainUtil.createPlot(player.getUniqueId(), plot);
		MainUtil.setSign(player.getName(), plot);
		
		if (player.isOnline() && teleport) {
			PlotPlayer wrappedPlayer = PlotPlayer.wrap(player.getPlayer());
			MainUtil.teleportPlayer(wrappedPlayer, wrappedPlayer.getLocation(), plot);
		}
		
		plugin.getPlotManager(plot.getWorld().worldname).claimPlot(plot.getWorld(), plot);
	}
	
	/**
	 * Gets all plots in order from 0;0 spiraling out
	 * @param maxPlots A limit to the number of plots available 
	 * @return An {@code Iterable<Plot>}
	 */
	public Iterable<Plot> getOrderedPlots(final int maxPlots) {
		final String plotWorld = Iterables.getFirst(plugin.getPlotWorlds(), null);
		
		if (plotWorld == null) {
			return Collections.emptySet();
		} else {
			return new Iterable<Plot>() {
				@Override
				public Iterator<Plot> iterator() {
					return new PlotIterator(plotWorld, maxPlots);
				}
			};
		}
	}
	
	private static class PlotIterator implements Iterator<Plot> {
		private static final int RIGHT = 0;
		private static final int LEFT = 1;
		private static final int UP = 2;
		private static final int DOWN = 3;
		
		// Final Order: Right -> Down -> Left -> Up
		private static final int[] NEXT = {DOWN, UP, RIGHT, LEFT};
		
		private final String plotWorld;
		
		private final int maxCount;
		private int plotCount;
		
		private int x;
		private int z;
		
		// For changing direction
		private int maxDist;
		private int dist;
		
		private int direction;
		
		public PlotIterator(String plotWorld, int maxCount) {
			this.plotWorld = plotWorld;
			this.maxCount = maxCount;
			
			plotCount = 0;
			direction = RIGHT;
			x = 0;
			z = 0;
			
			maxDist = 0;
			dist = 0;
		}
		
		@Override
		public boolean hasNext() {
			return (plotCount < maxCount);
		}
		
		private void nextLocation() {
			if (direction == RIGHT) {
				// Allow it to flow onto the next ring
				if (dist > maxDist) {
					dist = 1;
					maxDist += 2;
					direction = NEXT[direction];
				}
			} else {
				// Keep on same ring
				if (dist >= maxDist) {
					dist = 0;
					direction = NEXT[direction];
				}
			}
			
			// Move based on direction
			switch (direction) {
			case RIGHT:
				++x;
				break;
			case LEFT:
				--x;
				break;
			case UP:
				++z;
				break;
			case DOWN:
				--z;
				break;
			}
			
			++dist;
		}

		@Override
		public Plot next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			
			++plotCount;
			
			PlotId id = new PlotId(x, z);
			Plot plot = MainUtil.getPlot(plotWorld, id);
			
			// Move to next
			nextLocation();
			
			return plot;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
