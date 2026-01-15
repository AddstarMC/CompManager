package au.com.addstar.comp.util;

import java.util.*;

import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.util.SchematicHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class P2Bridge {
	private final PlotAPI api;
	
	public P2Bridge(PlotAPI plugin) {
		this.api = plugin;
	}

	public void registerPlotListener(Object listener) {
		api.registerListener(listener);
	}
	
	/**
	 * Gets the plot of a player
	 * @param playerId The UUID of the player
	 * @return The Plot they own, or null
	 */
	public Plot getPlot(UUID playerId) {
		for (Plot plot : api.getAllPlots()) {
			if (plot.hasOwner() && plot.isOwner(playerId)) {
				return plot;
			}
		}

		return null;
	}

	public ArrayList<Plot> getOwnedPlots(){
		ArrayList<Plot> plots = new ArrayList<>();
		for (Plot plot : api.getAllPlots()) {
			if (plot.hasOwner()) {
				plots.add(plot);
			}
		}
		Bukkit.getServer().getLogger().info("Found " + plots.size() + " plots");
		return plots;
	}

	
	/**
	 * Gets any plot at the given location
	 * @param location The location to look at
	 * @return The plot or null
	 */
	public Plot getPlotAt(Location location) {
		com.plotsquared.core.location.Location wrappedLocation = com.plotsquared.core.location.Location.at(
				location.getWorld().getName(), location.getBlockX(), location.getBlockX(), location.getBlockZ());
		Plot plot = Plot.getPlot(wrappedLocation);
		if (plot != null) {
			PlotArea area = plot.getArea();
			if (area != null)
				return area.getPlot(wrappedLocation);
		}
		return null;
	}

	public Plot getPlot(PlotId plotId) {
		Set<PlotArea> areas = api.getPlotAreas(Bukkit.getWorlds().get(0).getName());

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
		for (Plot plot : api.getAllPlots()) {
			if (plot.hasOwner()) {
				plots.add(plot);
			}
		}
		
		return plots;
	}
	
	/**
	 * Gets all plots that have owners.
	 * This is an alias for {@link #getUsedPlots()} provided for clarity in backup contexts.
	 * @return A list of all owned plots
	 */
	public List<Plot> getAllOwnedPlots() {
		return getUsedPlots();
	}
	
	/**
	 * Gets the PlotSquared SchematicHandler instance.
	 * This provides access to schematic export and import functionality.
	 * @return The SchematicHandler instance
	 */
	public SchematicHandler getSchematicHandler() {
		return api.getSchematicHandler();
	}
	
	/**
	 * Gets the number of used plots
	 * @return Number of owned plots
	 */
	public int getUsedPlotCount() {
		int count = 0;
		for (Plot plot : api.getAllPlots()) {
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
		for (Plot plot : api.getAllPlots()) {
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
		Bukkit.getLogger().fine("[DEBUG] P2Bridge.claim() called for plot: " + plot.getId());
		Bukkit.getLogger().fine("[DEBUG] Player: " + player.getName() + " (" + player.getUniqueId() + ")");
		Preconditions.checkArgument(!plot.hasOwner());
		Preconditions.checkState(Bukkit.isPrimaryThread(), "This must be done on the server thread");

		// Assign the plot to the offline player, before they join the comp server
		plot.setOwner(player.getUniqueId());

		if (player.isOnline() && teleport) {
			PlotPlayer wrappedPlayer = api.wrapPlayer(player.getUniqueId());
			plot.teleportPlayer(wrappedPlayer, aBoolean -> {
				if(aBoolean) {
					plot.claim(wrappedPlayer, teleport, null, true, true);
				}
			});
		}
	}
	
	/**
	 * Gets all plots in order from 0;0 spiraling out
	 * @param maxPlots A limit to the number of plots available 
	 * @return An {@code Iterable<Plot>}
	 */
	public Iterable<Plot> getOrderedPlots(final int maxPlots) {
		PlotArea targetArea = null;
		
		for (World world : Bukkit.getWorlds()) {
			Set<PlotArea> areas = api.getPlotAreas(world.getName());
			
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
		return () -> new PlotIterator(fTargetArea, maxPlots);
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
			PlotId id = PlotId.of(x, z);
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
