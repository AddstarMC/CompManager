package au.com.addstar.comp;

import au.com.addstar.comp.redis.CommandReceiver;
import au.com.addstar.comp.services.PlotBackupService;

public class CommandHandler implements CommandReceiver {
	private final CompManager manager;
	private final PlotBackupService backupService;
	
	public CommandHandler(CompManager manager, PlotBackupService backupService) {
		this.manager = manager;
		this.backupService = backupService;
	}
	
	@Override
	public void onReceive(String serverId, String command) {
		switch (command.toLowerCase()) {
		case "reload":
			manager.reloadCurrentComp();
			break;
		case "backup":
			handleBackupCommand();
			break;
		}
	}
	
	private void handleBackupCommand() {
		au.com.addstar.comp.Competition comp = manager.getCurrentComp();
		if (comp == null) {
			CompPlugin.instance.getLogger().warning("[Backup] Redis backup command received but no competition is active");
			return;
		}
		
		CompPlugin.instance.getLogger().info("[Backup] Redis backup command received for competition: " + comp.getTheme() + " (ID: " + comp.getCompId() + ")");
		
		backupService.backupPlots(comp, null, null);
	}
}
