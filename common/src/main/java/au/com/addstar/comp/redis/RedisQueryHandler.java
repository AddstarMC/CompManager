package au.com.addstar.comp.redis;

public interface RedisQueryHandler {
	/**
	 * Called to query some data
	 * @param command The command being queried
	 * @param arguments The provided arguments
	 * @return The response
	 * @throws QueryException thrown to indicate a problem
	 */
	public String onQuery(String command, String[] arguments) throws QueryException;
}
