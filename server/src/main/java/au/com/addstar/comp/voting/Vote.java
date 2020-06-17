package au.com.addstar.comp.voting;


import com.plotsquared.core.plot.PlotId;

import java.util.UUID;

public abstract class Vote {
	private final PlotId plot;
	private final UUID plotowner;
	
	public Vote(PlotId plot, UUID plotowner) {
        this.plot = plot;
        this.plotowner = plotowner;
	}
	
	public PlotId getPlot() {
		return plot;
	}

	public UUID getPlotOwner() { return plotowner; }

	/**
	 * Gets the vote as a number which can be loaded again in the vote provider
	 * @return The value of the vote
	 */
	public abstract int toNumber();
}
