package au.com.addstar.comp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.database.HikariConnectionPool;
import au.com.addstar.comp.database.StatementKey;
import au.com.addstar.comp.prizes.BasePrize;

/**
 * Handles loading and saving comp data to and from the database
 */
public class CompBackendManager {
	// comps table
	private static final String TABLE_COMP = "comps";
	private static final StatementKey STATEMENT_LOAD;
	private static final StatementKey STATEMENT_ADD;
	private static final StatementKey STATEMENT_UPDATE;
	private static final StatementKey STATEMENT_GETALL;
	private static final StatementKey STATEMENT_DELETE;
	
	// Criteria table
	private static final String TABLE_CRITERIA = "criteria";
	private static final StatementKey STATEMENT_CRITERIA_LOAD;
	private static final StatementKey STATEMENT_CRITERIA_ADD;
	private static final StatementKey STATEMENT_CRITERIA_REMOVE;
	private static final StatementKey STATEMENT_CRITERIA_UPDATE;
	
	// Server table
	private static final String TABLE_SERVER = "servers";
	private static final StatementKey STATEMENT_SERVER_GET;
	private static final StatementKey STATEMENT_SERVER_SET;
	private static final StatementKey STATEMENT_SERVER_GETALL;

	// Entrants table
	private static final String TABLE_RESULTS = "results";
	private static final StatementKey STATEMENT_RESULT_ADD;
	private static final StatementKey STATEMENT_RESULT_UPDATE_CLAIMED;
	private static final StatementKey STATEMENT_RESULT_GETALL_COMP;
	private static final StatementKey STATEMENT_RESULT_GETALL_WINNERS;
	private static final StatementKey STATEMENT_RESULT_GET;

	// Plot entries table
	private static final String TABLE_PLOT_ENTRIES = "plot_entries";
	private static final StatementKey STATEMENT_ENTRY_ADD;
	private static final StatementKey STATEMENT_ENTRY_GETALL_COMP;
	private static final StatementKey STATEMENT_ENTRY_GET;
	private static final StatementKey STATEMENT_ENTRY_REMOVE;

	
	static {
		STATEMENT_LOAD = new StatementKey("SELECT Theme, State, StartDate, EndDate, VoteEnd, VoteType, MaxEntrants, FirstPrize, SecondPrize, DefaultPrize FROM " + TABLE_COMP + " WHERE ID=?");
		STATEMENT_ADD = new StatementKey("INSERT INTO " + TABLE_COMP + " (Theme, State, StartDate, EndDate, VoteEnd, VoteType, MaxEntrants, FirstPrize, SecondPrize, DefaultPrize) VALUES (?,?,?,?,?,?,?,?,?,?)", true);
		STATEMENT_UPDATE = new StatementKey("UPDATE " + TABLE_COMP + " SET Theme=?, State=?, StartDate=?, EndDate=?, VoteEnd=?, VoteType=?, MaxEntrants=?, FirstPrize=?, SecondPrize=?, DefaultPrize=? WHERE ID=?");
		STATEMENT_GETALL = new StatementKey("SELECT ID, Theme, State FROM " + TABLE_COMP + " ORDER BY ID");
		STATEMENT_DELETE = new StatementKey("DELETE FROM " + TABLE_COMP + " WHERE ID=?");
		
		STATEMENT_CRITERIA_LOAD = new StatementKey("SELECT CriteriaID, Name, Description, Type, Data FROM " + TABLE_CRITERIA + " WHERE CompID=?");
		STATEMENT_CRITERIA_ADD = new StatementKey("INSERT INTO " + TABLE_CRITERIA + " (CompID, Name, Description, Type, Data) VALUES (?,?,?,?,?)", true);
		STATEMENT_CRITERIA_REMOVE = new StatementKey("DELETE FROM " + TABLE_CRITERIA + " WHERE CriteriaID=?");
		STATEMENT_CRITERIA_UPDATE = new StatementKey("UPDATE " + TABLE_CRITERIA + " SET Name=?, Description=?, Type=?, Data=? WHERE CriteriaID=?");
		
		STATEMENT_SERVER_GET = new StatementKey("SELECT CompID FROM " + TABLE_SERVER + " WHERE ServerID=?");
		STATEMENT_SERVER_GETALL = new StatementKey("SELECT ServerID, CompID FROM " + TABLE_SERVER + " WHERE CompID IS NOT NULL");
		STATEMENT_SERVER_SET = new StatementKey("REPLACE INTO " + TABLE_SERVER + " (ServerID, CompID) VALUES (?,?)");

		STATEMENT_RESULT_ADD = new StatementKey("INSERT INTO " + TABLE_RESULTS + " (CompID, UUID, Name, Rank, PlotID, Prize, Claimed) VALUES (?, ?, ?, ?, ?, ?, 0)");
		STATEMENT_RESULT_UPDATE_CLAIMED = new StatementKey("UPDATE " + TABLE_RESULTS + " SET Claimed=? WHERE CompID=? AND UUID=?");
		STATEMENT_RESULT_GETALL_COMP = new StatementKey("SELECT UUID, Name, Rank, PlotID, Prize, Claimed FROM " + TABLE_RESULTS + " WHERE CompID=?");
		STATEMENT_RESULT_GETALL_WINNERS = new StatementKey("SELECT UUID, Name, Rank, PlotID, Prize, Claimed FROM " + TABLE_RESULTS + " WHERE CompID=? AND Rank IS NOT NULL");
		STATEMENT_RESULT_GET = new StatementKey("SELECT Name, Rank, PlotID, Prize, Claimed FROM " + TABLE_RESULTS + "WHERE CompID=? AND UUID=?");

		STATEMENT_ENTRY_ADD = new StatementKey("INSERT INTO " + TABLE_PLOT_ENTRIES + " (CompID, UUID, PlotID, EntryDate) VALUES (?, ?, ?, ?)");
		STATEMENT_ENTRY_GETALL_COMP = new StatementKey("SELECT UUID, PlotID, EntryDate FROM " + TABLE_PLOT_ENTRIES + " WHERE CompID=?");
		STATEMENT_ENTRY_GET = new StatementKey("SELECT PlotID, EntryDate FROM " + TABLE_PLOT_ENTRIES + " WHERE CompID=? AND UUID=?");
		STATEMENT_ENTRY_REMOVE = new StatementKey("DELETE FROM " + TABLE_PLOT_ENTRIES + " WHERE CompID=? AND UUID=?");
	}
	
