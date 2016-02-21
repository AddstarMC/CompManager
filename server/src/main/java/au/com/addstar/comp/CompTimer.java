package au.com.addstar.comp;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;

import au.com.addstar.comp.notifications.NotificationManager;
import au.com.addstar.comp.notifications.NotificationManager.DisplayTarget;

public class CompTimer implements Runnable {
	private final CompManager compManager;
	private final NotificationManager notifications;
	
	private CompState lastState;
	
	public CompTimer(CompManager compManager, NotificationManager notifications) {
		this.compManager = compManager;
		this.notifications = notifications;
		lastState = CompState.Closed;
	}
	
	@Override
	public void run() {
		if (compManager.getCurrentComp() == null) {
			return;
		}
		
		Competition comp = compManager.getCurrentComp();
		if (comp.isAutomatic()) {
			doNotifications(comp);
		}
	}
	
	private static final String TIME_LEFT_FORMAT_LONG = "d'd 'H'h'";
	private static final String TIME_LEFT_FORMAT_SHORT = "'H'h 'm'm'";
	
	private void doNotifications(Competition comp) {
		CompState state = comp.getState();
		
		// TODO: Customizable messages
		if (state != lastState) {
			switch (state) {
			case Closed:
				notifications.broadcast("Placeholder: Comp is now closed", DisplayTarget.Subtitle);
				break;
			case Open:
				notifications.broadcast("Placeholder: Comp is now open", DisplayTarget.Subtitle);
				break;
			case Voting:
				notifications.broadcast("Placeholder: Comp has finished. Voting is now open", DisplayTarget.Subtitle);
				break;
			}
			lastState = state;
		}
		
		// TODO: Below is a test notification. Need to implement customizable notifications
		long time;
		String formatString;
		switch (state) {
		case Open:
			time = comp.getEndDate();
			formatString = "Time remaining: %s";
			break;
		case Voting:
			time = comp.getVoteEndDate();
			formatString = "Voting closes in %s";
			break;
		default:
			return;
		}
		
		time -= System.currentTimeMillis();
		
		if (time < TimeUnit.DAYS.toMillis(1)) {
			formatString = String.format(formatString, DurationFormatUtils.formatDuration(time, TIME_LEFT_FORMAT_SHORT));
		} else {
			formatString = String.format(formatString, DurationFormatUtils.formatDuration(time, TIME_LEFT_FORMAT_LONG));
		}
		
		notifications.broadcast(formatString, DisplayTarget.ActionBar, compManager.Entrant);
	}
}
