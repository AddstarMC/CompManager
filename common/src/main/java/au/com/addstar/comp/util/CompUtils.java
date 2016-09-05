package au.com.addstar.comp.util;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DurationFormatUtils;

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
	private static final String TIME_LEFT_FORMAT_LONG = "d'd 'H'h'";
	private static final String TIME_LEFT_FORMAT_SHORT = "H'h 'm'm'";
	
	/**
	 * Formats a time in ms as 'd MMM h:ma'
	 * @param date The unix time in ms
	 * @return The formatted string
	 */
	public static String formatDate(long date) {
		return endFormat.format(date);
	}
	
	/**
	 * Formats the time remaining like either '2d 3h' or '1h 10m'
	 * @param remaining The number of ms remaining
	 * @return The formatted string
	 */
	public static String formatTimeRemaining(long remaining) {
		if (Math.abs(remaining) < TimeUnit.DAYS.toMillis(1)) {
			return DurationFormatUtils.formatDuration(remaining, TIME_LEFT_FORMAT_SHORT);
		} else {
			return DurationFormatUtils.formatDuration(remaining, TIME_LEFT_FORMAT_LONG);
		}
	}
}
