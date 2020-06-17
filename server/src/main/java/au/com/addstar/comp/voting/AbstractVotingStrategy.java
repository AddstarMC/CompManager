package au.com.addstar.comp.voting;

import java.util.List;

import au.com.addstar.comp.gui.Hotbar;
import com.google.common.collect.Multimap;

import com.plotsquared.core.plot.PlotId;
import org.bukkit.entity.Player;

/**
 * Represents a way of producing and counting votes
 * @param <T> The type of vote
 */
public abstract class AbstractVotingStrategy<T extends Vote> {


	public AbstractVotingStrategy() {
		this.hasHotbar = false;
	}

	public void setHasHotbar(boolean hasHotbar) {
		this.hasHotbar = hasHotbar;
	}

	private boolean hasHotbar;

	/**
	 * Checks if re-voting is allowed with this strategy
	 * @return True if users can change their vote on a plot
	 */
	public abstract boolean allowRevote();
	
	/**
	 * Counts all the given votes and determines the best plots
	 * @param votes The votes to count, grouped by plot
	 * @return An unmodifiable ordered list where the first element is first place
	 */
	public abstract List<Placement> countVotes(Multimap<PlotId, T> votes);
	
	/**
	 * Creates the vote provider for this strategy
	 * @param storage The vote storage to pass to the provider
	 * @return The provider
	 */
	public abstract AbstractVoteProvider<T> createProvider(VoteStorage<T> storage);

	public boolean hasHotbar(){
		return hasHotbar;
	}

	protected abstract Hotbar createHotbar(Player player);

	public Hotbar getHotbar(Player player){
		return createHotbar(player);
	}

}
