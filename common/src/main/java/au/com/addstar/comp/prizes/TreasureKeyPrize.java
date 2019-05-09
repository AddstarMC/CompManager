package au.com.addstar.comp.prizes;

import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;

/**
 * Represents a key prize awarded through the Treasures plugin
 */
public class TreasureKeyPrize extends BasePrize {
	private final int count;
	private final String name;
	
	public TreasureKeyPrize(int keyCount, String keyName) {
		Preconditions.checkArgument(keyCount > 0);
		Preconditions.checkArgument(keyName.length() > 0);
		this.count = keyCount;
		this.name = keyName;
	}

	@Override
	public boolean award(Player player) {
		throw new UnsupportedOperationException("Treasure rewards are not yet available via the plugin");
	}

	@Override
	public String toDatabase() {
		return count + " " + name + " key" + (count > 1 ? "s" : "");
	}

	@Override
	public String toHumanReadable() {
		return count + " " + name + " key" + (count > 1 ? "s" : "");
	}
}
