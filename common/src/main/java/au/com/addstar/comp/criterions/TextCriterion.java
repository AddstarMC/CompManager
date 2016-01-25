package au.com.addstar.comp.criterions;

import org.bukkit.entity.Player;

/**
 * A plain old text based criterion. People will have to
 * manually check these criteria
 */
public class TextCriterion extends BaseCriterion {
	@Override
	public String describe() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public CriterionStanding getStanding(Player player) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
