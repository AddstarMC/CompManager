package au.com.addstar.comp.notifications;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompState;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.util.CompUtils;

import com.plotsquared.core.configuration.ConfigurationSection;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class StateChangeNotification {
	private static final String TimeLeftToken = "${time-left}";
	private static final String ThemeToken = "${theme}";
	private static final String StateToken = "${state}";
	private static final String EndToken = "${end}";

	private String message;
	private NotificationManager.DisplayTarget displayTarget;
	private long displayTime;

	public StateChangeNotification() {
	}

	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public long getDisplayTime() {
		return displayTime;
	}

	public NotificationManager.DisplayTarget getDisplayTarget() {
		return displayTarget;
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

	public void load(ConfigurationSection section) {
		message = ChatColor.translateAlternateColorCodes('&', section.getString("message"));

		switch (section.getString("location")) {
		case "action":
			displayTarget = NotificationManager.DisplayTarget.ActionBar;
			break;
		case "chat":
			displayTarget = NotificationManager.DisplayTarget.Chat;
			break;
		case "system":
			displayTarget = NotificationManager.DisplayTarget.SystemMessage;
			break;
		case "title":
			displayTarget = NotificationManager.DisplayTarget.Title;
			break;
		case "subtitle":
			displayTarget = NotificationManager.DisplayTarget.Subtitle;
			break;
		}

		try {
			displayTime = CompUtils.parseDateDiff(section.getString("display-time", "5s"));
		} catch (IllegalArgumentException e) {
			// Fallback to default value
			displayTime = TimeUnit.SECONDS.toMillis(5);
			// TODO: Notify of error
		}
	}
}
