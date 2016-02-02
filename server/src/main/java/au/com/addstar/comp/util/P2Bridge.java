package au.com.addstar.comp.util;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Plot;

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
	
	public int getMaxPlotCount() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
