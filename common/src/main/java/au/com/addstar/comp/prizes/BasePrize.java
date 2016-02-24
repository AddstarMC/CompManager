package au.com.addstar.comp.prizes;

import org.bukkit.entity.Player;

/**
 * Represents a prize that can be given to players
 */
public abstract class BasePrize {
	/**
	 * Awards the prize to the given player
	 * @param player The player to award
	 * @return True if the player was able to receive the prize
	 */
	public abstract boolean award(Player player);
	
	/**
	 * Formats this prize for the database
	 * @return The formatted string
	 */
	public abstract String toDatabase();
	
	/**
	 * Formats this prize for humans to read
	 * @return The formatted string
	 */
	public abstract String toHumanReadable();
	
	/**
	 * Checks if this prize is silently given to players or not
	 * @return True if no notification will be shown to the player
	 */
	public boolean isSilent() {
		return false;
	}
	
	/**
	 * Parses a prize from the input string. The expected format as produced by
	 * {@link #toDatabase()}
	 * @param input The formatted input
	 * @return The parsed prize
	 * @throws IllegalArgumentException Thrown if the input string is invalid
	 */
	public static BasePrize parsePrize(String input) throws IllegalArgumentException {
		if (input.startsWith("$")) {
			return new MoneyPrize(input);
		} else {
			throw new IllegalArgumentException("Unknown prize type");
		}
	}
}
