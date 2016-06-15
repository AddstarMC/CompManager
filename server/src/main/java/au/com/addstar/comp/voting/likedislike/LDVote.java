package au.com.addstar.comp.voting.likedislike;

import au.com.addstar.comp.voting.Vote;
import com.intellectualcrafters.plot.object.PlotId;

public class LDVote extends Vote {
	private final Type type;
	
	public LDVote(PlotId plot, Type type) {
		super(plot);
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}

	@Override
	public int toNumber() {
		switch (type) {
			case Dislike:
				return -1;
			case Like:
				return 1;
			default:
				return 0;
		}
	}

	public static LDVote fromValue(PlotId plot, int value) {
		Type type;
		if (value < 0) {
			type = Type.Dislike;
		} else if (value > 0) {
			type = Type.Like;
		} else {
			type = Type.Skip;
		}

		return new LDVote(plot, type);
	}

	public enum Type {
		Like,
		Dislike,
		Skip
	}
}
