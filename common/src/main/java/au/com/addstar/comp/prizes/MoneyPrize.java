package au.com.addstar.comp.prizes;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Represents a cash prize awarded through an economy plugin
 */
public class MoneyPrize extends BasePrize {
	private final double amount;
	
	public MoneyPrize(double amount) {
		Preconditions.checkArgument(amount > 0);
		this.amount = amount;
	}
	
	public MoneyPrize(String input) throws IllegalArgumentException {
		input = input.substring(1);
		amount = Double.parseDouble(input);
	}
	
	@Override
	public boolean award(Player player) {
		RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
		Economy econ = provider.getProvider();
		if (econ == null) {
			return false;
		}

		EconomyResponse response = econ.depositPlayer(player, amount);
		return response.transactionSuccess();
	}

	@Override
	public String toDatabase() {
		return String.format("$%f", amount);
	}

	@Override
	public String toHumanReadable() {
		return String.format("$%.2f", amount);
	}
}
