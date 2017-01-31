package au.com.addstar.comp.voting;

import au.com.addstar.comp.voting.likedislike.LikeDislikeStrategy;
import au.com.addstar.comp.voting.ranked.RankedStrategy;
import com.google.common.collect.Maps;

import java.util.Map;

public class VotingStrategies {
	public static final AbstractVotingStrategy<?> LIKE_DISLIKE_STRATEGY;
	public static final AbstractVotingStrategy<?> RANKED_STRATEGY;

	private static final Map<String, AbstractVotingStrategy<?>> strategies;



	static {
		LIKE_DISLIKE_STRATEGY = new LikeDislikeStrategy();
		RANKED_STRATEGY = new RankedStrategy();

		strategies = Maps.newHashMap();
		registerStrategy("likedislike", LIKE_DISLIKE_STRATEGY);
		registerStrategy("ranked", RANKED_STRATEGY);
	}

	public static void registerStrategy(String name, AbstractVotingStrategy<?> strategy) {
		strategies.put(name.toLowerCase(), strategy);
	}

	public static AbstractVotingStrategy<?> getStrategy(String name) {
		return strategies.get(name.toLowerCase());
	}

	public static AbstractVotingStrategy<?> getDefault() {
		return LIKE_DISLIKE_STRATEGY;
	}



}
