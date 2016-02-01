package au.com.addstar.comp.criterions;

import org.bukkit.entity.Player;

/**
 * Represents some kind of criterion for a competition
 */
public abstract class BaseCriterion {
	protected String name;
	protected String description;
	
	/**
	 * Sets the name of this criterion
	 * @param name The new name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the name of this criterion
	 * @return The name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets a description for this criterion
	 * @param description The new description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Gets a description of this criterion.
	 * If none is set, the result of {@link #describe()}
	 * will be used
	 * @return The description
	 */
	public String getDescription() {
		if (description != null) {
			return description;
		} else {
			return describe();
		}
	}
	
	/**
	 * Generates a description for this criterion.
	 * @return A description of this criterion
	 */
	public abstract String describe();
	
	/**
	 * Checks on a players standing with this criterion
	 * @param player The player to check
	 * @return The standing
	 */
	public abstract CriterionStanding getStanding(Player player);
	
	/**
	 * Loads the data into this criterion if it accepts it
	 * @param data The data string
	 */
	public abstract void load(String data);
	
	/**
	 * Creates a new empty criterion from the specified type
	 * @param type The type string
	 * @return A new Criterion
	 */
	public static BaseCriterion create(String type) {
		switch (type) {
		default:
		case "text":
			return new TextCriterion();
		}
	}
	
	/**
	 * Represents a player standing with this criterion
	 */
	public enum CriterionStanding {
		/**
		 * This criterion either presents no requirements, or cannot be determined programmatically
		 */
		NotApplicable,
		/**
		 * The player has achieved this criterion
		 */
		Achieved,
		/**
		 * The player has not achieved this criterion
		 */
		NotAchieved
	}
}
