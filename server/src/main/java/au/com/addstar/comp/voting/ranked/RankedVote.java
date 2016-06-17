package au.com.addstar.comp.voting.ranked;

import au.com.addstar.comp.voting.Vote;
import com.intellectualcrafters.plot.object.PlotId;

public class RankedVote extends Vote {
	private final Rank rank;
	public RankedVote(PlotId plot, Rank rank) {
		super(plot);
		this.rank = rank;
	}

	public Rank getRank() {
		return rank;
	}

	@Override
	public int toNumber() {
		switch (rank) {
		case ExtremelyDislike:
			return -2;
		case SomewhatDislike:
			return -1;
		default:
		case Neutral:
			return 0;
		case SomewhatLike:
			return 1;
		case ExtremelyLike:
			return 2;
		}
	}

	public enum Rank {
		ExtremelyDislike,
		SomewhatDislike,
		Neutral,
		SomewhatLike,
		ExtremelyLike
	}
}
