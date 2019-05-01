package au.com.addstar.comp.lobby.signs;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.md_5.bungee.api.ChatColor;

public class SignListener implements Listener {
	private final SignManager manager;
	
	public SignListener(SignManager manager) {
		this.manager = manager;
	}
	
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=false)
	public void onClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		
		Block block = event.getClickedBlock();
		if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN) {
			return;
		}
		
		boolean shift = event.getPlayer().isSneaking();
		BaseSign sign = manager.getSign(block);
		if (sign == null) {
			// Handle placing signs
			if (event.getAction() == Action.LEFT_CLICK_BLOCK && manager.handlePending(event.getPlayer(), block)) {
				event.getPlayer().sendMessage(ChatColor.GREEN + "Sign created");
				event.setCancelled(true);
			}
			return;
		}
		
		// handle sign interactions
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (shift) {
				// Check if they can break it
				if (!event.getPlayer().hasPermission(sign.getBreakPermission())) {
					event.setCancelled(true);
				} else {
					manager.removeSign(sign);
					try {
						manager.save();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				sign.onLeftClick(event.getPlayer());
				event.setCancelled(true);
			}
			
		} else {
			if (shift) {
				sign.onShiftRightClick(event.getPlayer());
			} else {
				sign.onRightClick(event.getPlayer());
			}
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	private void onPlayerQuit(PlayerQuitEvent event) {
		manager.removePendingSign(event.getPlayer());
	}
}
