package au.com.addstar.comp.lobby.signs;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
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
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.util.Messages;

public class JoinSign extends BaseSign {
	private static final String BREAK_PERMISSION = "comp.signs.join.break";
	
	private final CompManager manager;
	private final Messages messages;
	private final ConfirmationManager confirmationManager;
	
	public JoinSign(String serverId, Block block, CompManager manager, Messages messages, ConfirmationManager confirmationManager) {
		super(serverId, block, BREAK_PERMISSION);
		this.manager = manager;
		this.messages = messages;
		this.confirmationManager = confirmationManager;
	}
	
	@Override
	public void onRightClick(final Player player) {
		final CompServer server = manager.getServer(getServerId());
		if (server == null || server.getCurrentComp() == null || !server.isOnline()) {
			player.sendMessage(messages.get("join.denied.not-running"));
			return;
		}
		
		if (server.getCurrentComp().getState() != CompState.Open) {
			player.sendMessage(messages.get("join.denied.not-running"));
			return;
		}

		// Silently return, let it be handled
		if (confirmationManager.hasPendingConfirmations(player)) {
			return;
		}
		
		// Attempt to join the player into the comp
		ListenableFuture<Confirmable> future = server.joinComp(player, messages);
		
		Futures.addCallback(future, new FutureCallback<Confirmable>() {
			@Override
			public void onSuccess(Confirmable enterHandler) {
				handlePlayerJoin(enterHandler, player);
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
					System.err.println("Failed to join " + player.getName() + " into the comp on " + server.getId() + ":");
					error.printStackTrace();
				}
			}
		});
	}
	
	private void handlePlayerJoin(Confirmable enterHandler, Player player) {
		final CompServer server = manager.getServer(getServerId());
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

	@Override
	public void refresh() {
		final CompServer server = manager.getServer(getServerId());
		if (server == null || server.getCurrentComp() == null) {
			clear();
			setLine(2, messages.get("state.closed"));
			update();
			return;
		}
		
		if (!server.isOnline()) {
			clear();
			setLine(2, messages.get("state.offline"));
			update();
			return;
		}
		
		final Competition comp = server.getCurrentComp();
		
		// Get the player count for checking full status
		ListenableFuture<Integer> countFuture;
		if (comp.getState() == CompState.Open) {
			countFuture = server.getEntrantCount();
		} else {
			countFuture = Futures.immediateFuture(0);
		}
		
		Futures.addCallback(countFuture, new FutureCallback<Integer>() {
			@Override
			public void onSuccess(Integer count) {
				clear();
				
				// Display the theme over the first and second lines
				String theme = comp.getTheme();
				String[] lines = WordUtils.wrap(theme, 15, "\001", true).split("\001");
				for (int i = 0; i < lines.length && i < 2; ++i) {
					setLine(i, lines[i]);
				}
				
				boolean isFull = count >= comp.getMaxEntrants();
				
				// Display status
				switch (comp.getState()) {
				case Closed:
					setLine(2, messages.get("state.closed"));
					setLine(3, "");
					break;
				case Open:
					if (!isFull) {
						setLine(2, messages.get("state.open"));
						setLine(3, messages.get("sign.click-to-join"));
					} else {
						setLine(2, messages.get("state.full"));
						setLine(3, "");
					}
					break;
				case Voting:
					setLine(2, messages.get("state.voting"));
					setLine(3, "");
					break;
				case Visit:
					setLine(2, messages.get("state.visit"));
					setLine(3, "");
					break;
				}
				
				update();
			}
			
			@Override
			public void onFailure(@NotNull Throwable error) {
				setLine(2, messages.get("state.offline"));
				setLine(3, "");
				update();
			}
		});
	}
	
	@Override
	public void load(ConfigurationSection data) {
	}
	
	@Override
	public void save(ConfigurationSection data) {
	}
}
