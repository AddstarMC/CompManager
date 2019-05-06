package au.com.addstar.comp.lobby;

import au.com.addstar.comp.redis.CommandReceiver;

public class CommandHandler implements CommandReceiver {
	private final CompManager manager;
	
	public CommandHandler(CompManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void onReceive(String serverId, String command) {
		switch (command.toLowerCase()) {
		case "reloadsender":
			reload(serverId);
			break;
		case "reloadall":
			manager.reload(true);
			break;
		}
	}
	
	private void reload(String serverId) {
		CompServer server = manager.getServer(serverId);
		if (server == null) {
			// Reload all because a server may have just been assigned
			manager.reload(true);
		} else {
			// Reload just this server
			server.reload();
		}
	}
}
