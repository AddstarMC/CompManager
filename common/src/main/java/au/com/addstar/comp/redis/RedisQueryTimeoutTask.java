package au.com.addstar.comp.redis;

/**
 * A simple task to timeout redis queries.
 * This should be run every second
 */
public class RedisQueryTimeoutTask implements Runnable {
	private final RedisManager manager;
	
	public RedisQueryTimeoutTask(RedisManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void run() {
		manager.timeoutOldQueries();
	}
}
