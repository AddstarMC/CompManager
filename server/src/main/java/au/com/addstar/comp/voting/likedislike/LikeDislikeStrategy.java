package au.com.addstar.comp.voting.likedislike;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.gui.*;
import au.com.addstar.comp.gui.listeners.ButtonClickListener;
import au.com.addstar.comp.gui.listeners.LDVoteClickListener;
import au.com.addstar.comp.gui.listeners.PlotMoveClickListener;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import au.com.addstar.comp.voting.AbstractVoteProvider;
import au.com.addstar.comp.voting.Placement;
import au.com.addstar.comp.voting.VoteStorage;
import au.com.addstar.comp.voting.AbstractVotingStrategy;

import com.github.intellectualsites.plotsquared.plot.object.PlotId;

/**
 * The like / dislike strategy. Players can either like or dislike
 * a plot. Plots that are liked, get 1 point, dislike gets -1 points.
 * 
 * Scores are tallied and the plots are ranked by the number of points they have.
 */
public class LikeDislikeStrategy extends AbstractVotingStrategy<LDVote> {
 	//todo Move Text to Lang file

	public LikeDislikeStrategy() {
		super();
		setHasHotbar(true);
	}

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
	protected Hotbar createHotbar(Player player){
		Hotbar hotbar =  new Hotbar(player);
		setHasHotbar(true);
		int i = 0;
		for (LDVote.Type voteType : LDVote.Type.values()){
			HotbarButton button = new HotbarButton(i,voteType.toString(),LDVote.getColor(voteType));
			ButtonClickListener listener = new LDVoteClickListener(voteType);
			button.addClickListener(listener);
			button.setLore(Lore.fromString("Click to use,  when used will vote: " + voteType.toString()));
			hotbar.add(button);
			i++;
		}
		HotbarButton pButton = new HotbarButton(7,"Previous Plot",DyeColor.CYAN);
		pButton.setLore(new Lore("Click to move back to the last plot"));
		pButton.addClickListener(new PlotMoveClickListener(CompPlugin.instance,true));
		hotbar.add(pButton);
		HotbarButton nButton = new HotbarButton(8,"Next Plot",DyeColor.PINK);
		nButton.setLore(new Lore("Click to move to the next plot"));
		nButton.addClickListener(new PlotMoveClickListener(CompPlugin.instance,false));
		hotbar.add(nButton);
		return hotbar;
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
		public LDVote onVoteCommand(Player voter, PlotId plot, UUID plotowner, String[] args) throws IllegalArgumentException {

			String voteOptionsMsg = "You must specify 'like', 'dislike', or 'skip' for your vote; 'yes' and 'no' are also allowed";

			if (args.length < 1) {
				throw new IllegalArgumentException(voteOptionsMsg);
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
				throw new IllegalArgumentException(voteOptionsMsg);
			}
			
			return new LDVote(plot, plotowner, type);
		}
		
		@Override
		public Iterable<String> onVoteTabComplete(final String[] args) {
			if (args.length == 1) {
				return Stream.of("like", "dislike", "skip").filter(value -> args[0].toLowerCase().startsWith(value.toLowerCase())).collect(Collectors.toList());
			}
			
			return null;
		}
		
		@Override
		public String getVoteCommandArguments() {
			return "{like|dislike|skip}";
		}

		@Override
		public LDVote loadVote(PlotId plotId, UUID plotowner, int value) {
			return LDVote.fromValue(plotId, plotowner, value);
		}

	}
}
