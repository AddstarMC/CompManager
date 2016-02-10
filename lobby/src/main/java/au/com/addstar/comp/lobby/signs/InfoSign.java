package au.com.addstar.comp.lobby.signs;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.lobby.CompManager;
import au.com.addstar.comp.lobby.CompServer;
import net.md_5.bungee.api.ChatColor;

public class InfoSign extends BaseSign {
	private static final String BREAK_PERMISSION = "comp.signs.info.break";
	
	private final CompManager manager;
	private InfoType type;
	
	public InfoSign(String serverId, Block block, CompManager manager) {
		super(serverId, block, BREAK_PERMISSION);
		this.manager = manager;
	}
	
	public InfoSign(String serverId, InfoType type, Block block, CompManager manager) {
		super(serverId, block, BREAK_PERMISSION);
		this.manager = manager;
		this.type = type;
	}
	
	/**
	 * Gets the type of info this sign displays
	 * @return The InfoType
	 */
	public InfoType getType() {
		return type;
	}
	
	/**
	 * Sets the type of info this sign displays
	 * @param type The info type
	 */
	public void setType(InfoType type) {
		this.type = type;
	}
	
	@Override
	public void refresh() {
		CompServer server = manager.getServer(getServerId());
		if (server == null || server.getCurrentComp() == null || server.getCurrentComp().getState() == CompState.Closed) {
			clear();
			setLine(1, "Please Check");
			setLine(2, "Back Later");
			update();
			return;
		}
		
		switch (type) {
		case PlotsLeft:
			displayPlotsLeft(server);
			break;
		case PlotsUsedTotal:
			displayPlotsUsedTotal(server);
			break;
		case TimeEnd:
			displayTimeEnd(server);
			break;
		case TimeEndLeft:
			displayTimeBoth(server);
			break;
		case TimeLeft:
			displayTimeLeft(server);
			break;
		}
	}
	
	private void displayPlotsLeft(final CompServer server) {
		Futures.addCallback(server.getEntrantCount(), new FutureCallback<Integer>() {
			@Override
			public void onSuccess(Integer count) {
				clear();
				
				// Make it plots remaining
				count = server.getCurrentComp().getMaxEntrants() - count;
				
				setLine(1, "Plots Remaining");
				if (count == 0) {
					setLine(2, ChatColor.DARK_RED.toString() + ChatColor.BOLD.toString() + "NONE");					
				} else {
					setLine(2, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + String.valueOf(count));
				}
				update();
			}
			
			@Override
			public void onFailure(Throwable error) {
				clear();
				setLine(1, "Error");
				update();
				error.printStackTrace();
			}
		});
	}
	
	private void displayPlotsUsedTotal(final CompServer server) {
		Futures.addCallback(server.getEntrantCount(), new FutureCallback<Integer>() {
			@Override
			public void onSuccess(Integer count) {
				clear();
				setLine(1, "Plots Used");
				int total = server.getCurrentComp().getMaxEntrants();
				
				setLine(2, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + count + "/" + total);
				update();
			}
			
			@Override
			public void onFailure(Throwable error) {
				clear();
				setLine(1, "Error");
				error.printStackTrace();
				update();
			}
		});
	}
	
	private final SimpleDateFormat endFormat = new SimpleDateFormat("d MMM h:ma");
	
	private void displayTimeEnd(CompServer server) {
		Competition comp = server.getCurrentComp();
		clear();
		long time;
		switch (comp.getState()) {
		default:
		case Closed:
			return;
		case Open:
			setLine(1, "Ends");
			time = comp.getEndDate();
			break;
		case Voting:
			// TODO: Need to have access to voting end time
			return;
		}
		
		setLine(2, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + endFormat.format(time));
		update();
	}
	
	private static final String TIME_LEFT_FORMAT_LONG = "d'd 'H'h'";
	private static final String TIME_LEFT_FORMAT_SHORT = "'H'h 'm'm'";
	
	private void displayTimeLeft(CompServer server) {
		Competition comp = server.getCurrentComp();
		clear();
		long time;
		switch (comp.getState()) {
		default:
		case Closed:
			return;
		case Open:
			setLine(1, "Time Left");
			time = comp.getEndDate();
			break;
		case Voting:
			// TODO: Need to have access to voting end time
			return;
		}
		
		// Time remaining
		time -= System.currentTimeMillis();
		
		if (time < TimeUnit.DAYS.toMillis(1)) {
			setLine(2, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + DurationFormatUtils.formatDuration(time, TIME_LEFT_FORMAT_SHORT));
		} else {
			setLine(2, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + DurationFormatUtils.formatDuration(time, TIME_LEFT_FORMAT_LONG));
		}
		
		update();
	}
	
	private void displayTimeBoth(CompServer server) {
		Competition comp = server.getCurrentComp();
		clear();
		long time;
		switch (comp.getState()) {
		default:
		case Closed:
			return;
		case Open:
			setLine(0, "Ends");
			setLine(2, "Time Left");
			time = comp.getEndDate();
			break;
		case Voting:
			// TODO: Need to have access to voting end time
			return;
		}
		
		setLine(1, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + endFormat.format(time));
		
		long remaining = time - System.currentTimeMillis();
		
		if (remaining < TimeUnit.DAYS.toMillis(1)) {
			setLine(3, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + DurationFormatUtils.formatDuration(remaining, TIME_LEFT_FORMAT_SHORT));
		} else {
			setLine(3, ChatColor.DARK_BLUE.toString() + ChatColor.BOLD.toString() + DurationFormatUtils.formatDuration(remaining, TIME_LEFT_FORMAT_LONG));
		}
		
		update();
	}
	
	@Override
	public void load(ConfigurationSection data) {
		type = InfoType.valueOf(data.getString("infotype"));
	}
	
	@Override
	public void save(ConfigurationSection data) {
		data.set("infotype", type.name());
	}
	
	public enum InfoType {
		/**
		 * Displays the number of plots left
		 */
		PlotsLeft,
		/**
		 * Displays the number of plots used out of the total number of plots
		 */
		PlotsUsedTotal,
		/**
		 * Displays the time left
		 */
		TimeLeft,
		/**
		 * Displays the time and date of the end
		 */
		TimeEnd,
		/**
		 * Displays the time and date of the end as well as time left
		 */
		TimeEndLeft
	}
}
