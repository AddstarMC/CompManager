package au.com.addstar.comp.commands;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.CompManager;
import au.com.addstar.comp.CompPlugin;
import au.com.addstar.comp.services.PlotBackupService;
import au.com.addstar.comp.services.PlotBackupService.BackupProgressCallback;
import au.com.addstar.comp.services.PlotBackupService.BackupResult;
import au.com.addstar.comp.util.Messages;
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import org.jetbrains.annotations.NotNull;

/**
 * Command for backing up competition plots as schematics.
 */
public class BackupCommand implements ICommand {
	private final CompManager manager;
	private final PlotBackupService backupService;
	private final Messages messages;
	
	public BackupCommand(CompManager manager, PlotBackupService backupService, Messages messages) {
		this.manager = manager;
		this.backupService = backupService;
		this.messages = messages;
	}
	
	@Override
	public String getName() {
		return "backup";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "comp.admin.backup";
	}

	@Override
	public String getUsageString(String label, CommandSender sender) {
		return label;
	}

	@Override
	public String getDescription() {
		return "Backs up all plots for the current competition as schematics";
	}

	@Override
	public EnumSet<CommandSenderType> getAllowedSenders() {
		return EnumSet.allOf(CommandSenderType.class);
	}

	@Override
	public boolean onCommand(CommandSender sender, String parent, String label, String[] args) throws BadArgumentException {
		// Check if backup is already in progress
		if (backupService.isBackupInProgress()) {
			sender.sendMessage(messages.get("backup.in-progress"));
			return true;
		}
		
		// Check if competition exists
		au.com.addstar.comp.Competition comp = manager.getCurrentComp();
		if (comp == null) {
			sender.sendMessage(messages.get("backup.no-competition"));
			return true;
		}
		
		// Create progress callback for user feedback
		BackupProgressCallback progressCallback = new BackupProgressCallback() {
			@Override
			public void onProgress(int completed, int total, int successful, int failed) {
				sender.sendMessage(messages.get("backup.progress")
					.replace("{completed}", String.valueOf(completed))
					.replace("{total}", String.valueOf(total))
					.replace("{successful}", String.valueOf(successful))
					.replace("{failed}", String.valueOf(failed)));
			}
		};
		
		// Start backup
		sender.sendMessage(messages.get("backup.starting")
			.replace("{theme}", comp.getTheme())
			.replace("{compId}", String.valueOf(comp.getCompId())));
		
		ListenableFuture<BackupResult> future = backupService.backupPlots(comp, progressCallback, null);
		
		Futures.addCallback(future, new FutureCallback<BackupResult>() {
			@Override
			public void onSuccess(BackupResult result) {
				// Format success message
				String message;
				if (result.getFailedBackups() == 0) {
					message = messages.get("backup.success")
						.replace("{count}", String.valueOf(result.getSuccessfulBackups()))
						.replace("{path}", result.getArchiveDirectory().getAbsolutePath());
				} else {
					message = messages.get("backup.partial")
						.replace("{successful}", String.valueOf(result.getSuccessfulBackups()))
						.replace("{failed}", String.valueOf(result.getFailedBackups()))
						.replace("{total}", String.valueOf(result.getTotalPlots()))
						.replace("{path}", result.getArchiveDirectory().getAbsolutePath());
				}
				sender.sendMessage(message);
			}
			
			@Override
			public void onFailure(@NotNull Throwable error) {
				String errorMessage;
				if (error instanceof IllegalStateException || error instanceof IllegalArgumentException) {
					errorMessage = messages.get("backup.failed")
						.replace("{error}", error.getMessage());
				} else {
					errorMessage = messages.get("backup.failed.unexpected");
					CompPlugin.instance.getLogger().log(java.util.logging.Level.SEVERE, "Backup failed", error);
				}
				sender.sendMessage(errorMessage);
			}
		}, org.bukkit.Bukkit.getScheduler().getMainThreadExecutor(CompPlugin.instance));
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String parent, String label, String[] args) {
		return null;
	}
}
