package au.com.addstar.comp.prizes;

import org.bukkit.entity.Player;

/**
 * Represents a cash prize awarded through an economy plugin
 */
public class MoneyPrize extends BasePrize {
	@Override
	public boolean award(Player player) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String toDatabase() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String toHumanReadable() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
