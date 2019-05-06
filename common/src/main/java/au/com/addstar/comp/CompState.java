package au.com.addstar.comp;

/**
 * Represents the state of a competition
 */
public enum CompState {
	/**
	 * The competition is closed; the world cannot be joined
	 */
	Closed,
	/**
	 * The competition is open for building
	 */
	Open,
	/**
	 * The competition is accessible, but only for voting
	 */
	Voting,
	/**
	 * The competition is accessible, but only for visiting / browsing
	 */
	Visit
}
