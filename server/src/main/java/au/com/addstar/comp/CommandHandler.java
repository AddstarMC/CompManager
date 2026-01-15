package au.com.addstar.comp;

import java.util.logging.Level;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bukkit.Bukkit;

import au.com.addstar.comp.redis.CommandReceiver;
import au.com.addstar.comp.services.PlotBackupService;
import au.com.addstar.comp.services.PlotResetService;

public class CommandHandler implements CommandReceiver {
	private final CompManager manager;
	private final PlotBackupService backupService;
	private final PlotResetService resetService;
	
	public CommandHandler(CompManager manager, PlotBackupService backupService, PlotResetService resetService) {
		this.manager = manager;
		this.backupService = backupService;
		this.resetService = resetService;
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
		case "reset":
			handleResetCommand();
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
	
	private void handleResetCommand() {
		au.com.addstar.comp.Competition comp = manager.getCurrentComp();
		if (comp == null) {
			CompPlugin.instance.getLogger().warning("[Reset] Redis reset command received but no competition is active");
			return;
		}
		
		CompPlugin.instance.getLogger().info("[Reset] Redis reset command received for competition: " + comp.getTheme() + " (ID: " + comp.getCompId() + ")");
		
		ListenableFuture<Void> future = resetService.resetPlots(comp, null, null);
		
		Futures.addCallback(future, new FutureCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				CompPlugin.instance.getLogger().info("[Reset] Redis reset command completed successfully for competition: " + comp.getTheme());
			}
			
			@Override
			public void onFailure(Throwable t) {
				CompPlugin.instance.getLogger().log(Level.SEVERE, 
					"[Reset] Redis reset command failed for competition: " + comp.getTheme() + " (ID: " + comp.getCompId() + ")", t);
			}
		}, Bukkit.getScheduler().getMainThreadExecutor(CompPlugin.instance));
	}
}
