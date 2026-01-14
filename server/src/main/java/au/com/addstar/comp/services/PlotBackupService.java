package au.com.addstar.comp.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.sk89q.jnbt.CompoundTag;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.util.P2Bridge;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.util.SchematicHandler;

/**
 * Service for backing up plots as schematics.
 * Handles async operations, error recovery, and progress tracking.
 */
public class PlotBackupService {
	
	/**
	 * Callback interface for progress updates during backup operations.
	 */
	public interface BackupProgressCallback {
		/**
		 * Called periodically during backup to report progress.
		 * @param completed Number of plots processed so far
		 * @param total Total number of plots to process
		 * @param successful Number of successful backups
		 * @param failed Number of failed backups
		 */
		void onProgress(int completed, int total, int successful, int failed);
	}
	
	/**
	 * Result of a backup operation.
	 */
	public static class BackupResult {
		private final int totalPlots;
		private final int successfulBackups;
		private final int failedBackups;
		private final File archiveDirectory;
		
		public BackupResult(int totalPlots, int successfulBackups, int failedBackups, File archiveDirectory) {
			this.totalPlots = totalPlots;
			this.successfulBackups = successfulBackups;
			this.failedBackups = failedBackups;
			this.archiveDirectory = archiveDirectory;
		}
		
		public int getTotalPlots() {
			return totalPlots;
		}
		
		public int getSuccessfulBackups() {
			return successfulBackups;
		}
		
		public int getFailedBackups() {
			return failedBackups;
		}
		
		public File getArchiveDirectory() {
			return archiveDirectory;
		}
	}
	
	private final P2Bridge bridge;
	private final JavaPlugin plugin;
	private final Logger logger;
	private final boolean backupEmptyPlots;
	private final int progressInterval;
	
	private volatile boolean isBackingUp = false;
	private final Object backupLock = new Object();
	
	public PlotBackupService(P2Bridge bridge, JavaPlugin plugin, Logger logger, boolean backupEmptyPlots, int progressInterval) {
		this.bridge = bridge;
		this.plugin = plugin;
		this.logger = logger;
		this.backupEmptyPlots = backupEmptyPlots;
		this.progressInterval = progressInterval;
	}
	
	/**
	 * Checks if a backup operation is currently in progress.
	 * @return True if a backup is in progress
	 */
	public boolean isBackupInProgress() {
		return isBackingUp;
	}
	
