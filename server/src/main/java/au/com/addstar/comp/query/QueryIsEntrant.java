package au.com.addstar.comp.query;

import java.util.UUID;

import au.com.addstar.comp.redis.QueryException;
import au.com.addstar.comp.redis.RedisQueryHandler;
import au.com.addstar.comp.util.P2Bridge;

public class QueryIsEntrant implements RedisQueryHandler {
	private final P2Bridge bridge;
	public QueryIsEntrant(P2Bridge bridge) {
		this.bridge = bridge;
	}
	
	@Override
	public String onQuery(String command, String[] arguments) throws QueryException {
		if (arguments.length < 1) {
			throw new QueryException("Invalid Query. Requires: <uuid>");
		}
		
		try {
			UUID playerId = UUID.fromString(arguments[0]);
			
			if (bridge.getPlot(playerId) != null) {
				return "true";
			} else {
				return "false";
			}
		} catch (IllegalArgumentException e) {
			throw new QueryException("Invalid UUID");
		}
	}
}
