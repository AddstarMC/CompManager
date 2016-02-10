package au.com.addstar.comp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.database.ConnectionHandler;
import au.com.addstar.comp.database.DatabaseManager;
import au.com.addstar.comp.database.StatementKey;

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
	
	static {
		STATEMENT_LOAD = new StatementKey("SELECT Theme, State, StartDate, EndDate, MaxEntrants, FirstPrize, SecondPrize, DefaultPrize FROM " + TABLE_COMP + " WHERE ID=?");
		STATEMENT_ADD = new StatementKey("INSERT INTO " + TABLE_COMP + " (Theme, State, StartDate, EndDate, MaxEntrants, FirstPrize, SecondPrize, DefaultPrize) VALUES (?,?,?,?,?,?,?,?)", true);
		STATEMENT_UPDATE = new StatementKey("UPDATE " + TABLE_COMP + " SET Theme=?, State=?, StartDate=?, EndDate=?, MaxEntrants=?, FirstPrize=?, SecondPrize=?, DefaultPrize=? WHERE ID=?");
		
		STATEMENT_CRITERIA_LOAD = new StatementKey("SELECT CriteriaID, Name, Description, Type, Data FROM " + TABLE_CRITERIA + " WHERE CompID=?");
		STATEMENT_CRITERIA_ADD = new StatementKey("INSERT INTO " + TABLE_CRITERIA + " (CompID, Name, Description, Type, Data) VALUES (?,?,?,?,?)", true);
		STATEMENT_CRITERIA_REMOVE = new StatementKey("DELETE FROM " + TABLE_CRITERIA + " WHERE CriteriaID=?");
		STATEMENT_CRITERIA_UPDATE = new StatementKey("UPDATE " + TABLE_CRITERIA + "SET Name=?, Description=?, Type=?, Data=?");
		
		STATEMENT_SERVER_GET = new StatementKey("SELECT CompID FROM " + TABLE_SERVER + " WHERE ServerID=?");
		STATEMENT_SERVER_GETALL = new StatementKey("SELECT ServerID, CompID FROM " + TABLE_SERVER + " WHERE CompID IS NOT NULL");
		STATEMENT_SERVER_SET = new StatementKey("REPLACE INTO CompID (ServerID, CompID) VALUES (?,?)");
	}
	
	private final DatabaseManager manager;
	
	public CompBackendManager(DatabaseManager manager) {
		this.manager = manager;
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
				
				CompState state = CompState.valueOf(rs.getString("State"));
				if (state == null) {
					state = CompState.Closed;
				}
				result.setState(state);
				
				Timestamp start = rs.getTimestamp("StartDate");
				if (start != null) {
					result.setStartDate(start.getTime());
				}
				
				Timestamp end = rs.getTimestamp("EndDate");
				if (end != null) {
					result.setEndDate(end.getTime());
				}
				
				result.setMaxEntrants(rs.getInt("MaxEntrants"));
				
				// TODO: Prizes
				
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
		throw new UnsupportedOperationException("Not yet implemented");
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
					return Optional.absent();
				} else {
					return Optional.of(compId);
				}
			} else {
				return Optional.absent();
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
					results.put(serverId, Optional.<Integer>absent());
				}
			}
			
			return results;
		} finally {
			if (handler != null) {
				handler.release();
			}
		}
	}
}
