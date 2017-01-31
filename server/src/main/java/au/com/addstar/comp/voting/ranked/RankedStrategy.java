package au.com.addstar.comp.voting.ranked;

import au.com.addstar.comp.gui.Hotbar;
import au.com.addstar.comp.voting.AbstractVoteProvider;
import au.com.addstar.comp.voting.AbstractVotingStrategy;
import au.com.addstar.comp.voting.Placement;
import au.com.addstar.comp.voting.VoteStorage;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.intellectualcrafters.plot.object.PlotId;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RankedStrategy extends AbstractVotingStrategy<RankedVote> {
	@Override
	public boolean allowRevote() {
		return false;
	}

	@Override
	public List<Placement> countVotes(Multimap<PlotId, RankedVote> votes) {
		TreeMultimap<Integer, PlotId> rankedPlots;
		rankedPlots = TreeMultimap.create(Ordering.natural().reverse(), Ordering.arbitrary());

		for (PlotId plot : votes.keySet()) {
			int score = 0;
			// Tally all votes for this plot
			for (RankedVote vote : votes.get(plot)) {
				score += vote.toNumber();
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
	public AbstractVoteProvider<RankedVote> createProvider(VoteStorage<RankedVote> storage) {
		return new RankedProvider(storage);
	}

	@Override
	public Hotbar createHotbar(Player player) {
		setHasHotbar(false);
		return null;
	}

	private class RankedProvider extends AbstractVoteProvider<RankedVote> {
		public RankedProvider(VoteStorage<RankedVote> storage) {
			super(storage);
		}

		@Override
		public RankedVote onVoteCommand(Player voter, PlotId plot, UUID plotowner, String[] arguments) throws IllegalArgumentException {
			if (arguments.length < 1) {
				throw new IllegalArgumentException("Expected a value from 1 to 5 inclusive for the vote");
			}

			try {
				int value = Integer.parseInt(arguments[0]);
				return loadVote(plot, plotowner, value - 3);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Expected a value from 1 to 5 inclusive for the vote");
			}
		}

		@Override
		public Iterable<String> onVoteTabComplete(String[] arguments) {
			return null;
		}

		@Override
		public String getVoteCommandArguments() {
			return "1-5";
		}

		@Override
		public RankedVote loadVote(PlotId plotId, UUID plotowner, int value) throws IllegalArgumentException {
			RankedVote.Rank rank;
			if (value <= -2) {
				rank = RankedVote.Rank.ExtremelyDislike;
			} else if (value == -1) {
				rank = RankedVote.Rank.SomewhatDislike;
			} else if (value == 0) {
				rank = RankedVote.Rank.Neutral;
			} else if (value == 1) {
				rank = RankedVote.Rank.SomewhatLike;
			} else {
				rank = RankedVote.Rank.ExtremelyLike;
			}

			return new RankedVote(plotId, plotowner, rank);
		}
	}
}
