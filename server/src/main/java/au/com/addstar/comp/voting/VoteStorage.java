package au.com.addstar.comp.voting;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompPlugin;
import com.plotsquared.core.plot.PlotId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;


public class VoteStorage<T extends Vote> {
    private final AbstractVotingStrategy<T> strategy;
    private final AbstractVoteProvider<T> provider;
    private final SetMultimap<PlotId, T> votes;
    private final SetMultimap<UUID, T> playerVotes;

    private final CompManager manager;

    public VoteStorage(AbstractVotingStrategy<T> strategy, CompManager manager) {
        Preconditions.checkNotNull(strategy);

        this.strategy = strategy;
        this.manager = manager;
        provider = strategy.createProvider(this);
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
    public void recordVote(final Player player, final T vote) throws IllegalArgumentException {
        Preconditions.checkState(manager.getCurrentComp() != null, "No comp is set");
        if(!manager.Voter.test(player)){
            throw new IllegalArgumentException("Player does not meet the requirements to Vote");
        }
        Set<T> theirVotes = playerVotes.get(player.getUniqueId());
        for (T existing : theirVotes) {
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

        // Record the vote in the backend
        Bukkit.getScheduler().runTaskAsynchronously(CompPlugin.getPlugin(CompPlugin.class), () -> {
            try {
                manager.getBackend().addVote(player.getUniqueId(), vote, manager.getCurrentComp());
            } catch (SQLException e) {
                System.err.println("[CompManager] Failed to add vote to database:");
                e.printStackTrace();
            }
        });
    }

    public boolean canPlayersRevote() {
        return strategy.allowRevote();
    }

    public AbstractVotingStrategy<T> getStrategy() {
        return strategy;
    }

    public AbstractVoteProvider<T> getProvider() {
        return provider;
    }

    public void loadVotes() throws SQLException {
        votes.clear();
        playerVotes.clear();

        if (manager.getCurrentComp() == null) {
            return;
        }

        SetMultimap<UUID, T> loaded = manager.getBackend().loadVotes(manager.getCurrentComp(), provider);

        playerVotes.putAll(loaded);
        for (UUID voter : loaded.keySet()) {
            for (T vote : loaded.get(voter)) {
                votes.put(vote.getPlot(), vote);
            }
        }
    }

    /**
     * Counts all the votes using the voting strategy, and resets.
     * @return The placements generated by the strategy
     */
    public List<Placement> countVotes() {
        SetMultimap<PlotId, T> votes = HashMultimap.create(this.votes);
        this.votes.clear();

        return strategy.countVotes(votes);
    }
}
