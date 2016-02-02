package au.com.addstar.comp.query;

import au.com.addstar.comp.redis.RedisQueryHandler;
import au.com.addstar.comp.util.P2Bridge;

public class QueryEntrantCount implements RedisQueryHandler {
	private final P2Bridge bridge;
	public QueryEntrantCount(P2Bridge bridge) {
		this.bridge = bridge;
	}
	
	@Override
	public String onQuery(String command, String[] arguments) {
		int entrantCount = bridge.getOwners().size();
		return String.valueOf(entrantCount);
	}
}
