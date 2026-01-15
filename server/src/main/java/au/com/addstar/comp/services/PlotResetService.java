package au.com.addstar.comp.services;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.util.CompetitionChangeTracker;
import au.com.addstar.comp.util.P2Bridge;
import au.com.addstar.comp.util.ServerTransferUtil;
import au.com.addstar.comp.util.Messages;

/**
 * Service for resetting plots with backup, player transfer, and clearing.
 * Orchestrates the complete reset process: backup → transfer → clearing.
 */
public class PlotResetService {
	
	/**
	 * Callback interface for progress updates during reset operations.
	 */
	public interface ResetProgressCallback {
		/**
		 * Called when a phase of the reset process starts.
		 * @param phase The current phase name (e.g., "backup", "transfer", "clearing")
		 */
		void onPhaseStart(String phase);
	}
	
	private final PlotBackupService backupService;
	private final P2Bridge bridge;
	private final JavaPlugin plugin;
	private final Logger logger;
	private final CompetitionChangeTracker changeTracker;
	private final String lobbyId;
	private final Messages messages;
	
	private volatile boolean isResetting = false;
	private final Object resetLock = new Object();
	
	public PlotResetService(PlotBackupService backupService, P2Bridge bridge, JavaPlugin plugin, 
			Logger logger, CompetitionChangeTracker changeTracker, String lobbyId, Messages messages) {
		this.backupService = backupService;
		this.bridge = bridge;
		this.plugin = plugin;
		this.logger = logger;
		this.changeTracker = changeTracker;
		this.lobbyId = lobbyId;
		this.messages = messages;
	}
	
	/**
	 * Checks if a reset operation is currently in progress.
	 * @return True if a reset is in progress
	 */
	public boolean isResetInProgress() {
		return isResetting;
	}
	
	/**
	 * Resets all plots for the given competition.
	 * Process: backup → transfer players → clear plots → update tracker
	 * @param competition The competition to reset plots for
	 * @param progressCallback Optional callback for progress updates (can be null)
	 * @param onComplete Optional callback to run when reset completes (can be null)
	 * @return A future that completes when the reset is done
	 */
	public ListenableFuture<Void> resetPlots(Competition competition, ResetProgressCallback progressCallback, Runnable onComplete) {
		synchronized (resetLock) {
			if (isResetting) {
				return Futures.immediateFailedFuture(
					new IllegalStateException("Reset already in progress")
				);
			}
			
			// Check if backup is in progress
			if (backupService.isBackupInProgress()) {
				return Futures.immediateFailedFuture(
					new IllegalStateException("Backup is currently in progress. Cannot start reset.")
				);
			}
			
			isResetting = true;
		}
		
		com.google.common.util.concurrent.SettableFuture<Void> resultFuture = com.google.common.util.concurrent.SettableFuture.create();
		
		// Validate competition
		if (competition == null || competition.getCompId() < 0) {
			synchronized (resetLock) {
				isResetting = false;
			}
			return Futures.immediateFailedFuture(
				new IllegalArgumentException("Invalid competition: competition is null or has invalid compId")
			);
		}
		
		logger.info("[Reset] Starting plot reset for competition: " + competition.getTheme() + " (ID: " + competition.getCompId() + ")");
		
		if (progressCallback != null) {
			progressCallback.onPhaseStart("backup");
		}
		
		// Phase 1: Backup plots
		ListenableFuture<PlotBackupService.BackupResult> backupFuture = backupService.backupPlots(
			competition,
			null, // No progress callback for backup during reset
			null  // Handle completion in our flow
		);
		
		Futures.addCallback(backupFuture, new com.google.common.util.concurrent.FutureCallback<PlotBackupService.BackupResult>() {
			@Override
			public void onSuccess(PlotBackupService.BackupResult result) {
				logger.info("[Reset] Backup completed: " + result.getSuccessfulBackups() + " successful, " + result.getFailedBackups() + " failed");
				
				// Phase 2: Transfer players to lobby
				Bukkit.getScheduler().runTask(plugin, () -> {
					if (progressCallback != null) {
						progressCallback.onPhaseStart("transfer");
					}
					
					ServerTransferUtil.sendPlayersToLobby(lobbyId, plugin, messages, () -> {
						// Phase 3: Clear plots
						Bukkit.getScheduler().runTask(plugin, () -> {
							if (progressCallback != null) {
								progressCallback.onPhaseStart("clearing");
							}
							
							try {
								bridge.clearAllPlots(() -> {
									// Phase 4: Update tracker
									try {
										changeTracker.updateTrackedCompetition(competition);
										logger.info("[Reset] Plot reset completed successfully for competition: " + competition.getTheme());
										
										resultFuture.set(null);
										
										if (onComplete != null) {
											onComplete.run();
										}
									} catch (IOException e) {
										logger.log(Level.WARNING, "[Reset] Failed to update competition tracker", e);
										// Still consider reset successful
										resultFuture.set(null);
										
										if (onComplete != null) {
											onComplete.run();
										}
									} catch (Exception e) {
										logger.log(Level.SEVERE, "[Reset] Unexpected error in reset completion callback", e);
										resultFuture.setException(e);
									} finally {
										// Always reset the flag, even if callback fails
										synchronized (resetLock) {
											isResetting = false;
										}
									}
								});
							} catch (Exception e) {
								// Handle case where clearAllPlots() itself throws
								logger.log(Level.SEVERE, "[Reset] Failed to start plot clearing", e);
								synchronized (resetLock) {
									isResetting = false;
								}
								resultFuture.setException(new IllegalStateException("Failed to clear plots: " + e.getMessage(), e));
							}
						});
					});
				});
			}
			
			@Override
			public void onFailure(Throwable t) {
				logger.log(Level.SEVERE, "[Reset] Backup failed, aborting reset", t);
				synchronized (resetLock) {
					isResetting = false;
				}
				resultFuture.setException(new IllegalStateException("Backup failed: " + t.getMessage(), t));
			}
		}, Bukkit.getScheduler().getMainThreadExecutor(plugin));
		
		return resultFuture;
	}
}
