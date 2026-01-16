package au.com.addstar.comp.lobby.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for parsing command flags and arguments.
 * Supports flags in the format: --flag value or --flag=value
 */
public class CommandFlagParser {
	private final Map<String, String> flags;
	private final String[] positionalArgs;
	
	private CommandFlagParser(Map<String, String> flags, String[] positionalArgs) {
		this.flags = flags;
		this.positionalArgs = positionalArgs;
	}
	
	/**
	 * Parses command arguments into flags and positional arguments
	 * @param args The command arguments
	 * @return A CommandFlagParser instance
	 */
	public static CommandFlagParser parse(String[] args) {
		Map<String, String> flags = new HashMap<>();
		java.util.List<String> positional = new java.util.ArrayList<>();
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("--")) {
				String flagName = arg.substring(2);
				String value = null;
				
				// Check for --flag=value format
				int equalsIndex = flagName.indexOf('=');
				if (equalsIndex >= 0) {
					value = flagName.substring(equalsIndex + 1);
					flagName = flagName.substring(0, equalsIndex);
				} else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
					// Next argument is the value
					value = args[i + 1];
					i++; // Skip the value argument
				}
				
				flags.put(flagName.toLowerCase(), value);
			} else {
				positional.add(arg);
			}
		}
		
		return new CommandFlagParser(flags, positional.toArray(new String[0]));
	}
	
	/**
	 * Gets a flag value
	 * @param flagName The flag name (without --)
	 * @return The flag value, or null if not set
	 */
	public String getFlag(String flagName) {
		return flags.get(flagName.toLowerCase());
	}
	
	/**
	 * Checks if a flag is present
	 * @param flagName The flag name (without --)
	 * @return True if the flag is present
	 */
	public boolean hasFlag(String flagName) {
		return flags.containsKey(flagName.toLowerCase());
	}
	
	/**
	 * Gets positional arguments
	 * @return Array of positional arguments
	 */
	public String[] getPositionalArgs() {
		return positionalArgs;
	}
	
	/**
	 * Gets a positional argument by index
	 * @param index The index (0-based)
	 * @return The argument, or null if index is out of bounds
	 */
	public String getPositionalArg(int index) {
		if (index >= 0 && index < positionalArgs.length) {
			return positionalArgs[index];
		}
		return null;
	}
}
