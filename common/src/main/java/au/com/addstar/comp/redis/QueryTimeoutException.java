package au.com.addstar.comp.redis;

public class QueryTimeoutException extends QueryException {
	private static final long serialVersionUID = 575912754102559841L;

	public QueryTimeoutException(String message) {
		super(message);
	}
}
