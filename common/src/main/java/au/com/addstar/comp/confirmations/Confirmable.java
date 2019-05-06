package au.com.addstar.comp.confirmations;

public interface Confirmable {
	/**
	 * Confirms this task
	 */
	void confirm();
	
	/**
	 * Aborts the task.
	 */
	void abort();
}
