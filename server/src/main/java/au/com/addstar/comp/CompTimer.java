package au.com.addstar.comp;

import au.com.addstar.comp.notifications.NotificationManager;

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
		
		if (state != lastState) {
			notifications.broadcastStateChange(state);
			compManager.notifyStateChange(lastState);
			lastState = state;
		}
	}
}
