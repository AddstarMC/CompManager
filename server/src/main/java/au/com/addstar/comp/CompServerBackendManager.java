package au.com.addstar.comp;


import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.database.StatementKey;
import au.com.addstar.comp.voting.AbstractVoteProvider;
import au.com.addstar.comp.voting.Vote;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;

import com.github.intellectualsites.plotsquared.plot.object.PlotId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CompServerBackendManager extends CompBackendManager {
	// Votes table
	private static final String TABLE_VOTES = "votes";
	private static final StatementKey STATEMENT_VOTE_ADD;
	private static final StatementKey STATEMENT_VOTE_REMOVE;
	private static final StatementKey STATEMENT_VOTE_GETALL_COMP;
	private static final StatementKey STATEMENT_VOTE_GETALL_PLAYER;

	static {
		STATEMENT_VOTE_ADD = new StatementKey("INSERT INTO " + TABLE_VOTES + " (CompID, UUID, PlotID, PlotOwner, Vote) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE Vote=VALUES(Vote)");
		STATEMENT_VOTE_REMOVE = new StatementKey("DELETE FROM " + TABLE_VOTES + " WHERE CompID=? AND UUID=? AND PlotID=?");
		STATEMENT_VOTE_GETALL_COMP = new StatementKey("SELECT UUID, PlotID, PlotOwner, Vote FROM " + TABLE_VOTES + " WHERE CompID=?");
		STATEMENT_VOTE_GETALL_PLAYER = new StatementKey("SELECT PlotID, PlotOwner, Vote FROM " + TABLE_VOTES + " WHERE CompID=? AND UUID=?");
	}

	public CompServerBackendManager(DatabaseManager manager) {
		super(manager);
	}

	/**
	 * Loads all votes for a player in a comp
	 * @param player The player to load votes for
	 * @param comp The comp to load votes for
	 * @param provider The vote provider to make the votes
	 * @param <T> The type of vote
	 * @return A collection of loaded votes
	 * @throws SQLException Thrown if something goes wrong reading the votes
	 */
	public <T extends Vote> Collection<T> loadVotes(UUID player, Competition comp, AbstractVoteProvider<T> provider) throws SQLException {
		Connection handler = null;
		try {
			handler = getPool().getConnection();
			String sql;
			PreparedStatement statement = handler.prepareStatement(STATEMENT_VOTE_GETALL_PLAYER.getSQL());
			statement.setInt(1,comp.getCompId());
			statement.setString(2,player.toString());
			ResultSet rs = statement.executeQuery();
			List<T> votes = Lists.newArrayList();
			while (rs.next()) {
				String rawPlotId = rs.getString("PlotID");
				String rawPlotOwnerUUID = rs.getString("UUID");
				int voteValue = rs.getInt("Vote");

				PlotId plot = PlotId.fromString(rawPlotId);

				UUID plotowner;
				try {
					plotowner = UUID.fromString(rawPlotOwnerUUID);
				} catch (IllegalArgumentException e) {
					// It's fine to have invalid plotowners stored (will happen when this change is first deployed)
					plotowner = null;
				}

				T vote;
				try {
					vote = provider.loadVote(plot, plotowner, voteValue);
				} catch (IllegalArgumentException e) {
					// Drop invalid votes
					continue;
				}

				votes.add(vote);
			}

			return votes;
		} finally {
			if (handler != null) {
				handler.close();
			}
		}
	}

	/**
	 * Loads all votes for a comp
	 * @param comp The comp to load votes for
	 * @param provider A provider to create votes
	 * @param <T> The type of vote
	 * @return A multimap. The key is the UUID of the player, the values are the votes that player did
	 * @throws SQLException Thrown if something goes wrong reading the votes
	 */
	public <T extends Vote> SetMultimap<UUID, T> loadVotes(Competition comp, AbstractVoteProvider<T> provider) throws SQLException {
		Connection handler = null;
		try {
			handler = getPool().getConnection();
			String sql;
			PreparedStatement statement = handler.prepareStatement(STATEMENT_VOTE_GETALL_COMP.getSQL());
			statement.setInt(1,comp.getCompId());
			try (ResultSet rs = statement.executeQuery()) {
				SetMultimap<UUID, T> votes = HashMultimap.create();
				while (rs.next()) {
					String rawUUID = rs.getString("UUID");
					String rawPlotId = rs.getString("PlotID");
					String rawPlotOwnerUUID = rs.getString("UUID");
					int voteValue = rs.getInt("Vote");

					UUID id;
					try {
						id = UUID.fromString(rawUUID);
					} catch (IllegalArgumentException e) {
						// Drop invalid votes
						continue;
					}

					PlotId plot = PlotId.fromString(rawPlotId);

					UUID plotowner;
					try {
						plotowner = UUID.fromString(rawPlotOwnerUUID);
					} catch (IllegalArgumentException e) {
						// It's fine to have invalid plotowners stored (will happen when this change is first deployed)
						plotowner = null;
					}

					T vote;
					try {
						vote = provider.loadVote(plot, plotowner, voteValue);
					} catch (IllegalArgumentException e) {
						// Drop invalid votes
						continue;
					}

					votes.put(id, vote);
				}

				return votes;
			}
		} finally {
			if (handler != null) {
				handler.close();
			}
		}
	}

	/**
	 * Records a vote for the player. If the vote is already recorded for that player,
	 * it will be overridden
	 * @param voter The uuid of the voter
	 * @param vote The vote to record
	 * @param comp The comp to record the vote in
	 * @throws SQLException Thrown if something goes wrong writing the vote
	 */
	public void addVote(UUID voter, Vote vote, Competition comp) throws SQLException {
		Connection handler = null;
		try {
			handler = getPool().getConnection();
			String sql;
			PreparedStatement statement = handler.prepareStatement(STATEMENT_VOTE_ADD.getSQL());
			statement.setInt(1,comp.getCompId());
			statement.setString(2,voter.toString());
			statement.setString(3,vote.getPlot().toString());
			statement.setString(4,vote.getPlotOwner().toString());
			statement.setInt(5,vote.toNumber());
			statement.executeUpdate();
		} finally {
			if (handler != null) {
				handler.close();
			}
		}
	}

	/**
	 * Removes a vote for the player if any is recorded
	 * @param voter The UUID of the voter
	 * @param plot The id of the plot to clear the vote for
	 * @param comp The comp to remove the vote in
	 * @throws SQLException Thrown if something goes wrong clearing the vote
	 */
	public void removeVote(UUID voter, PlotId plot, Competition comp) throws SQLException {
		Connection handler = null;
		try {
			handler = getPool().getConnection();
			String sql;
			PreparedStatement statement = handler.prepareStatement(STATEMENT_VOTE_REMOVE.getSQL());
			statement.setInt(1,comp.getCompId());
			statement.setString(2,voter.toString());
			statement.setString(3,plot.toString());
			statement.executeUpdate();
		} finally {
			if (handler != null) {
				handler.close();
			}
		}
	}
}
