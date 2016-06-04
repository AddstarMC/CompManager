package au.com.addstar.comp.voting.likedislike;

import com.intellectualcrafters.plot.object.Plot;

import au.com.addstar.comp.voting.Vote;

public class LDVote extends Vote {
	private final Type type;
	
	public LDVote(Plot plot, Type type) {
		super(plot);
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
	
	public enum Type {
		Like,
		Dislike,
		Skip
	}
}
