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
import au.com.addstar.monolith.command.BadArgumentException;
import au.com.addstar.monolith.command.CommandSenderType;
import au.com.addstar.monolith.command.ICommand;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

/**
 * Command for backing up competition plots as schematics.
 */
public class BackupCommand implements ICommand {
	private final CompManager manager;
	private final PlotBackupService backupService;
	
	public BackupCommand(CompManager manager, PlotBackupService backupService) {
		this.manager = manager;
		this.backupService = backupService;
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
			sender.sendMessage(ChatColor.RED + "A backup is already in progress. Please wait for it to complete.");
			return true;
		}
		
		// Check if competition exists
		au.com.addstar.comp.Competition comp = manager.getCurrentComp();
		if (comp == null) {
			sender.sendMessage(ChatColor.RED + "There is no active competition on this server.");
			return true;
		}
		
		// Create progress callback for user feedback
		BackupProgressCallback progressCallback = new BackupProgressCallback() {
			@Override
			public void onProgress(int completed, int total, int successful, int failed) {
				sender.sendMessage(ChatColor.GOLD + String.format("Backup progress: %d/%d plots processed (%d successful, %d failed)",
					completed, total, successful, failed));
			}
		};
		
		// Start backup
		sender.sendMessage(ChatColor.GOLD + "Starting plot backup for competition: " + comp.getTheme() + " (ID: " + comp.getCompId() + ")");
		
		ListenableFuture<BackupResult> future = backupService.backupPlots(comp, progressCallback, null);
		
		Futures.addCallback(future, new FutureCallback<BackupResult>() {
			@Override
			public void onSuccess(BackupResult result) {
				// Format success message
				String message;
				if (result.getFailedBackups() == 0) {
					message = ChatColor.GREEN + String.format(
						"Backup completed successfully! %d plots backed up to: %s",
						result.getSuccessfulBackups(),
						result.getArchiveDirectory().getAbsolutePath()
					);
				} else {
					message = ChatColor.YELLOW + String.format(
						"Backup completed with some failures. %d successful, %d failed out of %d total. Files saved to: %s",
						result.getSuccessfulBackups(),
						result.getFailedBackups(),
						result.getTotalPlots(),
						result.getArchiveDirectory().getAbsolutePath()
					);
				}
				sender.sendMessage(message);
			}
			
			@Override
			public void onFailure(@NotNull Throwable error) {
				String errorMessage;
				if (error instanceof IllegalStateException) {
					errorMessage = ChatColor.RED + "Backup failed: " + error.getMessage();
				} else if (error instanceof IllegalArgumentException) {
					errorMessage = ChatColor.RED + "Backup failed: " + error.getMessage();
				} else {
					errorMessage = ChatColor.RED + "Backup failed due to an unexpected error. Check console for details.";
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
