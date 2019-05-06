package au.com.addstar.comp.util;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;

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

	public ArrayList<Plot> getOwnedPlots(){
		ArrayList<Plot> plots = new ArrayList<>();
		for (Plot plot : plugin.getPlots()) {
			if (plot.hasOwner()) {
				plots.add(plot);
			}
		}
		plugin.getLogger().log("Found " + plots.size() + " plots");
		return plots;
	}

	
	/**
	 * Gets any plot at the given location
	 * @param location The location to look at
	 * @return The plot or null
	 */
	public Plot getPlotAt(Location location) {
		com.intellectualcrafters.plot.object.Location wrappedLocation = new com.intellectualcrafters.plot.object.Location(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
		PlotArea area = plugin.getApplicablePlotArea(wrappedLocation);
		if (area == null) {
			return null;
		}
		
		return area.getPlot(wrappedLocation);
	}

	public Plot getPlot(PlotId plotId) {
		Set<PlotArea> areas = plugin.getPlotAreas(Bukkit.getWorlds().get(0).getName());

		for (PlotArea area : areas) {
			if (area.getPlotCount() > 0) {
				return area.getPlot(plotId);
			}
		}
		throw new IllegalStateException("No plot area for some reason");
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
		
		plot.create(player.getUniqueId(), false);
		plot.setSign(player.getName());
		
		if (player.isOnline() && teleport) {
			PlotPlayer wrappedPlayer = PlotPlayer.wrap(player.getPlayer());
			plot.teleportPlayer(wrappedPlayer);
		}
		
		plugin.getPlotManager(plot).claimPlot(plot.getArea(), plot);
	}
	
	/**
	 * Gets all plots in order from 0;0 spiraling out
	 * @param maxPlots A limit to the number of plots available 
	 * @return An {@code Iterable<Plot>}
	 */
	public Iterable<Plot> getOrderedPlots(final int maxPlots) {
		PlotArea targetArea = null;
		
		for (World world : Bukkit.getWorlds()) {
			Set<PlotArea> areas = plugin.getPlotAreas(world.getName());
			
			targetArea = Iterables.getFirst(areas, null);
			if (targetArea != null) {
				break;
			}
		}
		
		if (targetArea == null) {
			System.err.println("There are no PlotAreas available to assign players!");
			return Collections.emptySet();
		}
		
		final PlotArea fTargetArea = targetArea;
		return new Iterable<Plot>() {
			@Override
			public Iterator<Plot> iterator() {
				return new PlotIterator(fTargetArea, maxPlots);
			}
		};
	}
	
	private static class PlotIterator implements Iterator<Plot> {
		private static final int RIGHT = 0;
		private static final int LEFT = 1;
		private static final int UP = 2;
		private static final int DOWN = 3;
		
		// Final Order: Right -> Down -> Left -> Up
		private static final int[] NEXT = {DOWN, UP, RIGHT, LEFT};
		
		private final PlotArea area;
		
		private final int maxCount;
		private int plotCount;
		
		private int x;
		private int z;
		
		// For changing direction
		private int maxDist;
		private int dist;
		
		private int direction;
		
		public PlotIterator(PlotArea area, int maxCount) {
			this.area = area;
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
			Plot plot = area.getPlotAbs(id);
			
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
