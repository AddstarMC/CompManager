package au.com.addstar.comp.util;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

public final class CompUtils {
	private CompUtils() {}
	
	public static long parseDateDiff(String input) throws IllegalArgumentException {
		return parseDateDiff(input, false);
	}
	
	public static long parseSignedDateDiff(String input) throws IllegalArgumentException {
		return parseDateDiff(input, true);
	}
	
	private static final Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$", Pattern.CASE_INSENSITIVE);
	
	private static long parseDateDiff(String input, boolean allowNegative) throws IllegalArgumentException {
		Matcher m = dateDiffPattern.matcher(input);
		
		if (m.matches()) {
			boolean negative;
			long time = 0;
			
			if (m.group(1) != null) {
				Preconditions.checkArgument(allowNegative, "Invalid format");
				negative = m.group(1).equals("-");
			} else {
				negative = false;
			}

			if (m.group(2) != null) {
				time += TimeUnit.DAYS.toMillis(Integer.parseInt(m.group(2)) * 7);
			}
			
			if (m.group(3) != null) {
				time += TimeUnit.DAYS.toMillis(Integer.parseInt(m.group(3)));
			}
			
			if (m.group(4) != null) {
				time += TimeUnit.HOURS.toMillis(Integer.parseInt(m.group(4)));
			}
			
			if (m.group(5) != null) {
				time += TimeUnit.MINUTES.toMillis(Integer.parseInt(m.group(5)));
			}
			
			if (m.group(6) != null) {
				time += TimeUnit.SECONDS.toMillis(Integer.parseInt(m.group(6)));
			}
			
			if (negative) {
				time *= -1;
			}
			
			return time;
		}
		
		throw new IllegalArgumentException("Invalid format");
	}
	
	private static final SimpleDateFormat endFormat = new SimpleDateFormat("d MMM h:ma");
	
	/**
	 * Formats a time in ms as 'd MMM h:ma'
	 * @param date The unix time in ms
	 * @return The formatted string
	 */
	public static String formatDate(long date) {
		return endFormat.format(date);
	}
	
	/**
	 * Formats the time remaining until a target time as a human-readable string.
	 * Formats as: "Xd" for days, "Xh Ym" for hours, "Xm Ys" for minutes, or "Xs" for seconds.
	 * Only shows the largest two units.
	 * @param remaining The number of ms remaining (can be negative, will return "0s")
	 * @return Formatted time string (e.g., "2d 5h", "17h 13m", "5m 20s", "23s") or "0s" if time has passed
	 */
	public static String formatTimeRemaining(long remaining) {
		if (remaining <= 0) {
			return "0s";
		}
		
		long seconds = remaining / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;
		
		if (days >= 1) {
			long remainingHours = hours % 24;
			if (remainingHours > 0) {
				return days + "d " + remainingHours + "h";
			} else {
				return days + "d";
			}
		} else if (hours >= 1) {
			long remainingMinutes = minutes % 60;
			if (remainingMinutes > 0) {
				return hours + "h " + remainingMinutes + "m";
			} else {
				return hours + "h";
			}
		} else if (minutes >= 1) {
			long remainingSeconds = seconds % 60;
			if (remainingSeconds > 0) {
				return minutes + "m " + remainingSeconds + "s";
			} else {
				return minutes + "m";
			}
		} else {
			return seconds + "s";
		}
	}
}
