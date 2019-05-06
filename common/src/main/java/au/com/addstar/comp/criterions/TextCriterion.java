package au.com.addstar.comp.criterions;

import org.bukkit.entity.Player;

import com.google.common.base.Strings;

/**
 * A plain old text based criterion. People will have to
 * manually check these criteria
 */
public class TextCriterion extends BaseCriterion {
	@Override
	public String describe() {
		return Strings.nullToEmpty(description);
	}

	@Override
	public CriterionStanding getStanding(Player player) {
		return CriterionStanding.NotApplicable;
	}
	
	@Override
	public void load(String data) {
		// Not handled
	}
}
