package au.com.addstar.comp.entry;

import org.bukkit.OfflinePlayer;

import com.intellectualcrafters.plot.object.Plot;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.confirmations.Confirmable;

public abstract class EnterHandler implements Confirmable {
	private final Competition comp;
	private final Plot plot;
	private final OfflinePlayer player;
	
	public EnterHandler(Competition comp, Plot plot, OfflinePlayer player) {
		this.comp = comp;
		this.plot = plot;
		this.player = player;
	}
	
	/**
	 * Gets the comp being entered
	 * @return The comp
	 */
	public Competition getComp() {
		return comp;
	}
	
	/**
	 * Gets the plot being assigned
	 * @return The plot
	 */
	public Plot getPlot() {
		return plot;
	}
	
	/**
	 * Gets the player being assigned to a plot
	 * @return The player. This may or may not be an online player
	 */
	public OfflinePlayer getPlayer() {
		return player;
	}
	
	@Override
	public void confirm() {
		complete();
	}
	
	/**
	 * Completes the entry process. The plot will
	 * be assigned to this player
	 */
	public abstract void complete();
	
	/**
	 * Aborts the entry process. The plot will be
	 * free to be assigned to another player.
	 * No message will be shown to anyone.
	 */
	public abstract void abort();
}
