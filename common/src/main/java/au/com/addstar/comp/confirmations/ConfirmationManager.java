package au.com.addstar.comp.confirmations;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

public class ConfirmationManager {
	private final Map<CommandSender, Confirmation<?>> confirmations;
	private final TreeMultimap<Long, CommandSender> orderedConfirmations;
	
	public ConfirmationManager() {
		confirmations = Maps.newHashMap();
		orderedConfirmations = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
	}
	
	/**
	 * Adds a pending confirmation for a player to accept or reject.
	 * If there is an existing pending confirmation, it will be silently aborted
	 * @param player The player that owns the confirmation
	 * @param confirmation The confirmation
	 */
	public <T extends Confirmable> void addConfirmation(CommandSender player, Confirmation<T> confirmation) {
		synchronized (confirmations) {
			Confirmation<?> existing = confirmations.put(player, confirmation);
			
			if (existing != null) {
				existing.getHandler().abort();
				orderedConfirmations.remove(existing.getExpireMessage(), player);
			}
			
			orderedConfirmations.put(confirmation.getExpireTime(), player);
		}
	}
	
	/**
	 * Checks if a player has any pending confirmations
	 * @param player The player to check
	 * @return True if there are any
	 */
	public boolean hasPendingConfirmations(CommandSender player) {
		synchronized (confirmations) {
			return confirmations.containsKey(player);
		}
	}
	
	/**
	 * Attempts to confirm any pending confirmation the player has.
	 * @param player The player to confirm for
	 * @param input The input string, player provided, to match the token (if any)
	 * @return True if a confirmation was handled (either success or fail)
	 */
	public boolean tryConfirm(CommandSender player, String input) {
		synchronized (confirmations) {
			Confirmation<?> confirmation = confirmations.get(player);
			
			if (confirmation == null) {
				return false;
			}
			
			// Check any required token
			if (confirmation.getToken() != null) {
				String token = confirmation.getToken();
				// Input cannot contain multiple spaces in a row thanks to the bukkit command system
				token = token.replaceAll(" +", " ");
				
				if (!input.equalsIgnoreCase(token)) {
					// Display error
					if (confirmation.getTokenFailMessage() != null) {
						player.sendMessage(confirmation.getTokenFailMessage());
					}
					
					// Abort
					confirmation.getHandler().abort();
					removeConfirmation(player, confirmation);
					return true;
				}
			}
			
			// Display message
			if (confirmation.getAcceptMessage() != null) {
				player.sendMessage(confirmation.getAcceptMessage());
			}
			
			// Accept
			confirmation.getHandler().confirm();
			removeConfirmation(player, confirmation);
			return true;
		}
	}
	
	/**
	 * Attempts to reject any pending confirmation the player has.
	 * @param player The player
	 * @return True if a confirmation was handled (either success or fail)
	 */
	public boolean tryAbort(CommandSender player) {
		synchronized (confirmations) {
			Confirmation<?> confirmation = confirmations.get(player);
			
			if (confirmation == null) {
				return false;
			}
			
			// Display message
			if (confirmation.getAbortMessage() != null) {
				player.sendMessage(confirmation.getAbortMessage());
			}
			
			// Abort
			confirmation.getHandler().abort();
			removeConfirmation(player, confirmation);
			return true;
		}
	}
	
	private void removeConfirmation(CommandSender player, Confirmation<?> confirmation) {
		synchronized (confirmations) {
			confirmations.remove(player);
			orderedConfirmations.remove(confirmation.getExpireMessage(), player);
		}
	}
	
	/**
	 * Checks and aborts confirmations if their expiry time has passed.
	 */
	public void expireConfirmations() {
		synchronized (confirmations) {
			Iterator<Long> timeIterator = orderedConfirmations.keySet().iterator();
			while (timeIterator.hasNext()) {
				long time = timeIterator.next();
				
				if (System.currentTimeMillis() >= time) {
					// They have expired
					Set<CommandSender> players = orderedConfirmations.get(time);
					timeIterator.remove();
					
					for (CommandSender player : players) {
						confirmations.remove(player);
					}
				} else {
					// No more have expired
					break;
				}
			}
		}
	}
}
