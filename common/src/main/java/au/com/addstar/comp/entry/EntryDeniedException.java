package au.com.addstar.comp.entry;

public class EntryDeniedException extends Exception {
	private static final long serialVersionUID = -8721892854522107747L;
	
	private final Reason reason;
	
	public EntryDeniedException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}
	
	/**
	 * Gets the reason for entry being denied
	 * @return The reason
	 */
	public Reason getReason() {
		return reason;
	}
	
	public enum Reason {
		NotRunning,
		Full,
		AlreadyEntered,
		Whitelist
	}
}
