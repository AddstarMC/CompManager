package au.com.addstar.comp.prizes;

import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;

/**
 * Represents a cash prize awarded through an economy plugin
 */
public class MoneyPrize extends BasePrize {
	private double amount;
	
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
		// TODO: Award money prize
		throw new UnsupportedOperationException("Not yet implemented");
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
