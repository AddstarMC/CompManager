package au.com.addstar.comp.lobby.signs;

import org.apache.commons.lang.WordUtils;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import net.md_5.bungee.api.ChatColor;

public class JoinSign extends BaseSign {
	private static final String BREAK_PERMISSION = "comp.signs.join.break";
	
	private final CompManager manager;
	
	public JoinSign(String serverId, Block block, CompManager manager) {
		super(serverId, block, BREAK_PERMISSION);
		this.manager = manager;
	}
	
	@Override
	public void onRightClick(Player player) {
		player.sendMessage("TODO: Send to comp world");
		// TODO Send player to comp
	}

	@Override
	public void refresh() {
		final CompServer server = manager.getServer(getServerId());
		if (server == null || server.getCurrentComp() == null) {
			clear();
			setLine(2, ChatColor.DARK_RED.toString() + ChatColor.BOLD + "CLOSED");
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
					setLine(2, ChatColor.DARK_RED.toString() + ChatColor.BOLD + "CLOSED");
					break;
				case Open:
					if (!isFull) {
						setLine(2, ChatColor.BLUE.toString() + ChatColor.BOLD + "OPEN");
						setLine(3, ChatColor.DARK_BLUE.toString() + ChatColor.ITALIC + "Click to Join");
					} else {
						setLine(2, ChatColor.DARK_RED.toString() + ChatColor.BOLD + "FULL");
						setLine(3, ChatColor.DARK_BLUE.toString() + ChatColor.ITALIC + "Click to Visit");
					}
					break;
				case Voting:
					setLine(2, ChatColor.DARK_AQUA.toString() + ChatColor.BOLD + "VOTING");
					setLine(3, ChatColor.DARK_BLUE.toString() + ChatColor.ITALIC + "Click to Join");
					break;
				}
				
				update();
			}
			
			@Override
			public void onFailure(Throwable error) {
				setLine(2, ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Offline");
				
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
