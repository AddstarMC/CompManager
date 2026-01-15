package au.com.addstar.comp.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * Utility class for handling proxy/server transfer operations.
 * Handles sending players to lobby via BungeeCord plugin messages.
 */
public class ServerTransferUtil {
	
	/**
	 * Delay in ticks before calling the completion callback after player transfers.
	 * Value: 10 ticks = 0.5 seconds (at 20 TPS)
	 */
	private static final long TRANSFER_DELAY_TICKS = 10L;
	
	/**
	 * Sends a single player to the lobby server via BungeeCord plugin message.
	 * @param player The player to transfer
	 * @param lobbyId The ID of the lobby server
	 * @param plugin The plugin instance
	 */
	public static void sendPlayerToLobby(Player player, String lobbyId, JavaPlugin plugin) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(lobbyId);
		
		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}
	
	/**
	 * Sends all online players to the lobby server.
	 * Players with the bypass permission are not transferred but receive a notification.
	 * @param lobbyId The ID of the lobby server
	 * @param plugin The plugin instance
	 * @param messages The messages object for notifications
	 * @param whenDone Callback to run when transfers are complete
	 */
	public static void sendPlayersToLobby(String lobbyId, JavaPlugin plugin, Messages messages, Runnable whenDone) {
		// Get all online players
		List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
		
		// Separate players by bypass permission
		List<Player> playersToTransfer = new ArrayList<>();
		List<Player> bypassedPlayers = new ArrayList<>();
		
		for (Player player : onlinePlayers) {
			// Check for bypass permission
			if (player.hasPermission("comp.reset.bypass")) {
				bypassedPlayers.add(player);
			} else {
				playersToTransfer.add(player);
			}
		}
		
		// Send bypass notification to players with permission
		String bypassMessage = messages.get("reset.bypass.notify");
		for (Player player : bypassedPlayers) {
			player.sendMessage(bypassMessage);
		}
		
		// Transfer players without bypass permission
		for (Player player : playersToTransfer) {
			sendPlayerToLobby(player, lobbyId, plugin);
		}
		
		// Wait a short time for transfers to complete, then call callback
		// BungeeCord transfers are instant, but we give a small delay for safety
		Bukkit.getScheduler().runTaskLater(plugin, whenDone, TRANSFER_DELAY_TICKS);
	}
}
