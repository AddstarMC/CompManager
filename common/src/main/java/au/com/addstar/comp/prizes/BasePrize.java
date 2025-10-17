package au.com.addstar.comp.prizes;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a prize that can be given to players
 */
public abstract class BasePrize {

	/**
	 * Pattern for matching strings like:
	 *  5 Diamond Keys
	 *  2 Emerald Keys
	 *  1 Diamond Key
	 */
	private static final Pattern keyPattern = Pattern.compile("(\\d+) ([^ ]+) keys?", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for matching strings like:
     *  1 StarGem
     *  2 StarGems
     */
    private static final Pattern stargemPattern = Pattern.compile("(\\d+) stargems?", Pattern.CASE_INSENSITIVE);

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
		}
        else if (input.toLowerCase().contains(" stargem")) {
            Matcher gemMatcher = stargemPattern.matcher(input);
            if (gemMatcher.find()) {
                int gemCount = Integer.parseInt(gemMatcher.group(1));
                return new StarGemPrize(gemCount);
            }
        }
        else if (input.toLowerCase().contains(" key")) {
			Matcher keyMatcher = keyPattern.matcher(input);
			if (keyMatcher.find()) {
                int keyCount = Integer.parseInt(keyMatcher.group(1));
                String keyType = keyMatcher.group(2);
                return new TreasureKeyPrize(keyCount, keyType);
            }
        }
        throw new IllegalArgumentException("Unknown prize type");
	}
}
