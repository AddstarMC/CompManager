package au.com.addstar.comp.lobby;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.comp.Competition;
import au.com.addstar.comp.util.CompUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.bukkit.Bukkit;

import java.util.Map;

/**
 * Periodically broadcast reminders using BungeeChat
 */
public class BroadcastReminder implements Runnable {
	private final CompManager manager;

	private String bungeeChatBroadcastChannel;

	/**
	 * Map between compId and the last "Enter this comp" broadcast
	 */
	private Map<Integer, Long> lastRunningBroadcast = Maps.newHashMap();

	/**
	 * Map between compId and the last "Vote for this comp" broadcast
	 */
	private Map<Integer, Long> lastVotingBroadcast = Maps.newHashMap();

	/**
	 * Constructor
	 *
	 * @param manager          Manager
	 * @param broadcastChannel Broadcast channel name; null if BungeeChat is not available
	 */
	public BroadcastReminder(
			CompManager manager,
			String broadcastChannel) {

		this.manager = manager;
		this.bungeeChatBroadcastChannel = broadcastChannel;

	}

	/**
	 * Check each comp to see if a reminder needs to be broadcasted
	 */
	@Override
	public void run() {
		for (final CompServer server : manager.getServers()) {

			Competition activeComp = server.getCurrentComp();
			if (activeComp == null) {
				continue;
			}

			int compId = activeComp.getCompId();

			if (activeComp.isRunning()) {
				if (shouldBroadcast(
						compId,
						activeComp.getStartDate(), activeComp.getEndDate(),
						manager.getGlobalBroadcastRunningMin(), manager.getGlobalBroadcastRunningMax(),
						lastRunningBroadcast)) {

					String msg = manager.getMessage("broadcast.running", "theme", activeComp.getTheme());
					broadcastNow(
							compId,
							activeComp.getEndDate(),
							lastRunningBroadcast,
							msg
					);
				}

			} else if (server.getCurrentComp().isVoting()) {
				if (shouldBroadcast(
						compId,
						activeComp.getEndDate(), activeComp.getVoteEndDate(),
						manager.getGlobalBroadcastVotingMin(), manager.getGlobalBroadcastVotingMax(),
						lastVotingBroadcast)) {

					String msg = manager.getMessage("broadcast.voting", "theme", activeComp.getTheme());
					broadcastNow(
							compId,
							activeComp.getVoteEndDate(),
							lastVotingBroadcast,
							msg
					);

				}
			}

		}
	}

	/**
	 * Send a server-wide broadcast
	 *
	 * @param compId
	 * @param endDate
	 * @param lastBroadcastMap
	 * @param broadcastMessage
	 */
	private void broadcastNow(
			Integer compId,
			long endDate,
			Map<Integer, Long> lastBroadcastMap,
			String broadcastMessage) {

		// Add/update the cached date for this comp
		lastBroadcastMap.put(compId, System.currentTimeMillis());

		long timeRemainingMinutes = minutesRemaining(endDate);
		String timeRemaining;

		// Format time remaining as user-friendly text, for example 2d 23h or 3h 57m
		timeRemaining = CompUtils.formatTimeRemaining(timeRemainingMinutes * 60 * 1000);

		// Replace {timeleft} with the time remaining
		String msg = broadcastMessage.replace("{timeleft}", timeRemaining);

		// Broadcast locally
		Bukkit.getServer().broadcastMessage(msg);

		if (!Strings.isNullOrEmpty(bungeeChatBroadcastChannel)) {
			// Also broadcast globally using BungeeChat
			BungeeChat.mirrorChat(msg, bungeeChatBroadcastChannel);
		}

	}

	/**
	 * Compute the time, in milliseconds, between the current time and startDateMillis
	 *
	 * @param startDateMillis Starting date
	 * @return The elapsed time, in minutes
	 */
	private int minutesElapsed(long startDateMillis) {
		if (startDateMillis == 0) {
			return 0;
		}

		double differenceMinutes = timespanMinutes(startDateMillis, System.currentTimeMillis());
		if (differenceMinutes < 0)
			return 0;
		else
			return (int) differenceMinutes;
	}

	/**
	 * Compute the time, in milliseconds, between endDateMillis and the current time
	 *
	 * @param endDateMillis Ending date
	 * @return The elapsed time, in minutes
	 */
	private long minutesRemaining(long endDateMillis) {
		double differenceMinutes = timespanMinutes(System.currentTimeMillis(), endDateMillis);
		if (differenceMinutes < 0)
			return 0;
		else
			return Math.round(differenceMinutes);
	}

	/**
	 * Determine whether a broadcast should be sent out
	 *
	 * @param compId           Competition Id
	 * @param startDate        Start date (from currentTimeMillis) of the activity (e.g. build start date or voting start date)
	 * @param endDate          End date (from currentTimeMillis) of the activity (e.g. build end date or voting end date)
	 * @param intervalMin      Minumum broadcast interval, in minutes
	 * @param intervalMax      Maximum broadcast interval, in minutes
	 * @param lastBroadcastMap Map keeping track of the last broadcast time for each active competition
	 * @return True if a broadcast message should be sent
	 */
	private boolean shouldBroadcast(
			int compId,
			long startDate,
			long endDate,
			int intervalMin,
			int intervalMax,
			Map<Integer, Long> lastBroadcastMap) {

		long timeRemainingMinutes = minutesRemaining(endDate);
		if (timeRemainingMinutes < 5) {
			// Less than 5 minutes remain
			// This is not enough time for people to reasonably accomplish anything
			// Do not broadcast
			return false;
		}

		if (!lastBroadcastMap.containsKey(compId)) {
			// This information has not been broadcast yet
			// Update the map with the current time;
			// wait to send a broadcast until after the current time threshold is reached
			lastBroadcastMap.put(compId, System.currentTimeMillis());
			return false;
		}

		long lastBroadcastDateMilli = lastBroadcastMap.get(compId);
		int differenceMinutes = minutesElapsed(lastBroadcastDateMilli);

		// Compute the broadcast interval threshold
		// The threshold is a value between intervalMin and intervalMax,
		// scaled based on the fraction of time remaining

		double activityLengthMinutes = timespanMinutes(startDate, endDate);
		double fractionRemaining = 0;
		if (activityLengthMinutes > 0) {
			fractionRemaining = timeRemainingMinutes / activityLengthMinutes;
		}

		double broadcastIntervalThreshold = intervalMin + (intervalMax - intervalMin) * fractionRemaining;

		// Need to broadcast the message
// Do not broadcast the message yet
		return differenceMinutes >= broadcastIntervalThreshold;

	}

	/**
	 * Convert the duration between two millisecond dates, then convert to minutes
	 *
	 * @param startDate
	 * @param endDate
	 * @return Duration, in minutes
	 */
	private double timespanMinutes(long startDate, long endDate) {
		return (endDate - startDate) / 1000.0 / 60.0;
	}

}
