package au.com.addstar.comp.confirmations;

public interface Confirmable {
	/**
	 * Confirms this task
	 */
	public void confirm();
	
	/**
	 * Aborts the task.
	 */
	public void abort();
}
