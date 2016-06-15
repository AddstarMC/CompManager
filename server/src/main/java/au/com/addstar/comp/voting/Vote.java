package au.com.addstar.comp.voting;

import com.intellectualcrafters.plot.object.PlotId;

public abstract class Vote {
	private final PlotId plot;
	
	public Vote(PlotId plot) {
		this.plot = plot;
	}
	
	public PlotId getPlot() {
		return plot;
	}

	/**
	 * Gets the vote as a number which can be loaded again in the vote provider
	 * @return The value of the vote
	 */
	public abstract int toNumber();
}
