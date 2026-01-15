package au.com.addstar.comp.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;

import au.com.addstar.comp.Competition;
import au.com.addstar.comp.CompState;

/**
 * Utility class for tracking competition changes using file-based persistence.
 */
public class CompetitionChangeTracker {
	private final File dataFolder;
	private final File trackingFile;
	
	/**
	 * Creates a new CompetitionChangeTracker.
	 * @param dataFolder The plugin's data folder where the tracking file will be stored
	 */
	public CompetitionChangeTracker(File dataFolder) {
		this.dataFolder = dataFolder;
		this.trackingFile = new File(dataFolder, "last-comp-id.txt");
	}
	
	/**
	 * Gets the last tracked competition ID.
	 * @return The last comp ID, or -1 if no comp has been tracked yet
	 */
	public int getLastCompId() {
		if (!trackingFile.exists()) {
			return -1;
		}
		
		try {
			String content = new String(Files.readAllBytes(trackingFile.toPath())).trim();
			if (content.isEmpty()) {
				return -1;
			}
			return Integer.parseInt(content);
		} catch (IOException | NumberFormatException e) {
			return -1;
		}
	}
	
	/**
	 * Saves the competition ID using atomic file operations.
	 * Uses a temporary file and atomic move to ensure file integrity.
	 * @param compId The competition ID to save
	 * @throws IOException If the file operation fails
	 */
	public void saveLastCompId(int compId) throws IOException {
		// Ensure data folder exists
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		
		// Write to temporary file first
		File tempFile = new File(dataFolder, "last-comp-id.txt.tmp");
		Files.write(tempFile.toPath(), String.valueOf(compId).getBytes());
		
		// Atomically move temp file to final location
		Files.move(tempFile.toPath(), trackingFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}
	
	/**
	 * Checks if the competition has changed compared to the last tracked competition.
	 * 
	 * <p>Null handling: If {@code newComp} is null, this method returns false (no change detected).
	 * </p>
	 * 
	 * <p>First run detection: If no previous competition has been tracked (getLastCompId() returns -1),
	 * and a new competition exists (newCompId >= 0), this is considered a change.</p>
	 * 
	 * @param newComp The new competition to compare (can be null)
	 * @return True if the competition ID has changed, false otherwise (including when newComp is null)
	 */
	public boolean hasCompetitionChanged(Competition newComp) {
		if (newComp == null) {
			return false;
		}
		
		int lastCompId = getLastCompId();
		int newCompId = newComp.getCompId();
		
		// If no previous comp tracked, consider it changed if new comp exists
		if (lastCompId == -1) {
			return newCompId >= 0;
		}
		
		return lastCompId != newCompId;
	}
	
	/**
	 * Determines if plots should be reset based on competition state and existing plots.
	 * @param newComp The new competition
	 * @param hasExistingPlots Whether there are existing plots that need clearing
	 * @param currentState The current state of the competition
	 * @return True if plots should be reset
	 */
	public boolean shouldResetPlots(Competition newComp, boolean hasExistingPlots, CompState currentState) {
		if (newComp == null) {
			return false;
		}
		
		// Only reset if competition has changed
		if (!hasCompetitionChanged(newComp)) {
			Bukkit.getLogger().info("[CompetitionChangeTracker] Competition has not changed, plots will not be reset");
			return false;
		}
		
		// Only reset if there are existing plots to clear
		if (!hasExistingPlots) {
			Bukkit.getLogger().info("[CompetitionChangeTracker] No existing plots to clear, plots will not be reset");
			return false;
		}
		
		// Don't reset if competition is currently in a running state (shouldn't happen, but safety check)
		if (currentState == CompState.Open || currentState == CompState.Voting) {
			Bukkit.getLogger().info("[CompetitionChangeTracker] Competition is currently in a running state, plots will not be reset");
			return false;
		}
		
		Bukkit.getLogger().info("[CompetitionChangeTracker] Plots will be reset");
		return true;
	}
	
	/**
	 * Updates the tracked competition after a successful reset.
	 * @param comp The competition to track
	 * @throws IOException If the file operation fails
	 */
	public void updateTrackedCompetition(Competition comp) throws IOException {
		if (comp != null && comp.getCompId() >= 0) {
			saveLastCompId(comp.getCompId());
		}
	}
}
