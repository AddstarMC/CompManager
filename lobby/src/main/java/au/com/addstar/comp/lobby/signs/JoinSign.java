package au.com.addstar.comp.lobby.signs;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import au.com.addstar.comp.lobby.LobbyPlugin;
import au.com.addstar.comp.lobby.services.CompetitionJoinService;
import au.com.addstar.comp.util.Messages;

public class JoinSign extends BaseSign {
	private static final String BREAK_PERMISSION = "comp.signs.join.break";
	
	private final CompManager manager;
	private final CompetitionJoinService joinService;
	private final Messages messages;
	
	public JoinSign(String serverId, Block block, CompManager manager, CompetitionJoinService joinService, Messages messages) {
		super(serverId, block, BREAK_PERMISSION);
		this.manager = manager;
		this.joinService = joinService;
		this.messages = messages;
	}
	
	@Override
	public void onRightClick(final Player player) {
		final CompServer server = manager.getServer(getServerId());
		joinService.initiateJoin(player, server);
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
		}, Bukkit.getScheduler().getMainThreadExecutor(LobbyPlugin.instance));
	}
	
	@Override
	public void load(ConfigurationSection data) {
	}
	
	@Override
	public void save(ConfigurationSection data) {
	}
}
