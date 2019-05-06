package au.com.addstar.comp.query;

import au.com.addstar.comp.redis.RedisQueryHandler;

public class QueryPing implements RedisQueryHandler {
	@Override
	public String onQuery(String command, String[] arguments) {
		return "PONG";
	}
}
