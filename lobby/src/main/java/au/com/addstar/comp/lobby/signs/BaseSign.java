package au.com.addstar.comp.lobby.signs;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class BaseSign {
	private final String serverId;
	
	// sign position
	private final Block block;
	private Sign signState;
	
	// Permission to break the sign
	private final String breakPermission;
	
	public BaseSign(String serverId, Block block, String breakPermission) {
		this.serverId = serverId;
		this.block = block;
		this.breakPermission = breakPermission;
	}
	
	/**
	 * Gets the id of the server this is for
	 * @return The server id
	 */
	public String getServerId() {
		return serverId;
	}
	
	/**
	 * Gets the sign block
	 * @return The sign block
	 */
	public Block getBlock() {
		return block;
	}
	
	/**
	 * Gets the permission needed to break this sign
	 * @return The permission
	 */
	public String getBreakPermission() {
		return breakPermission;
	}
	
	/**
	 * Refreshes the content of the sign.
	 * This is executed on the server thread, do not block waiting for data
	 */
	public abstract void refresh();
	
	private Sign getState() {
		if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN) {
			if (signState == null) {
				signState = (Sign)block.getState();
			}
			
			return signState;
		} else {
			signState = null;
			return null;
		}
	}
	
	/**
	 * Sets the contents of a line on the sign
	 * @param line The line number 0-3 inclusive
	 * @param text The text to set.
	 */
	protected final void setLine(int line, String text) {
		Sign state = getState();
		if (state != null) {
			state.setLine(line, text);
		}
	}
	
	/**
	 * Clears the contents of the sign
	 */
	protected final void clear() {
		Sign state = getState();
		if (state != null) {
			for (int i = 0; i < 4; ++i) {
				state.setLine(i, "");
			}
		}
	}
	
	/**
	 * To be called after {@link #clear} or {@link #setLine(int, String)}
	 * to update the contents of the sign
	 */
	protected final void update() {
		Sign state = getState();
		if (state != null) {
			state.update();
		}
	}
	
	/**
	 * Called when a player left clicks on the sign
	 * @param player The player who clicked
	 */
	public void onLeftClick(Player player) {}
	/**
	 * Called when a player right clicks on the sign
	 * @param player The player who clicked
	 */
	public void onRightClick(Player player) {}
	/**
	 * Called when a player right clicks on the sign while crouching
	 * @param player The player who clicked
	 */
	public void onShiftRightClick(Player player) {}
	
	/**
	 * Loads any specific values needed for this sign
	 * @param data The ConfigurationSection where theyre stored
	 */
	public abstract void load(ConfigurationSection data);
	/**
	 * Saves any specific values needed for this sign
	 * @param data The ConfigurationSection to write to
	 */
	public abstract void save(ConfigurationSection data);
}
