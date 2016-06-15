package au.com.addstar.comp;

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
		doStateNotifications(comp);
		
		notifications.doBroadcasts();
	}
	
	private void doStateNotifications(Competition comp) {
		CompState state = comp.getState();
		
		// TODO: Customizable messages
		if (state != lastState) {
			switch (state) {
			case Closed:
				notifications.broadcast("Placeholder: Comp is now closed", DisplayTarget.Subtitle, 5000);
				break;
			case Open:
				notifications.broadcast("Placeholder: Comp is now open", DisplayTarget.Subtitle, 5000);
				break;
			case Voting:
				notifications.broadcast("Placeholder: Comp has finished. Voting is now open", DisplayTarget.Subtitle, 5000);
				break;
			}
			compManager.notifyStateChange(lastState);
			lastState = state;
		}
	}
}
