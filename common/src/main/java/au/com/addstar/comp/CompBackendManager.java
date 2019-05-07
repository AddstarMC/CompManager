package au.com.addstar.comp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import au.com.addstar.comp.database.ConnectionPool;
import java.util.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.database.ConnectionHandler;
import au.com.addstar.comp.database.DatabaseManager;
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

	
	static {
		STATEMENT_LOAD = new StatementKey("SELECT Theme, State, StartDate, EndDate, VoteEnd, VoteType, MaxEntrants, FirstPrize, SecondPrize, DefaultPrize FROM " + TABLE_COMP + " WHERE ID=?");
		STATEMENT_ADD = new StatementKey("INSERT INTO " + TABLE_COMP + " (Theme, State, StartDate, EndDate, VoteEnd, VoteType, MaxEntrants, FirstPrize, SecondPrize, DefaultPrize) VALUES (?,?,?,?,?,?,?,?)", true);
		STATEMENT_UPDATE = new StatementKey("UPDATE " + TABLE_COMP + " SET Theme=?, State=?, StartDate=?, EndDate=?, VoteEnd=?, VoteType=?, MaxEntrants=?, FirstPrize=?, SecondPrize=?, DefaultPrize=? WHERE ID=?");
		
		STATEMENT_CRITERIA_LOAD = new StatementKey("SELECT CriteriaID, Name, Description, Type, Data FROM " + TABLE_CRITERIA + " WHERE CompID=?");
		STATEMENT_CRITERIA_ADD = new StatementKey("INSERT INTO " + TABLE_CRITERIA + " (CompID, Name, Description, Type, Data) VALUES (?,?,?,?,?)", true);
		STATEMENT_CRITERIA_REMOVE = new StatementKey("DELETE FROM " + TABLE_CRITERIA + " WHERE CriteriaID=?");
		STATEMENT_CRITERIA_UPDATE = new StatementKey("UPDATE " + TABLE_CRITERIA + "SET Name=?, Description=?, Type=?, Data=?");
		
		STATEMENT_SERVER_GET = new StatementKey("SELECT CompID FROM " + TABLE_SERVER + " WHERE ServerID=?");
		STATEMENT_SERVER_GETALL = new StatementKey("SELECT ServerID, CompID FROM " + TABLE_SERVER + " WHERE CompID IS NOT NULL");
		STATEMENT_SERVER_SET = new StatementKey("REPLACE INTO CompID (ServerID, CompID) VALUES (?,?)");

		STATEMENT_RESULT_ADD = new StatementKey("INSERT INTO " + TABLE_RESULTS + " (CompID, UUID, Name, Rank, PlotID, Prize, Claimed) VALUES (?, ?, ?, ?, ?, ?, 0)");
		STATEMENT_RESULT_UPDATE_CLAIMED = new StatementKey("UPDATE " + TABLE_RESULTS + " SET Claimed=? WHERE CompID=? AND UUID=?");
		STATEMENT_RESULT_GETALL_COMP = new StatementKey("SELECT UUID, Name, Rank, PlotID, Prize, Claimed FROM " + TABLE_RESULTS + " WHERE CompID=?");
		STATEMENT_RESULT_GETALL_WINNERS = new StatementKey("SELECT UUID, Name, Rank, PlotID, Prize, Claimed FROM " + TABLE_RESULTS + " WHERE CompID=? AND Rank IS NOT NULL");
		STATEMENT_RESULT_GET = new StatementKey("SELECT Name, Rank, PlotID, Prize, Claimed FROM " + TABLE_RESULTS + "WHERE CompID=? AND UUID=?");
	}
	
	private final DatabaseManager manager;
	
	public CompBackendManager(DatabaseManager manager) {
		this.manager = manager;
	}

	protected ConnectionPool getPool() {
		return manager.getPool();
	}
	
	private BasePrize loadPrize(String input) {
		if (input == null) {
			return null;
		}
		
		try {
			return BasePrize.parsePrize(input);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
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
		ConnectionHandler handler = null;
		try {
			handler = manager.getPool().getConnection();
			
			ResultSet rs = handler.executeQuery(STATEMENT_LOAD, id);
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
				ResultSet criteria = handler.executeQuery(STATEMENT_CRITERIA_LOAD, id);
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
		} finally {
			if (handler != null) {
				handler.release();
			}
		}
	}
	
	/**
	 * Adds a new competition
	 * @param competition The competition to add
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void add(Competition competition) throws SQLException {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * Updates the settings of a competition
	 * @param competition The competition to update
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public void update(Competition competition) throws SQLException {
		Preconditions.checkArgument(competition.getCompId() >= 0);
		
		ConnectionHandler handler = null;
		try {
			handler = manager.getPool().getConnection();
			handler.executeUpdate(STATEMENT_UPDATE,
					competition.getTheme(),
					(competition.isAutomatic() ? "Auto" : competition.getState().name()),
					new Timestamp(competition.getStartDate()),
					new Timestamp(competition.getEndDate()),
					new Timestamp(competition.getVoteEndDate()),
					competition.getVotingStrategy(),
					competition.getMaxEntrants(),
					(competition.getFirstPrize() != null ? competition.getFirstPrize().toDatabase() : null),
					(competition.getSecondPrize() != null ? competition.getSecondPrize().toDatabase() : null),
					(competition.getParticipationPrize() != null ? competition.getParticipationPrize().toDatabase() : null),
					competition.getCompId()
					);
		} finally {
			if (handler != null) {
				handler.release();
			}
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
		ConnectionHandler handler = null;
		try {
			handler = manager.getPool().getConnection();
			
			ResultSet rs = handler.executeQuery(STATEMENT_SERVER_GET, serverId);
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
		} finally {
			if (handler != null) {
				handler.release();
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
		
		ConnectionHandler handler = null;
		try {
			handler = manager.getPool().getConnection();
			
			handler.executeUpdate(STATEMENT_SERVER_SET, serverId, comp.getCompId());
		} finally {
			if (handler != null) {
				handler.release();
			}
		}
	}
	
	/**
	 * Gets all servers with assigned comps
	 * @return A map of serverID to compID
	 * @throws SQLException Thrown if an SQLException occurs in the database
	 */
	public Map<String, Optional<Integer>> getServerComps() throws SQLException {
		ConnectionHandler handler = null;
		try {
			handler = manager.getPool().getConnection();
			
			Map<String, Optional<Integer>> results = Maps.newHashMap();
			
			ResultSet rs = handler.executeQuery(STATEMENT_SERVER_GETALL);
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
		} finally {
			if (handler != null) {
				handler.release();
			}
		}
	}

	public Collection<EntrantResult> loadResults(Competition comp, boolean winnersOnly) throws SQLException {
		ConnectionHandler handler = null;
		try {
			handler = getPool().getConnection();

			List<EntrantResult> results = Lists.newArrayList();
			try (ResultSet rs = handler.executeQuery(winnersOnly ? STATEMENT_RESULT_GETALL_WINNERS : STATEMENT_RESULT_GETALL_COMP, comp.getCompId())) {
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
		} finally {
			if (handler != null) {
				handler.release();
			}
		}
	}

	public EntrantResult getResult(Competition comp, UUID playerId) throws SQLException {
		ConnectionHandler handler = null;
		try {
			handler = getPool().getConnection();

			try (ResultSet rs = handler.executeQuery(STATEMENT_RESULT_GET, comp.getCompId(), playerId.toString())) {
				if (rs.next()) {
					try {
						return createEntrantResult(playerId, rs);
					} catch (IllegalArgumentException e) {
						// Skip the entry
					}
				}
			}

			return null;
		} finally {
			if (handler != null) {
				handler.release();
			}
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
		ConnectionHandler handler = null;
		try {
			handler = getPool().getConnection();
			String prize;
			if (result.getPrize().isPresent()) {
				prize = result.getPrize().get().toDatabase();
			} else {
				prize = null;
			}

			handler.executeUpdate(STATEMENT_RESULT_ADD, comp.getCompId(), result.getPlayerId(), result.getPlayerName(), result.getRank().orElse(null), result.getPlotId(), prize);
		} finally {
			if (handler != null) {
				handler.release();
			}
		}
	}

	public void addResults(Competition comp, Iterable<EntrantResult> results) throws SQLException {
		ConnectionHandler handler = null;
		try {
			handler = getPool().getConnection();
			for (EntrantResult result : results) {
				String prize;
				if (result.getPrize().isPresent()) {
					prize = result.getPrize().get().toDatabase();
				} else {
					prize = null;
				}

				handler.batchUpdate(STATEMENT_RESULT_ADD, comp.getCompId(), result.getPlayerId(), result.getPlayerName(), result.getRank().orElse(null), result.getPlotId(), prize);
			}

			handler.executeBatch(STATEMENT_RESULT_ADD);
		} finally {
			if (handler != null) {
				handler.release();
			}
		}
	}
}
