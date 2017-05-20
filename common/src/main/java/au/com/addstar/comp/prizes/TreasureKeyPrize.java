package au.com.addstar.comp.prizes;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;
import org.bukkit.plugin.RegisteredServiceProvider;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Represents a key prize awarded through the Treasures plugin
 */
public class TreasureKeyPrize extends BasePrize {
	private int count;
	private String name;
	
	public TreasureKeyPrize(int keyCount, String keyName) {
		Preconditions.checkArgument(keyCount > 0);
		Preconditions.checkArgument(keyName.length() > 0);
		this.count = keyCount;
		this.name = keyName;
	}

	@Override
	public boolean award(Player player) {
		throw new NotImplementedException();
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
