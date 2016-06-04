package au.com.addstar.comp.voting.likedislike;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.intellectualcrafters.plot.object.Plot;

import au.com.addstar.comp.voting.Placement;
import au.com.addstar.comp.voting.VotingStrategy;

/**
 * The like / dislike strategy. Players can either like or dislike
 * a plot. Plots that are liked, get 1 point, dislike gets -1 points.
 * 
 * Scores are tallied and the plots are ranked by the number of points they have.
 */
public class LikeDislikeStrategy extends VotingStrategy<LDVote> {
	
	@Override
	public boolean allowRevote() {
		return true;
	}
	
	@Override
	public LDVote createVote(Plot plot, String[] args) throws IllegalArgumentException {
		if (args.length < 1) {
			throw new IllegalArgumentException("You must specify either 'like', 'dislike', or 'skip' for your vote");
		}
		
		// Parse the vote type
		LDVote.Type type;
		switch (args[0].toLowerCase()) {
		case "like":
		case "yes":
			type = LDVote.Type.Like;
			break;
		case "dislike":
		case "no":
			type = LDVote.Type.Dislike;
			break;
		case "skip":
			type = LDVote.Type.Skip;
			break;
		default:
			throw new IllegalArgumentException("You must specify either 'like', 'dislike', or 'skip' for your vote");
		}
		
		return new LDVote(plot, type);
	}
	
	@Override
	public Iterable<String> tabCompleteVote(final String[] args) {
		if (args.length == 1) {
			return Iterables.filter(Arrays.asList("like", "dislike", "skip"), new Predicate<String>() {
				@Override
				public boolean apply(String value) {
					return args[0].toLowerCase().startsWith(value.toLowerCase());
				}
			});
		}
		
		return null;
	}
	
	@Override
	public String getVoteCommandArguments() {
		return "{like|dislike|skip}";
	}
	
	@Override
	public List<Placement> countVotes(Multimap<Plot, LDVote> votes) {
		TreeMultimap<Integer, Plot> rankedPlots;
		rankedPlots = TreeMultimap.create(Ordering.natural().reversed(), Ordering.arbitrary());
		
		for (Plot plot : votes.keySet()) {
			int score = 0;
			// Tally all votes for this plot
			for (LDVote vote : votes.get(plot)) {
				switch (vote.getType()) {
				case Like:
					++score;
					break;
				case Dislike:
					--score;
					break;
				default:
					// No change
					break;
				}
			}
			
			rankedPlots.put(score, plot);
		}
		
		// Extract the ordered plots
		List<Placement> places = Lists.newArrayList();
		for (Integer key : rankedPlots.keySet()) {
			Set<Plot> plots = rankedPlots.get(key);
			places.add(new Placement(plots));
		}
		
		return Collections.unmodifiableList(places);
	}
}
