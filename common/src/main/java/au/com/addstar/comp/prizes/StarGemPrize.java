package au.com.addstar.comp.prizes;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Represents a stargem prize awarded through the Swaparoo plugin
 */
public class StarGemPrize extends BasePrize {
	private final int count;

	public StarGemPrize(int stargemCount) {
		Preconditions.checkArgument(stargemCount > 0);
		this.count = stargemCount;
	}

	@Override
	public boolean award(Player player) {
        // Run the console command to give the player StarGems if they are online
        // (if player is not online, log an error and return false)
        if (!player.isOnline()) {
            Bukkit.getLogger().warning("Unable to award " + count + " StarGems to offline player " + player.getName());
            return false;
        } else {
            String command = String.format("swaparoo stargems add %s %d", player.getName(), count);
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(), command);
            return true;
        }
	}

	@Override
	public String toDatabase() {
		return count + " stargem" + (count > 1 ? "s" : "");
	}

	@Override
	public String toHumanReadable() {
		return count + " StarGem" + (count > 1 ? "s" : "");
	}
}
