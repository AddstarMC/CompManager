package au.com.addstar.comp.voting;

import java.util.List;

import com.google.common.collect.Multimap;
import com.intellectualcrafters.plot.object.Plot;

/**
 * Represents a way of producing and counting votes
 * @param <T> The type of vote
 */
public abstract class VotingStrategy<T extends Vote> {
	/**
	 * Checks if re-voting is allowed with this strategy
	 * @return True if users can change their vote on a plot
	 */
	public abstract boolean allowRevote();
	
	/**
	 * Creates a vote
	 * @param plot The plot being voted on
	 * @param arguments The arguments provided to the vote command
	 * @return The created vote
	 * @throws IllegalArgumentException Throws an exception if the provided arguments arent valid for this strategy
	 */
	public abstract T createVote(Plot plot, String[] arguments) throws IllegalArgumentException;
	
	/**
	 * Handles tab completion for the vote command
	 * @param arguments The arguments provided to the vote command
	 * @return The options or null
	 */
	public abstract Iterable<String> tabCompleteVote(String[] arguments);
	
	/**
	 * Gets the argument usage text for the vote command when using this strategy.
	 * This should only concern itself with arguments that this strategy uses
	 * @return The usage text
	 */
	public abstract String getVoteCommandArguments();
	
	/**
	 * Counts all the given votes and determines the best plots
	 * @param votes The votes to count, grouped by plot
	 * @return An unmodifiable ordered list where the first element is first place
	 */
	public abstract List<Placement> countVotes(Multimap<Plot, T> votes);
}
