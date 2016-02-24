package au.com.addstar.comp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class Messages {
	private Properties properties;
	
	private final InputStream defaultStream;
	private final File file;
	
	public Messages(File file, InputStream defaultStream) {
		this.file = file;
		this.defaultStream = defaultStream;
		
		properties = new Properties();
	}
	
	/**
	 * Reloads the messages from the filesystem
	 * @throws IOException Thrown if an error occurs while loading the messages
	 */
	public void reload() throws IOException {
		Properties defaultLang = new Properties();
		defaultLang.load(defaultStream);
		
		properties = new Properties(defaultLang);
		
		if (file.exists()) {
			FileInputStream input = new FileInputStream(file);
			try {
				properties.load(input);
			} finally {
				input.close();
			}
		}
	}
	
	private String colourize(String input) {
		return ChatColor.translateAlternateColorCodes('&', input);
	}
	
	/**
	 * Gets a message
	 * @param id The id in the message file
	 * @return The formatted string
	 */
	public String get(String id) {
		String value = properties.getProperty(id);
		if (value == null) {
			return "{" + id + "}";
		} else {
			return colourize(value);
		}
	}
	
	private static Pattern tokenPattern = Pattern.compile("\\{([^\\}]*?)\\}");
	
	/**
	 * Gets a message and replaces arguments
	 * @param id The id in the message file
	 * @param arguments An array of String,Object pairs. This array must have an even length
	 * @return The formatted string
	 */
	public String get(String id, Object... arguments) {
		if (arguments.length == 0) {
			return get(id);
		}
		
		Preconditions.checkArgument(arguments.length % 2 == 0, "Arguments must be given in key value pairs");
		
		Map<String, Object> map = Maps.newHashMap();
		for (int i = 0; i < arguments.length-1; i += 2) {
			if (arguments[i] instanceof String) {
				map.put((String)arguments[i], arguments[i+1]);
			} else {
				throw new IllegalArgumentException("Invalid key in arguments at index " + i + ": Not a string. " + Arrays.toString(arguments));
			}
		}
		
		return get(id, map);
	}
	
	/**
	 * Gets a message and replaces arguments
	 * @param id The id in the message file
	 * @param arguments The arguments available to replace
	 * @return The formatted string
	 */
	public String get(String id, Map<String, Object> arguments) {
		String value = properties.getProperty(id);
		if (value == null) {
			return "{" + id + "}";
		} else {
			value = colourize(value);
			
			Matcher matcher = tokenPattern.matcher(value);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				String token = matcher.group(1).toLowerCase();
				
				Object replacement = arguments.get(token);
				if (replacement != null) {
					matcher.appendReplacement(buffer, String.valueOf(replacement));
				} else {
					matcher.appendReplacement(buffer, matcher.group(0));
				}
			}
			
			matcher.appendTail(buffer);
			
			return buffer.toString();
		}
	}
}