	private final DatabaseManager manager;
	
	public CompBackendManager(DatabaseManager manager) {
		this.manager = manager;
	}

	protected HikariConnectionPool getPool() {
		return manager.getPool();
	}
	
	private BasePrize loadPrize(String input) {
		if (input == null) {
			return null;
		}
		
		try {
			return BasePrize.parsePrize(input);
		} catch (IllegalArgumentException e) {
			Logger.getLogger("CompManager").log(Level.WARNING, "Failed to parse prize", e);
			return null;
		}
	}
	
	/**
	 * Loads a competition
	 * @param id The id of the comp
	 * @return The loaded Competition or null
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public Competition load(int id) throws SQLException {
		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_LOAD.getSQL());
			statement.setInt(1, id);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				Competition result = new Competition();
				result.setCompId(id);
				result.setTheme(rs.getString("Theme"));

				String stateString = rs.getString("State");
				if (stateString.equalsIgnoreCase("auto")) {
					result.setAutoState();
				} else {
					CompState state;
					try {
						state = CompState.valueOf(rs.getString("State"));
					} catch (IllegalArgumentException e) {
						state = CompState.Closed;
					}
					result.setState(state);
				}

				Timestamp start = rs.getTimestamp("StartDate");
				if (start != null) {
					result.setStartDate(start.getTime());
				}

				Timestamp end = rs.getTimestamp("EndDate");
				if (end != null) {
					result.setEndDate(end.getTime());
				}

				Timestamp voteEnd = rs.getTimestamp("VoteEnd");
				if (voteEnd != null) {
					result.setVoteEndDate(voteEnd.getTime());
				}

				result.setVotingStrategy(rs.getString("VoteType"));

				result.setMaxEntrants(rs.getInt("MaxEntrants"));

				result.setFirstPrize(loadPrize(rs.getString("FirstPrize")));
				result.setSecondPrize(loadPrize(rs.getString("SecondPrize")));
				result.setParticipationPrize(loadPrize(rs.getString("DefaultPrize")));

				// Load criteria
				PreparedStatement criteriaStatement = handler.prepareStatement(STATEMENT_CRITERIA_LOAD.getSQL());
				criteriaStatement.setInt(1, id);
				ResultSet criteria = criteriaStatement.executeQuery();
				while (criteria.next()) {
					BaseCriterion criterion = BaseCriterion.create(criteria.getString("Type"));
					criterion.setName(criteria.getString("Name"));
					criterion.setDescription(criteria.getString("Description"));

					String data = criteria.getString("Data");
					if (data != null) {
						criterion.load(data);
					}

					result.getCriteria().add(criterion);
				}

				return result;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Adds a new competition
	 * @param competition The competition to add
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void add(Competition competition) throws SQLException {
		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_ADD.getSQL(), STATEMENT_ADD.returnsGeneratedKeysInt());
			statement.setString(1, competition.getTheme());
			statement.setString(2, (competition.isAutomatic() ? "Auto" : competition.getState().name()));
			statement.setTimestamp(3, competition.getStartDate() > 0 ? new Timestamp(competition.getStartDate()) : null);
			statement.setTimestamp(4, competition.getEndDate() > 0 ? new Timestamp(competition.getEndDate()) : null);
			statement.setTimestamp(5, competition.getVoteEndDate() > 0 ? new Timestamp(competition.getVoteEndDate()) : null);
			statement.setString(6, competition.getVotingStrategy());
			statement.setInt(7, competition.getMaxEntrants());
			statement.setString(8, competition.getFirstPrize() != null ? competition.getFirstPrize().toDatabase() : null);
			statement.setString(9, competition.getSecondPrize() != null ? competition.getSecondPrize().toDatabase() : null);
			statement.setString(10, competition.getParticipationPrize() != null ? competition.getParticipationPrize().toDatabase() : null);
			statement.executeUpdate();
			
			// Get the generated ID
			ResultSet generatedKeys = statement.getGeneratedKeys();
			if (generatedKeys.next()) {
				int compId = generatedKeys.getInt(1);
				competition.setCompId(compId);
				
				// Save all criteria
				for (BaseCriterion criterion : competition.getCriteria()) {
					PreparedStatement criteriaStatement = handler.prepareStatement(STATEMENT_CRITERIA_ADD.getSQL(), STATEMENT_CRITERIA_ADD.returnsGeneratedKeysInt());
					criteriaStatement.setInt(1, compId);
					criteriaStatement.setString(2, criterion.getName());
					criteriaStatement.setString(3, criterion.getDescription());
					criteriaStatement.setString(4, getCriterionType(criterion));
					criteriaStatement.setString(5, getCriterionData(criterion));
					criteriaStatement.executeUpdate();
					criteriaStatement.close();
				}
			}
		}
	}
	
	/**
	 * Gets the type string for a criterion
	 */
	private String getCriterionType(BaseCriterion criterion) {
		// Determine type based on class name
		String className = criterion.getClass().getSimpleName();
		if (className.contains("BlockInclude")) {
			return "block";
		} else if (className.contains("Text")) {
			return "text";
		}
		return "text"; // Default
	}
	
