package au.com.addstar.comp;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import au.com.addstar.comp.entry.EnterHandler;
import au.com.addstar.comp.entry.EntryDeniedException;
import au.com.addstar.comp.entry.EntryDeniedException.Reason;
import au.com.addstar.comp.prizes.BasePrize;
import au.com.addstar.comp.redis.RedisManager;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.voting.AbstractVotingStrategy;
import au.com.addstar.comp.voting.Placement;
import au.com.addstar.comp.voting.Vote;
import au.com.addstar.comp.voting.VoteStorage;
import au.com.addstar.comp.voting.VotingStrategies;
import au.com.addstar.comp.whitelist.WhitelistHandler;


import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompManager {
    /**
     * A predicate you can use to check if a player has entered the comp
     */
    public final Predicate<Player> Entrant;
    /**
     * A predicate you can use to check if a player has not entered the comp
     */
    public final Predicate<Player> NonEntrant;
    /*
     * A predicate that checks if a player is whitelisted and can vote.
     */
    public final Predicate<Player> Voter;

    private final CompServerBackendManager backend;
    private final P2Bridge bridge;
    private final Logger logger;
    private final WhitelistHandler whitelist;
    private final RedisManager redis;

    private Competition currentComp;
    private VoteStorage<? extends Vote> voteStorage;

    public CompManager(CompServerBackendManager backend, WhitelistHandler whitelist, P2Bridge bridge, RedisManager redis, Logger logger) {
        this.backend = backend;
        this.whitelist = whitelist;
        this.bridge = bridge;
        this.logger = logger;
        this.redis = redis;

        Entrant = player -> {
            if (currentComp != null && currentComp.getState() != CompState.Closed) {
                return CompManager.this.bridge.getPlot(player.getUniqueId()) != null;
            } else {
                return false;
            }
        };
        Voter = player -> {
            if(currentComp !=null && currentComp.getState() == CompState.Voting) {
                try{
                    return whitelist.isWhitelisted(player);
                }catch (SQLException e){
                    return false;
                }
            }
            else {
                return false;
            }
        };
        NonEntrant = Entrant.negate();

        voteStorage = new VoteStorage<>(VotingStrategies.getDefault(), this);
    }

    /**
     * Loads the current comp from the database.
     * This method will block waiting for the result
     */
    public void reloadCurrentComp() {
        try {

            Optional<Integer> compID = backend.getCompID(CompPlugin.getServerName());
            if (compID.isPresent()) {
                currentComp = backend.load(compID.get());
                logger.info("Current Competition (ID " + compID.get() + "): " + currentComp.getTheme());
            } else {
                currentComp = null;
                logger.info("Current Competition: None for Server:"+CompPlugin.getServerName());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load the current competition for this server", e);
        }

        if (currentComp != null) {
            AbstractVotingStrategy<?> strategy;
            String votingStrategyName = currentComp.getVotingStrategy();

            if (Strings.isNullOrEmpty(votingStrategyName)) {
                logger.warning("Voting strategy not defined; using the default strategy");
                strategy = VotingStrategies.getDefault();
            } else {
                strategy = VotingStrategies.getStrategy(votingStrategyName);
                if (strategy == null) {
                    logger.warning("Failed to find voting strategy " + votingStrategyName + ". Falling back to default strategy");
                    strategy = VotingStrategies.getDefault();
                }
            }

            voteStorage = new VoteStorage<>(strategy, this);
            try {
                voteStorage.loadVotes();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load votes for the current competition", e);
            }
        }
    }

    /**
     * Pushes all changes to the backend and notifies the lobby
     */
    public void updateCurrentComp() {
        if (currentComp == null) {
            return;
        }

        try {
            backend.update(currentComp);
            redis.broadcastCommand("reloadsender");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update current comp", e);
        }
    }

    /**
     * Gets the currently selected competition.
     * The comp may or may not be running
     * @return A Competition object or null
     */
    public Competition getCurrentComp() {
        return currentComp;
    }

    /**
     * Checks if a competition is currently running
     * @return True if {#link getCurrentComp()} is not null and {@link Competition#isRunning()} is true
     */
    public boolean isCompRunning() {
        if (currentComp == null) {
            return false;
        }

        return currentComp.isRunning();
    }

    /**
     * Sets the currently selected competition
     * @param comp The competition or null
     */
    public void setCurrentComp(Competition comp) {
        currentComp = comp;
    }

    /**
     * Checks if a player has entered this comp
     * @param player The player to check
     * @return True if they have a plot
     */
    public boolean hasEntered(OfflinePlayer player) {
        return hasEntered(player.getUniqueId());
    }

    public boolean hasEntered(UUID id){
        return bridge.getPlot(id) != null;
    }

    /**
     * Checks if the current comp is full
     * @return True if its full
     */
    public boolean isFull() {
        if (currentComp == null) {
            return false;
        }

        return bridge.getUsedPlotCount() >= currentComp.getMaxEntrants();
    }

    public CompState getState() {
        if (currentComp != null) {
            return currentComp.getState();
        } else {
            return CompState.Closed;
        }
    }

    public Long getTimeEnd(){
        switch (currentComp.getState()) {
            case Open:
                return currentComp.getEndDate();
            case Voting:
                 return currentComp.getVoteEndDate();
            default:
            case Closed:
                return -1L;
        }
    }



    /**
     * Notifies the manager that the state has changed automatically.
     * @param previousState The last state the comp was in
     */
    public void notifyStateChange(CompState previousState) {
        if (currentComp == null) {
            return;
        }

        // Must be automatic to do this
        if (!currentComp.isAutomatic()) {
            return;
        }

        if (previousState == CompState.Voting) {
            if (currentComp.getState() == CompState.Closed) {
                // TODO: Check that the comp has not already finished
                finishCompetition();
            }
        }
    }

    /**
     * Gets the vote storage for registering votes.
     * This may only be used if a comp is loaded
     * @return The vote storage
     * @throws IllegalStateException Thrown if a comp is not loaded
     */
    public VoteStorage<? extends Vote> getVoteStorage() throws IllegalStateException {
        Preconditions.checkState(currentComp != null);

        return voteStorage;
    }

    public CompServerBackendManager getBackend() {
        return backend;
    }

    /**
     * Completes a competition by tallying votes, awarding winners, and performing any and all post
     * comp actions
     * @throws IllegalStateException Thrown if the comp is not able to be finished
     */
    public void finishCompetition() throws IllegalStateException {
        Preconditions.checkState(currentComp != null);

        // Determine the winning plots
        final int maxPlacements = 2;// TODO: Do we allow customisable placements?
        List<PlotId> placements = determineWinners(maxPlacements);
        List<EntrantResult> fullResults = Lists.newArrayList();
        Optional<BasePrize> participationPrize = Optional.ofNullable(currentComp.getParticipationPrize());

        if (placements.isEmpty()) {
            // Record everyone as having participated
            for (Plot plot : bridge.getUsedPlots()) {
                for (UUID owner : plot.getOwners()) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
                    fullResults.add(new EntrantResult(owner, player.getName(), plot.getId().toString(), Optional.empty(), participationPrize, false));
                }
            }

            logger.warning("[Results] No votes have been recorded for the competition. No winner will be declared");
            // TODO: Declare no winner.
        } else {
            if (placements.size() < maxPlacements) {
                logger.warning("[Results] Not enough plots were voted on to determine enough winners. Only " + placements.size() + " ranks were filled out of " + maxPlacements);
            }

            logger.info("[Results] " + currentComp.getTheme() + " has been completed");
            logger.info("[Results] There were " + bridge.getUsedPlotCount() + " entrants");
            logger.info("[Results] The results were:");
            int index = 1;
            for (PlotId plotId : placements) {
                StringBuilder ownerNames = new StringBuilder();
                Plot plot = bridge.getPlot(plotId);
                // Select the prize
                Optional<BasePrize> prize;
                if (index == 1) {
                    prize = Optional.ofNullable(currentComp.getFirstPrize());
                } else if (index == 2) {
                    prize = Optional.ofNullable(currentComp.getSecondPrize());
                } else {
                    prize = participationPrize;
                }

                for (UUID owner : plot.getOwners()) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
                    fullResults.add(new EntrantResult(owner, player.getName(), plot.getId().toString(), Optional.of(index), prize, false));

                    if (ownerNames.length() != 0) {
                        ownerNames.append(", ");
                    }
                    ownerNames.append(Bukkit.getOfflinePlayer(owner));
                }

                logger.info("[Results] Place " + index + ": " + ownerNames + " with plot " + plotId);
                ++index;
            }

            // Add all non winners
            for (Plot plot : bridge.getUsedPlots()) {
                // Ignore plots that got a placement
                if (placements.contains(plot.getId())) {
                    continue;
                }

                for (UUID owner : plot.getOwners()) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
                    fullResults.add(new EntrantResult(owner, player.getName(), plot.getId().toString(), Optional.empty(), participationPrize, false));
                }
            }
        }

        // Record the results
        try {
            backend.addResults(currentComp, fullResults);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to record results", e);
        }
        currentComp.setState(CompState.Closed);
        updateCurrentComp();
    }

    private List<PlotId> determineWinners(int maxPlacements) {
        List<Placement> placements = voteStorage.countVotes();
        List<PlotId> finalPlacements = Lists.newArrayListWithCapacity(maxPlacements);
        // Resolve any ties
        for (Placement place : placements) {
            if (finalPlacements.size() >= maxPlacements) {
                break;
            }

            if (place.isDefinitive()) {
                finalPlacements.add(place.getPlot());
            } else {
                List<PlotId> contenders = Lists.newArrayList(place.getContenders());
                Collections.shuffle(contenders);
                logger.warning("[Voting] A tie was detected between the following plots: " + contenders);

                for (PlotId contender : contenders) {
                    if (finalPlacements.size() >= maxPlacements) {
                        break;
                    }

                    logger.warning("[Voting] " + contender + " was chosen randomly");
                    finalPlacements.add(contender);
                }
            }
        }

        return finalPlacements;
    }

    // Plots reserved for players entering the comp (but not finished entering)
    private final Set<Plot> reservedPlots = Sets.newHashSet();

    /**
     * Tries to enter a player into the current comp.
     * @param player The player to enter
     * @return Returns a handler to continue or abort the entry process (for handling rule acceptance, etc.)
     * @throws EntryDeniedException Thrown if the player cannot enter the comp
     */
    public EnterHandler enterComp(OfflinePlayer player) throws EntryDeniedException {
        if (!isCompRunning()) {
            throw new EntryDeniedException(Reason.NotRunning, "No comp running");
        }

        if (hasEntered(player)) {
            throw new EntryDeniedException(Reason.AlreadyEntered, player.getName() + " is already entered");
        }

        try {
            if (!whitelist.isWhitelisted(player)) {
                throw new EntryDeniedException(Reason.Whitelist, player.getName() + " is not whitelisted");
            }
        } catch (SQLException e) {
            // Reject for safety just incase they arent actually whitelisted
            logger.log(Level.SEVERE, "Unable to check whitelist status for " + player.getName(), e);
            throw new EntryDeniedException(Reason.Whitelist, "Whitelist error");
        }

        if (bridge.getUsedPlotCount() >= currentComp.getMaxEntrants()) {
            throw new EntryDeniedException(Reason.Full, "Comp is full");
        }

        // Find a plot for them
        Plot target = null;
        for (Plot plot : bridge.getOrderedPlots(currentComp.getMaxEntrants())) {
            // Must not have an owner, or be reserved
            if (!plot.hasOwner() && !reservedPlots.contains(plot)) {
                target = plot;
                break;
            }
        }

        // Can happen if there are reserved plots
        if (target == null) {
            throw new EntryDeniedException(Reason.Full, "Comp is full");
        }

        reservedPlots.add(target);

        return new EnterHandlerImpl(currentComp, target, player);
    }

    private class EnterHandlerImpl extends EnterHandler {
        public EnterHandlerImpl(Competition comp, Plot plot, OfflinePlayer player) {
            super(comp, plot, player);
        }

        @Override
        public void complete() {
            // Assign the plot
            bridge.claim(getPlot(), getPlayer(), true);
            reservedPlots.remove(getPlot());
        }

        @Override
        public void abort() {
            reservedPlots.remove(getPlot());
            // No further action required
        }
    }
}
