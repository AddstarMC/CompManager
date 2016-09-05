package au.com.addstar.comp.lobby;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.comp.Competition;

import au.com.addstar.comp.util.CompUtils;
import au.com.addstar.comp.util.Messages;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically broadcast reminders using BungeeChat
 */
public class BroadcastReminder implements Runnable {
	private final CompManager manager;
	private final Logger logger;

	private final Messages messages;

	private String bungeeChatBroadcastChannel;

	/**
	 * Minimum minutes between broadcasting that a comp is running
	 */
	private int broadcastIntervalRunningMin;

	/**
	 * Maximum minutes between broadcasting that a comp is running
	 */
	private int broadcastIntervalRunningMax;

	/**
	 * Minimum minutes between broadcasting that a comp is in the voting state
	 */
	private int broadcastIntervalVotingMin;

	/**
	 * Maximum minutes between broadcasting that a comp is in the voting state
	 */
	private int broadcastIntervalVotingMax;

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
	 * @param manager
	 */
	public BroadcastReminder(
			CompManager manager,
			String broadcastChannel,
			ConfigurationSection broadcastSettings,
			Logger pluginLogger,
			Messages pluginMessages) {

		this.manager = manager;

		broadcastIntervalRunningMin = stringToInt(broadcastSettings, "global-broadcast-running-min", 15);
		broadcastIntervalRunningMax = stringToInt(broadcastSettings, "global-broadcast-running-max", 120);

		broadcastIntervalVotingMin = stringToInt(broadcastSettings, "global-broadcast-voting-min", 10);
		broadcastIntervalVotingMax = stringToInt(broadcastSettings, "global-broadcast-voting-max", 60);

		logger = pluginLogger;
		logger.log(Level.INFO, "Broadcast interval running: " +
				broadcastIntervalRunningMin + " to " + broadcastIntervalRunningMax + " minutes");

		logger.log(Level.INFO, "Broadcast interval voting: " +
				broadcastIntervalVotingMin + " to " + broadcastIntervalVotingMax + " minutes");

		messages = pluginMessages;

		this.bungeeChatBroadcastChannel = broadcastChannel;
	}

	@Override
	public void run() {
		for (final CompServer server : manager.getServers()) {

			Competition activeComp = server.getCurrentComp();
			if (activeComp == null) {
				continue;
			}

			Integer compId = activeComp.getCompId();

			if (activeComp.isRunning()) {
				if (shouldBroadcast(
						compId,
						activeComp.getStartDate(), activeComp.getEndDate(),
						broadcastIntervalRunningMin, broadcastIntervalRunningMax,
						lastRunningBroadcast)) {

					String msg = messages.get("broadcast.running", "theme", activeComp.getTheme());
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
						broadcastIntervalVotingMin, broadcastIntervalVotingMax,
						lastVotingBroadcast)) {

					String msg = messages.get("broadcast.voting", "theme", activeComp.getTheme());
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
	 * Return the broadcast message prefix, for example [Comp]
	 *
	 * @param compPrefix
	 * @return
	 */
	private String getPrefix(String compPrefix) {
		return ChatColor.GOLD + "[" + ChatColor.DARK_GREEN + "Comp" + ChatColor.GOLD + "]";
	}

	/**
	 * Convert the number of milliseconds between two dates, then convert to minutes
	 *
	 * @param startDate
	 * @param endDate
	 * @return Duration, in minutes
	 */
	private double millisToMinutes(long startDate, long endDate) {
		return (endDate - startDate) / 1000.0 / 60.0;
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

		double differenceMinutes = millisToMinutes(startDateMillis, System.currentTimeMillis());
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
		double differenceMinutes = millisToMinutes(System.currentTimeMillis(), endDateMillis);
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

		double activityLengthMinutes = millisToMinutes(startDate, endDate);
		double fractionRemaining = 0;
		if (activityLengthMinutes > 0) {
			fractionRemaining = timeRemainingMinutes / activityLengthMinutes;
		}

		double broadcastIntervalThreshold = intervalMin + (intervalMax - intervalMin) * fractionRemaining;

		if (differenceMinutes >= broadcastIntervalThreshold) {
			// Need to broadcast the message
			return true;
		} else {
			// Do not broadcast the message yet
			return false;
		}

	}

	/**
	 * Lookup the config value and convert to an integer
	 * @param configSettings    Configuration section
	 * @param keyName           Setting to find
	 * @param defaultValue      Default value if not found or not numeric
	 * @return Configuration value
	 */
	private int stringToInt(ConfigurationSection configSettings, String keyName, int defaultValue) {
		if (configSettings == null) {
			logger.log(Level.WARNING, "Config section is null; cannot get setting for " + keyName);
			return defaultValue;
		}

		String valueText = configSettings.getString(keyName, Integer.toString(defaultValue));
		if (Strings.isNullOrEmpty(valueText))
			return defaultValue;

		try {
			int value = Integer.parseInt(valueText);
			return value;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

}