	/**
	 * Gets the data string for a criterion
	 */
	private String getCriterionData(BaseCriterion criterion) {
		// TextCriterion doesn't need data
		if (criterion instanceof au.com.addstar.comp.criterions.TextCriterion) {
			return null;
		}
		
		// BlockIncludeCriterion uses Gson to serialize BlockCriteria
		if (criterion instanceof au.com.addstar.comp.criterions.BlockIncludeCriterion) {
			try {
				// Use reflection to access the blockCriteria field
				java.lang.reflect.Field field = criterion.getClass().getDeclaredField("blockCriteria");
				field.setAccessible(true);
				Object blockCriteria = field.get(criterion);
				if (blockCriteria != null) {
					com.google.gson.Gson gson = new com.google.gson.Gson();
					return gson.toJson(blockCriteria);
				}
			} catch (Exception e) {
				Logger.getLogger("CompManager").log(Level.WARNING, "Failed to serialize criterion data", e);
			}
		}
		
		return null;
	}
	
	/**
	 * Gets all competitions from the database
	 * @return A list of competitions with their IDs, themes, and states
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public List<Competition> getAll() throws SQLException {
		List<Competition> competitions = Lists.newArrayList();
		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_GETALL.getSQL());
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				int id = rs.getInt("ID");
				Competition comp = load(id);
				if (comp != null) {
					competitions.add(comp);
				}
			}
		}
		return competitions;
	}
	
	/**
	 * Deletes a competition from the database
	 * @param compId The ID of the competition to delete
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void delete(int compId) throws SQLException {
		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_DELETE.getSQL());
			statement.setInt(1, compId);
			statement.executeUpdate();
		}
	}
	
	/**
	 * Updates the settings of a competition
	 * @param competition The competition to update
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void update(Competition competition) throws SQLException {
		Preconditions.checkArgument(competition.getCompId() >= 0);
		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_UPDATE.getSQL());
			statement.setString(1, competition.getTheme());
			statement.setString(2, (competition.isAutomatic() ? "Auto" : competition.getState().name()));
			statement.setTimestamp(3, new Timestamp(competition.getStartDate()));
			statement.setTimestamp(4, new Timestamp(competition.getEndDate()));
			statement.setTimestamp(5, new Timestamp(competition.getVoteEndDate()));
			statement.setString(6, competition.getVotingStrategy());
			statement.setInt(7, competition.getMaxEntrants());
			statement.setString(8, competition.getFirstPrize() != null ? competition.getFirstPrize().toDatabase() : null);
			statement.setString(9, competition.getSecondPrize() != null ? competition.getSecondPrize().toDatabase() : null);
			statement.setString(10, competition.getParticipationPrize() != null ? competition.getParticipationPrize().toDatabase() : null);
			statement.setInt(11, competition.getCompId());
			statement.executeUpdate();
		}
	}
	
	/**
	 * Gets the currently active comp for a server
	 * @param serverId The serverID
	 * @return an Optional. When present, it is the active compID. 
	 *         When absent, there is no assigned comp.
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public Optional<Integer> getCompID(String serverId) throws SQLException {
		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_SERVER_GET.getSQL(), STATEMENT_SERVER_GET.returnsGeneratedKeysInt());
			statement.setString(1, serverId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				int compId = rs.getInt("CompID");
				if (rs.wasNull()) {
					return Optional.empty();
				} else {
					return Optional.of(compId);
				}
			} else {
				return Optional.empty();
			}
		}
	}
	
	/**
	 * Sets the currently active comp for a server
	 * @param serverId the serverID
	 * @param comp The competition to set. This MUST be in the database
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void setComp(String serverId, Competition comp) throws SQLException {
		Preconditions.checkArgument(comp.getCompId() >= 0, "That comp has not been added to the database");

		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_SERVER_SET.getSQL());
			statement.setString(1, serverId);
			statement.setInt(2, comp.getCompId());
			statement.executeUpdate();
		}
	}
	
	/**
	 * Gets all servers with assigned comps
	 * @return A map of serverID to compID
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public Map<String, Optional<Integer>> getServerComps() throws SQLException {
		try (Connection handler = manager.getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_SERVER_GETALL.getSQL(), STATEMENT_SERVER_GETALL.returnsGeneratedKeysInt());
			Map<String, Optional<Integer>> results = Maps.newHashMap();
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String serverId = rs.getString("ServerID");
				int compId = rs.getInt("CompID");
				if (!rs.wasNull()) {
					results.put(serverId, Optional.of(compId));
				} else {
					results.put(serverId, Optional.empty());
				}
			}

			return results;
		}
	}

	public Collection<EntrantResult> loadResults(Competition comp, boolean winnersOnly) throws SQLException {
		try (Connection handler = getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(winnersOnly ? STATEMENT_RESULT_GETALL_WINNERS.getSQL() : STATEMENT_RESULT_GETALL_COMP.getSQL());
			statement.setInt(1, comp.getCompId());
			List<EntrantResult> results = Lists.newArrayList();
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					try {
						UUID playerId = UUID.fromString(rs.getString("UUID"));
						EntrantResult result = createEntrantResult(playerId, rs);
						results.add(result);
					} catch (IllegalArgumentException e) {
						// Skip the entry
					}
				}
			}
			return results;
		}
	}

	public EntrantResult getResult(Competition comp, UUID playerId) throws SQLException {
		try (Connection handler = getPool().getConnection()) {
			String sql;
			PreparedStatement statement = handler.prepareStatement(STATEMENT_RESULT_GET.getSQL());
			statement.setInt(1, comp.getCompId());
			statement.setString(2, playerId.toString());
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					try {
						return createEntrantResult(playerId, rs);
					} catch (IllegalArgumentException e) {
						// Skip the entry
					}
				}
			}

			return null;
		}
	}

	private EntrantResult createEntrantResult(UUID playerId, ResultSet rs) throws SQLException {
		String playerName = rs.getString("Name");
		String plotId = rs.getString("PlotID");

		int rawRank = rs.getInt("Rank");
		Optional<Integer> rank;
		if (rs.wasNull()) {
			rank = Optional.empty();
		} else {
			rank = Optional.of(rawRank);
		}

		String rawPrize = rs.getString("Prize");
		Optional<BasePrize> prize;
		if (rawPrize == null) {
			prize = Optional.empty();
		} else {
			prize = Optional.of(BasePrize.parsePrize(rawPrize));
		}

		boolean claimed = rs.getBoolean("Claimed");

		return new EntrantResult(playerId, playerName, plotId, rank, prize, claimed);
	}

	public void addResult(Competition comp, EntrantResult result) throws SQLException {
		try (Connection handler = getPool().getConnection()) {
			String prize;
			if (result.getPrize().isPresent()) {
				prize = result.getPrize().get().toDatabase();
			} else {
				prize = null;
			}
			String sql;
			PreparedStatement statement = handler.prepareStatement(STATEMENT_RESULT_ADD.getSQL());
			setParameters(comp, result, prize, statement);

			statement.executeUpdate();
		}
	}

	private void setParameters(Competition comp, EntrantResult result, String prize, PreparedStatement statement) throws SQLException {
		statement.setInt(1, comp.getCompId());
		statement.setString(2, result.getPlayerId().toString());
		statement.setString(3, result.getPlayerName());
		if (result.getRank().isPresent()) {
			statement.setInt(4, result.getRank().get());
		} else {
			statement.setNull(4, java.sql.Types.INTEGER);
		}
		statement.setString(5, result.getPlotId());
		statement.setString(6, prize);
	}

	public void addResults(Competition comp, Iterable<EntrantResult> results) throws SQLException {
		try (Connection handler = getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_RESULT_ADD.getSQL());
			for (EntrantResult result : results) {
				String prize;
				if (result.getPrize().isPresent()) {
					prize = result.getPrize().get().toDatabase();
				} else {
					prize = null;
				}
				setParameters(comp, result, prize, statement);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	/**
	 * Adds a plot entry record for a player in a competition
	 * @param comp The competition
	 * @param playerId The UUID of the player
	 * @param plotId The plot identifier (format: "x;z")
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void addEntry(Competition comp, UUID playerId, String plotId) throws SQLException {
		Preconditions.checkArgument(comp.getCompId() >= 0, "Competition must have a valid ID");
		try (Connection handler = getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_ENTRY_ADD.getSQL());
			statement.setInt(1, comp.getCompId());
			statement.setString(2, playerId.toString());
			statement.setString(3, plotId);
			statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			statement.executeUpdate();
		}
	}

	/**
	 * Gets all plot entries for a competition
	 * @param comp The competition
	 * @return A map of player UUID to plot ID
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public Map<UUID, String> getEntries(Competition comp) throws SQLException {
		Preconditions.checkArgument(comp.getCompId() >= 0, "Competition must have a valid ID");
		try (Connection handler = getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_ENTRY_GETALL_COMP.getSQL());
			statement.setInt(1, comp.getCompId());
			Map<UUID, String> entries = Maps.newHashMap();
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					try {
						UUID playerId = UUID.fromString(rs.getString("UUID"));
						String plotId = rs.getString("PlotID");
						entries.put(playerId, plotId);
					} catch (IllegalArgumentException e) {
						// Skip invalid UUID entries
					}
				}
			}
			return entries;
		}
	}

	/**
	 * Gets a specific player's plot entry for a competition
	 * @param comp The competition
	 * @param playerId The UUID of the player
	 * @return The plot ID if the player has an entry, or null
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public String getEntry(Competition comp, UUID playerId) throws SQLException {
		Preconditions.checkArgument(comp.getCompId() >= 0, "Competition must have a valid ID");
		try (Connection handler = getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_ENTRY_GET.getSQL());
			statement.setInt(1, comp.getCompId());
			statement.setString(2, playerId.toString());
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					return rs.getString("PlotID");
				}
			}
			return null;
		}
	}

	/**
	 * Removes a plot entry record for a player in a competition
	 * @param comp The competition
	 * @param playerId The UUID of the player
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void removeEntry(Competition comp, UUID playerId) throws SQLException {
		Preconditions.checkArgument(comp.getCompId() >= 0, "Competition must have a valid ID");
		try (Connection handler = getPool().getConnection()) {
			PreparedStatement statement = handler.prepareStatement(STATEMENT_ENTRY_REMOVE.getSQL());
			statement.setInt(1, comp.getCompId());
			statement.setString(2, playerId.toString());
			statement.executeUpdate();
		}
	}
}
