package au.com.addstar.comp.voting;

import com.intellectualcrafters.plot.object.Plot;

public abstract class Vote {
	private final Plot plot;
	
	public Vote(Plot plot) {
		this.plot = plot;
	}
	
	public Plot getPlot() {
		return plot;
	}
}
