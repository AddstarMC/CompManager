package au.com.addstar.comp.lobby.services;

import java.util.concurrent.TimeUnit;

import au.com.addstar.comp.lobby.LobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.confirmations.Confirmable;
import au.com.addstar.comp.confirmations.Confirmation;
import au.com.addstar.comp.confirmations.ConfirmationManager;
import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.entry.EntryDeniedException;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.util.Messages;

/**
 * Service for handling competition join logic.
 */
public class CompetitionJoinService {
	private final ConfirmationManager confirmationManager;
	private final Messages messages;
	
	public CompetitionJoinService(ConfirmationManager confirmationManager, Messages messages) {
		this.confirmationManager = confirmationManager;
		this.messages = messages;
	}
	
	/**
	 * Initiates the join process for a player.
	 * Validates the competition state, attempts to join, and sets up confirmation.
	 * 
	 * @param player The player attempting to join
	 * @param server The competition server
	 * @return A Future that completes with a Confirmable handler on success, or fails with an EntryDeniedException
	 */
	public ListenableFuture<Confirmable> initiateJoin(final Player player, final CompServer server) {
		// Validate server and competition state
		if (server == null || server.getCurrentComp() == null || !server.isOnline()) {
			player.sendMessage(messages.get("join.denied.not-running"));
			return Futures.immediateFailedFuture(new EntryDeniedException(EntryDeniedException.Reason.NotRunning, "Server or competition not available"));
		}
		
		if (server.getCurrentComp().getState() != CompState.Open) {
			player.sendMessage(messages.get("join.denied.not-running"));
			return Futures.immediateFailedFuture(new EntryDeniedException(EntryDeniedException.Reason.NotRunning, "Competition is not open"));
		}

		// Silently return if player has pending confirmations
		if (confirmationManager.hasPendingConfirmations(player)) {
			return Futures.immediateFailedFuture(new EntryDeniedException(EntryDeniedException.Reason.NotRunning, "Player has pending confirmations"));
		}
		
		// Attempt to join the player into the comp
		ListenableFuture<Confirmable> future = server.joinComp(player, messages);
		
		Futures.addCallback(future, new FutureCallback<Confirmable>() {
			@Override
			public void onSuccess(Confirmable enterHandler) {
				handlePlayerJoin(enterHandler, player, server);
			}
			
			@Override
			public void onFailure(@NotNull Throwable error) {
				// Expected error
				if (error instanceof EntryDeniedException) {
					switch (((EntryDeniedException)error).getReason()) {
					case NotRunning:
						player.sendMessage(messages.get("join.denied.not-running"));
						break;
					case AlreadyEntered:
						server.send(player);
						// TODO: Teleport to own plot
						break;
					case Full:
						player.sendMessage(messages.get("join.denied.full"));
						break;
					case Whitelist:
						player.sendMessage(messages.get("join.denied.whitelist"));
						break;
					}
				} else {
					player.sendMessage(ChatColor.RED + "Sorry, something went wrong. Please try again later.");
					LobbyPlugin.instance.getLogger().log(java.util.logging.Level.SEVERE, "Failed to join " + player.getName() + " into the comp on " + server.getId(), error);
				}
			}
		}, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
		
		return future;
	}
	
	/**
	 * Handles the successful join initiation by displaying competition information
	 * and setting up the confirmation requirement.
	 * 
	 * @param enterHandler The confirmation handler
	 * @param player The player joining
	 * @param server The competition server
	 */
	private void handlePlayerJoin(Confirmable enterHandler, Player player, CompServer server) {
		Competition comp = server.getCurrentComp();
		
		// Show the comp information
		player.sendMessage(messages.get("join.prompt.header", "theme", comp.getTheme()));
		player.sendMessage(messages.get("join.prompt.theme", "theme", comp.getTheme()));
		
		// Display prize
		if (comp.getFirstPrize() != null ) {
			String firstPrize = comp.getFirstPrize().toHumanReadable();
			String secondPrize;
			if (comp.getSecondPrize() != null) {
				secondPrize = comp.getSecondPrize().toHumanReadable();
			} else {
				secondPrize = "none";
			}
			
			player.sendMessage(messages.get("join.prompt.prize", "prize1", firstPrize, "prize2", secondPrize));
		}
		player.sendMessage(messages.get("join.prompt.ends", "time", CompUtils.formatDate(comp.getEndDate()), "timeleft", CompUtils.formatTimeRemaining(comp.getEndDate() - System.currentTimeMillis())));
		
		// Display criteria
		if (!comp.getCriteria().isEmpty()) {
			player.sendMessage(messages.get("join.prompt.criteria.header"));
			for (BaseCriterion criterion : comp.getCriteria()) {
				player.sendMessage(messages.get("join.prompt.criteria.format", "name", criterion.getName(), "description", criterion.getDescription()));
			}
		}
		
		player.sendMessage(messages.get("join.prompt.footer", "theme", comp.getTheme()));
		
		Confirmation<Confirmable> confirmation = Confirmation.builder(enterHandler)
				.expiresIn(20, TimeUnit.SECONDS)
				.withExpireMessage(messages.get("join.denied.timeout"))
				.withRequiredToken(comp.getTheme())
				.withTokenFailMessage(messages.get("join.denied.token"))
				.build();
		
		confirmationManager.addConfirmation(player, confirmation);
	}
}
