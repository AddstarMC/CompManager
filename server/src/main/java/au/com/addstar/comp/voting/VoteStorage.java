package au.com.addstar.comp.voting;

import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.intellectualcrafters.plot.object.Plot;

public class VoteStorage<T extends Vote> {
	private final VotingStrategy<T> strategy;
	private final SetMultimap<Plot, Vote> votes;
	private final SetMultimap<UUID, Vote> playerVotes;
	
	public VoteStorage(VotingStrategy<T> strategy) {
		Preconditions.checkNotNull(strategy);
		
		this.strategy = strategy;
		votes = HashMultimap.create();
		playerVotes = HashMultimap.create();
	}
	
	/**
	 * Records a vote by the player.
	 * Note that if revoting is not permitted, this will throw an IllegalArgumentException
	 * if you attempt to change the vote on a plot
	 * @param player The player making the vote
	 * @param vote The vote to be made
	 * @throws IllegalArgumentException Thrown if a vote for that plot has already been
	 * registered, and revoting is not permitted
	 */
	public void recordVote(Player player, T vote) throws IllegalArgumentException {
		Set<Vote> theirVotes = playerVotes.get(player.getUniqueId());
		for (Vote existing : theirVotes) {
			if (existing.getPlot().equals(vote.getPlot())) {
				if (!canPlayersRevote()) {
					throw new IllegalArgumentException("Players are not permitted to revote");
				}
				
				playerVotes.remove(player.getUniqueId(), existing);
				votes.remove(existing.getPlot(), existing.getPlot());
				break;
			}
		}
		
		playerVotes.put(player.getUniqueId(), vote);
		votes.put(vote.getPlot(), vote);
	}
	
	public boolean canPlayersRevote() {
		return strategy.allowRevote();
	}
	
	public VotingStrategy<T> getStrategy() {
		return strategy;
	}
}
