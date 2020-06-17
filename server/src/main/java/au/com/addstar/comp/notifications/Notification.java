package au.com.addstar.comp.notifications;


import java.util.Optional;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;

import com.plotsquared.core.configuration.ConfigurationSection;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class Notification {
	private static final String TimeLeftToken = "${time-left}";
	private static final String ThemeToken = "${theme}";
	private static final String StateToken = "${state}";
	private static final String EndToken = "${end}";
	
	private String message;
	
	// Conditions
	private Optional<CompState> ifState;
	private Optional<Boolean> ifEntrant;
	private Optional<Boolean> ifFull;
	
	public Notification() {
	}

	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	private static final String TIME_LEFT_FORMAT_LONG = "d'd 'H'h'";
	private static final String TIME_LEFT_FORMAT_SHORT = "H'h 'm'm'";
	private final SimpleDateFormat endFormat = new SimpleDateFormat("d MMM h:ma");
	
	public String formatMessage(CompManager manager) {
		if (message.contains(TimeLeftToken)) {
			long time;
			switch (manager.getState()) {
			case Open:
				time = manager.getCurrentComp().getEndDate();
				break;
			case Voting:
				time = manager.getCurrentComp().getVoteEndDate();
				break;
			default:
				time = 0;
				break;
			}
			
			if (time > 0) {
				time -= System.currentTimeMillis();
				
				String replaceString;
				if (time < TimeUnit.DAYS.toMillis(1)) {
					replaceString = DurationFormatUtils.formatDuration(time, TIME_LEFT_FORMAT_SHORT);
				} else {
					replaceString = DurationFormatUtils.formatDuration(time, TIME_LEFT_FORMAT_LONG);
				}
				
				message = message.replace(TimeLeftToken, replaceString);
			}
		} else if (message.contains(EndToken)) {
			long time;
			switch (manager.getState()) {
			case Open:
				time = manager.getCurrentComp().getEndDate();
				break;
			case Voting:
				time = manager.getCurrentComp().getVoteEndDate();
				break;
			default:
				time = 0;
				break;
			}
			
			if (time != 0) {
				String timeEndString = endFormat.format(time);
				message = message.replace(EndToken, timeEndString);
			}
		} else if (message.contains(ThemeToken)) {
			Competition comp = manager.getCurrentComp();
			if (comp != null) {
				message = message.replace(ThemeToken, comp.getTheme());
			} else {
				message = message.replace(ThemeToken, "Unassigned");
			}
		} else if (message.contains(StateToken)) {
			CompState state = manager.getState();
			
			ChatColor colour;
			switch (state) {
			default:
			case Closed:
				colour = ChatColor.RED;
				break;
			case Open:
				colour = ChatColor.GREEN;
				break;
			case Voting:
				colour = ChatColor.AQUA;
				break;
			}
			
			message = message.replace(StateToken, colour.toString() + ChatColor.BOLD.toString() + state.name().toUpperCase());
		}
		// TODO: Other tokens
		
		return message;
	}
	
	public Optional<CompState> getIfState() {
		return ifState;
	}
	
	public void setIfState(Optional<CompState> state) {
		ifState = state;
	}
	
	public java.util.Optional<Boolean> getIfEntrant() {
		return ifEntrant;
	}
	
	public void setIfEntrant(Optional<Boolean> ifEntrant) {
		this.ifEntrant = ifEntrant;
	}
	
	public Optional<Boolean> getIfFull() {
		return ifFull;
	}
	
	public void setIfFull(Optional<Boolean> ifFull) {
		this.ifFull = ifFull;
	}
	
	public void load(ConfigurationSection section) {
		message = ChatColor.translateAlternateColorCodes('&', section.getString("message"));
		
		// Conditions
		if (section.isString("if-state")) {
			switch (section.getString("if-state").toLowerCase()) {
			case "open":
				ifState = Optional.of(CompState.Open);
				break;
			case "voting":
				ifState = Optional.of(CompState.Voting);
				break;
			case "closed":
				ifState = Optional.of(CompState.Closed);
				break;
			case "visit":
				ifState = Optional.of(CompState.Visit);
				break;
			default:
				ifState = Optional.empty();
				break;
			}
		} else {
			ifState = Optional.empty();
		}
		
		if (section.isBoolean("if-entrant")) {
			ifEntrant = java.util.Optional.of(section.getBoolean("if-entrant"));
		} else {
			ifEntrant = java.util.Optional.empty();
		}
		
		if (section.isBoolean("if-full")) {
			ifFull = Optional.of(section.getBoolean("if-full"));
		} else {
			ifFull = Optional.empty();
		}
	}
}
