package au.com.addstar.comp.voting.likedislike;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.intellectualcrafters.plot.object.PlotId;
import org.bukkit.entity.Player;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import au.com.addstar.comp.voting.AbstractVoteProvider;
import au.com.addstar.comp.voting.Placement;
import au.com.addstar.comp.voting.VoteStorage;
import au.com.addstar.comp.voting.AbstractVotingStrategy;

/**
 * The like / dislike strategy. Players can either like or dislike
 * a plot. Plots that are liked, get 1 point, dislike gets -1 points.
 * 
 * Scores are tallied and the plots are ranked by the number of points they have.
 */
public class LikeDislikeStrategy extends AbstractVotingStrategy<LDVote> {
	@Override
	public boolean allowRevote() {
		return true;
	}
	
	@Override
	public List<Placement> countVotes(Multimap<PlotId, LDVote> votes) {
		TreeMultimap<Integer, PlotId> rankedPlots;
		rankedPlots = TreeMultimap.create(Ordering.natural().reverse(), Ordering.arbitrary());
		
		for (PlotId plot : votes.keySet()) {
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
			Set<PlotId> plots = rankedPlots.get(key);
			places.add(new Placement(plots));
		}
		
		return Collections.unmodifiableList(places);
	}
	
	@Override
	public AbstractVoteProvider<LDVote> createProvider(VoteStorage<LDVote> storage) {
		return new VoteProvider(storage);
	}
	
	public static class VoteProvider extends AbstractVoteProvider<LDVote> {
		public VoteProvider(VoteStorage<LDVote> storage) {
			super(storage);
		} 
		
		@Override
		public LDVote onVoteCommand(Player voter, PlotId plot, String[] args) throws IllegalArgumentException {
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
		public Iterable<String> onVoteTabComplete(final String[] args) {
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
		public LDVote loadVote(PlotId plotId, int value) {
			return LDVote.fromValue(plotId, value);
		}
	}
}
