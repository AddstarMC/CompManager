package au.com.addstar.comp;

import au.com.addstar.comp.redis.CommandReceiver;

public class CommandHandler implements CommandReceiver {
	private final CompManager manager;
	
	public CommandHandler(CompManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void onReceive(String serverId, String command) {
		switch (command.toLowerCase()) {
		case "reload":
			manager.reloadCurrentComp();
			break;
		}
	}
}