	/**
	 * Backs up all owned plots for the given competition.
	 * @param competition The competition to backup plots for
	 * @param progressCallback Optional callback for progress updates (can be null)
	 * @param onComplete Optional callback to run when backup completes (can be null)
	 * @return A future that completes with the backup result
	 */
	public ListenableFuture<BackupResult> backupPlots(Competition competition, BackupProgressCallback progressCallback, Runnable onComplete) {
		synchronized (backupLock) {
			if (isBackingUp) {
				return Futures.immediateFailedFuture(
					new IllegalStateException("Backup already in progress")
				);
			}
			isBackingUp = true;
		}
		
		SettableFuture<BackupResult> resultFuture = SettableFuture.create();
		
		// Validate competition
		if (competition == null || competition.getCompId() < 0) {
			synchronized (backupLock) {
				isBackingUp = false;
			}
			return Futures.immediateFailedFuture(
				new IllegalArgumentException("Invalid competition: competition is null or has invalid compId")
			);
		}
		
		final int compId = competition.getCompId();
		
		// Run backup on async thread
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				performBackup(competition, compId, progressCallback, resultFuture);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Unexpected error during backup", e);
				resultFuture.setException(e);
			} finally {
				synchronized (backupLock) {
					isBackingUp = false;
				}
				if (onComplete != null) {
					Bukkit.getScheduler().runTask(plugin, onComplete);
				}
			}
		});
		
		return resultFuture;
	}
	
	private void performBackup(Competition competition, int compId, BackupProgressCallback progressCallback, SettableFuture<BackupResult> resultFuture) {
		// Get all owned plots
		List<Plot> plots = bridge.getAllOwnedPlots();
		
		if (plots.isEmpty()) {
			logger.info("[Backup] No plots to backup for competition " + compId);
			File archiveDir = new File(plugin.getDataFolder(), "archives/" + compId);
			resultFuture.set(new BackupResult(0, 0, 0, archiveDir));
			return;
		}
		
		// Create archive directory
		File archiveDir = new File(plugin.getDataFolder(), "archives/" + compId);
		if (!archiveDir.exists()) {
			if (!archiveDir.mkdirs()) {
				String error = "Failed to create archive directory: " + archiveDir.getAbsolutePath();
				logger.log(Level.SEVERE, "[Backup] " + error);
				resultFuture.setException(new IOException(error));
				return;
			}
		}
		
		logger.info("[Backup] Starting backup of " + plots.size() + " plots for competition " + compId);
		
		AtomicInteger successful = new AtomicInteger(0);
		AtomicInteger failed = new AtomicInteger(0);
		
		SchematicHandler schematicHandler = bridge.getSchematicHandler();
		
		// Filter plots if needed
		List<Plot> plotsToBackup = new ArrayList<>();
		for (Plot plot : plots) {
			if (backupEmptyPlots || !isPlotEmpty(plot)) {
				plotsToBackup.add(plot);
			}
		}
		
		if (plotsToBackup.isEmpty()) {
			logger.info("[Backup] No plots to backup after filtering");
			resultFuture.set(new BackupResult(plots.size(), 0, 0, archiveDir));
			return;
		}
		
		// Collect all backup futures
		List<CompletableFuture<Boolean>> backupFutures = new ArrayList<>();
		
		for (Plot plot : plotsToBackup) {
			CompletableFuture<Boolean> plotFuture = backupPlotAsync(plot, archiveDir, schematicHandler);
			backupFutures.add(plotFuture);
			
			// Progress updates as futures complete
			plotFuture.whenComplete((success, throwable) -> {
				if (success != null && success) {
					successful.incrementAndGet();
				} else {
					failed.incrementAndGet();
				}
				
				int completed = successful.get() + failed.get();
				
				// Progress updates
				if (completed % progressInterval == 0 || completed == plotsToBackup.size()) {
					logger.info(String.format("[Backup] Progress: %d/%d plots processed (%d successful, %d failed)",
						completed, plotsToBackup.size(), successful.get(), failed.get()));
					
					if (progressCallback != null) {
						final int finalCompleted = completed;
						final int finalSuccessful = successful.get();
						final int finalFailed = failed.get();
						Bukkit.getScheduler().runTask(plugin, () -> {
							progressCallback.onProgress(finalCompleted, plotsToBackup.size(), finalSuccessful, finalFailed);
						});
					}
				}
			});
		}
		
		// Wait for all backups to complete
		CompletableFuture<Void> allBackups = CompletableFuture.allOf(
			backupFutures.toArray(new CompletableFuture[0])
		);
		
		allBackups.whenComplete((result, throwable) -> {
			logger.info(String.format("[Backup] Completed backup for competition %d: %d successful, %d failed out of %d total",
				compId, successful.get(), failed.get(), plotsToBackup.size()));
			
			resultFuture.set(new BackupResult(plotsToBackup.size(), successful.get(), failed.get(), archiveDir));
		});
	}
	
	private boolean isPlotEmpty(Plot plot) {
        // TODO: Implement a check to see if the plot is empty
		return false;
	}
	
	private CompletableFuture<Boolean> backupPlotAsync(Plot plot, File archiveDir, SchematicHandler schematicHandler) {
		CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
		
		String plotId = plot.getId().toString();
		UUID firstOwner = plot.getOwners().iterator().next();
		
		// Generate base filename
		String baseFilename = plotId + "-" + firstOwner.toString() + ".schem";
		
		// Find unique filename (handle duplicates)
		File targetFile = findUniqueFile(archiveDir, baseFilename);
		
		try {
			// Get CompoundTag from PlotSquared (runs async internally)
			CompletableFuture<CompoundTag> tagFuture = schematicHandler.getCompoundTag(plot);
			
			// Chain save operation after tag is retrieved
			tagFuture.whenComplete((tag, throwable) -> {
				if (throwable != null) {
					logger.log(Level.WARNING, String.format("[Backup] Failed to get schematic data for plot %s (owner: %s): %s",
						plotId, firstOwner, throwable.getMessage()));
					resultFuture.complete(false);
					return;
				}
				
				if (tag == null) {
					logger.log(Level.WARNING, String.format("[Backup] Schematic data is null for plot %s (owner: %s)",
						plotId, firstOwner));
					resultFuture.complete(false);
					return;
				}
				
				// Save schematic (run on async thread)
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					boolean saved = saveSchematicWithRetry(schematicHandler, tag, targetFile, plotId, firstOwner);
					resultFuture.complete(saved);
				});
			});
			
		} catch (Exception e) {
			logger.log(Level.WARNING, String.format("[Backup] Error backing up plot %s (owner: %s): %s",
				plotId, firstOwner, e.getMessage()));
			resultFuture.complete(false);
		}
		
		return resultFuture;
	}
	
	private File findUniqueFile(File directory, String baseFilename) {
		File file = new File(directory, baseFilename);
		
		if (!file.exists()) {
			return file;
		}
		
		// File exists, try with increment
		int index = 1;
		String baseName = baseFilename.substring(0, baseFilename.lastIndexOf('.'));
		String extension = baseFilename.substring(baseFilename.lastIndexOf('.'));
		
		do {
			String newFilename = baseName + "-" + index + extension;
			file = new File(directory, newFilename);
			index++;
		} while (file.exists() && index < 1000); // Safety limit
		
		return file;
	}
	
	private boolean saveSchematicWithRetry(SchematicHandler schematicHandler, CompoundTag tag, File targetFile, String plotId, UUID owner) {
		// First attempt
		if (saveSchematic(schematicHandler, tag, targetFile, plotId, owner)) {
			return true;
		}
		
		// Retry once
		logger.log(Level.WARNING, String.format("[Backup] Retrying save for plot %s (owner: %s)", plotId, owner));
		return saveSchematic(schematicHandler, tag, targetFile, plotId, owner);
	}
	
	private boolean saveSchematic(SchematicHandler schematicHandler, CompoundTag tag, File targetFile, String plotId, UUID owner) {
		try {
			String path = targetFile.getAbsolutePath();
			boolean saved = schematicHandler.save(tag, path);
			
			if (!saved) {
				logger.log(Level.WARNING, String.format("[Backup] SchematicHandler.save() returned false for plot %s (owner: %s)",
					plotId, owner));
				return false;
			}
			
			// Verify file was created and has content
			try {
				if (!targetFile.exists()) {
					logger.log(Level.WARNING, String.format("[Backup] Schematic file was not created for plot %s (owner: %s)",
						plotId, owner));
					return false;
				}
				
				if (targetFile.length() == 0) {
					logger.log(Level.WARNING, String.format("[Backup] Schematic file is empty for plot %s (owner: %s)",
						plotId, owner));
					return false;
				}
			} catch (SecurityException e) {
				logger.log(Level.SEVERE, String.format("[Backup] Permission denied checking file for plot %s (owner: %s): %s",
					plotId, owner, e.getMessage()));
				return false;
			}
			
			return true;
			
		} catch (SecurityException e) {
			logger.log(Level.SEVERE, String.format("[Backup] Permission denied saving plot %s (owner: %s): %s",
				plotId, owner, e.getMessage()));
			return false;
		} catch (Exception e) {
			// Check for disk full errors in exception message
			String errorMsg = e.getMessage();
			if (errorMsg != null && (errorMsg.contains("No space left") || errorMsg.contains("disk full"))) {
				logger.log(Level.SEVERE, String.format("[Backup] Disk full while saving plot %s (owner: %s): %s",
					plotId, owner, e.getMessage()));
			} else {
				logger.log(Level.WARNING, String.format("[Backup] Unexpected error saving plot %s (owner: %s): %s",
					plotId, owner, e.getMessage()));
			}
			return false;
		}
	}
}
