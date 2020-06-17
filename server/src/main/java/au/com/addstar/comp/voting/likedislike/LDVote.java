package au.com.addstar.comp.voting.likedislike;

import au.com.addstar.comp.voting.Vote;


import com.plotsquared.core.plot.PlotId;
import org.bukkit.DyeColor;

import java.util.UUID;

public class LDVote extends Vote {
	private final Type type;
	
	public LDVote(PlotId plot, UUID plotowner, Type type) {
		super(plot, plotowner);
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}

	public static DyeColor getColor(Type type){
		switch (type) {
			case Dislike:
				return DyeColor.RED;
			case Like:
				return DyeColor.LIME;
			case Skip:
			default:
				return DyeColor.BLACK;
		}
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


	public static LDVote fromValue(PlotId plot, UUID plotowner, int value) {
		Type type;
		if (value < 0) {
			type = Type.Dislike;
		} else if (value > 0) {
			type = Type.Like;
		} else {
			// Neutral vote; neither like nor dislike
			type = Type.Skip;
		}

		return new LDVote(plot, plotowner, type);
	}

	public enum Type {
		Like,
		Dislike,
		Skip
	}
}
