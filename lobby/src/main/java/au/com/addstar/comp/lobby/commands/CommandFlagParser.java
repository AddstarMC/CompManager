package au.com.addstar.comp.lobby.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for parsing command flags and arguments.
 * Supports flags in the format: --flag value or --flag=value
 * Supports quoted strings with spaces for both flag values and positional arguments.
 * Quotes can be single (') or double (") and support escaped quotes within strings.
 */
public class CommandFlagParser {
	private final Map<String, String> flags;
	private final String[] positionalArgs;
	
	private CommandFlagParser(Map<String, String> flags, String[] positionalArgs) {
		this.flags = flags;
		this.positionalArgs = positionalArgs;
	}
	
	/**
	 * Result of parsing a quoted value, containing the parsed value and number of arguments consumed.
	 */
	private static class ParseResult {
		final String value;
		final int consumedArgs;
		
		ParseResult(String value, int consumedArgs) {
			this.value = value;
			this.consumedArgs = consumedArgs;
		}
	}
	
	/**
	 * Parses a quoted value from the arguments array, starting at the given index.
	 * Collects arguments until a closing quote matching the opening quote is found.
	 * Supports both single (') and double (") quotes, and handles escaped quotes.
	 * 
	 * @param args The arguments array
	 * @param startIndex The index to start parsing from
	 * @return A ParseResult containing the parsed value and number of arguments consumed
	 * @throws IllegalArgumentException if quotes are not properly closed
	 */
	private static ParseResult parseQuotedValue(String[] args, int startIndex) {
		if (startIndex >= args.length) {
			throw new IllegalArgumentException("Unexpected end of arguments while parsing quoted value");
		}
		
		String firstArg = args[startIndex];
		if (firstArg.isEmpty()) {
			throw new IllegalArgumentException("Cannot parse empty argument as quoted value");
		}
		
		// Detect quote type (single or double)
		char quoteChar = 0;
		int quoteStartIndex = -1;
		
		// Check if it starts with a quote
		if (firstArg.startsWith("\"")) {
			quoteChar = '"';
			quoteStartIndex = 0;
		} else if (firstArg.startsWith("'")) {
			quoteChar = '\'';
			quoteStartIndex = 0;
		}
		
		// If no quote at start, not a quoted value
		if (quoteStartIndex == -1) {
			return new ParseResult(firstArg, 1);
		}
		
		// Check if the quote is closed in the same argument
		int quoteEndIndex = firstArg.indexOf(quoteChar, quoteStartIndex + 1);
		
		// Handle escaped quotes - if we find \" or \', skip it
		while (quoteEndIndex > 0 && quoteEndIndex < firstArg.length() - 1) {
			char charBefore = firstArg.charAt(quoteEndIndex - 1);
			if (charBefore == '\\') {
				// Escaped quote, continue searching
				quoteEndIndex = firstArg.indexOf(quoteChar, quoteEndIndex + 1);
			} else {
				break;
			}
		}
		
		// If quote is closed in the same argument
		if (quoteEndIndex >= quoteStartIndex + 1) {
			// Extract the value (remove quotes)
			String value = firstArg.substring(quoteStartIndex + 1, quoteEndIndex);
			// Unescape quotes
			value = unescapeQuotes(value, quoteChar);
			return new ParseResult(value, 1);
		}
		
		// Quote spans multiple arguments - collect them
		StringBuilder valueBuilder = new StringBuilder();
		// Add the part after the opening quote
		if (firstArg.length() > quoteStartIndex + 1) {
			valueBuilder.append(firstArg.substring(quoteStartIndex + 1));
		}
		
		int consumedArgs = 1;
		boolean foundClosingQuote = false;
		
		// Continue through subsequent arguments
		for (int i = startIndex + 1; i < args.length; i++) {
			consumedArgs++;
			String arg = args[i];
			
			// Look for closing quote
			int closingQuoteIndex = -1;
			for (int j = 0; j < arg.length(); j++) {
				if (arg.charAt(j) == quoteChar) {
					// Check if it's escaped
					if (j == 0 || arg.charAt(j - 1) != '\\') {
						closingQuoteIndex = j;
						break;
					}
				}
			}
			
			if (closingQuoteIndex >= 0) {
				// Found closing quote
				if (closingQuoteIndex > 0) {
					valueBuilder.append(' ').append(arg.substring(0, closingQuoteIndex));
				}
				foundClosingQuote = true;
				break;
			} else {
				// No closing quote yet, add this argument
				if (valueBuilder.length() > 0) {
					valueBuilder.append(' ');
				}
				valueBuilder.append(arg);
			}
		}
		
		if (!foundClosingQuote) {
			throw new IllegalArgumentException("Unclosed quote: expected closing " + quoteChar + " but reached end of arguments");
		}
		
		// Unescape quotes in the final value
		String finalValue = unescapeQuotes(valueBuilder.toString(), quoteChar);
		return new ParseResult(finalValue, consumedArgs);
	}
	
	/**
	 * Unescapes quotes in a string value.
	 * Converts \" to " and \' to '.
	 * 
	 * @param value The string value to unescape
	 * @param quoteChar The quote character that was used (for context)
	 * @return The unescaped string
	 */
	private static String unescapeQuotes(String value, char quoteChar) {
		// Replace \" with " and \' with '
		String result = value.replace("\\\"", "\"").replace("\\'", "'");
		return result;
	}
	
	/**
	 * Checks if a string starts with a quote character (single or double).
	 * 
	 * @param str The string to check
	 * @return True if the string starts with a quote
	 */
	private static boolean startsWithQuote(String str) {
		return !str.isEmpty() && (str.startsWith("\"") || str.startsWith("'"));
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
					String valuePart = flagName.substring(equalsIndex + 1);
					flagName = flagName.substring(0, equalsIndex);
					
					// Check if value starts with a quote
					if (startsWithQuote(valuePart)) {
						// Parse quoted value - may span multiple arguments
						// Create temporary array starting with the value part, then remaining args
						int remainingArgs = args.length - i;
						String[] tempArgs = new String[remainingArgs];
						tempArgs[0] = valuePart;
						if (remainingArgs > 1) {
							System.arraycopy(args, i + 1, tempArgs, 1, remainingArgs - 1);
						}
						ParseResult result = parseQuotedValue(tempArgs, 0);
						value = result.value;
						i += result.consumedArgs - 1; // -1 because loop will increment
					} else {
						value = valuePart;
					}
				} else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
					// Next argument is the value - check if it's quoted
					if (startsWithQuote(args[i + 1])) {
						ParseResult result = parseQuotedValue(args, i + 1);
						value = result.value;
						i += result.consumedArgs; // Skip all consumed arguments
					} else {
						value = args[i + 1];
						i++; // Skip the value argument
					}
				}
				
				flags.put(flagName.toLowerCase(), value);
			} else {
				// Positional argument - check if it starts with a quote
				if (startsWithQuote(arg)) {
					ParseResult result = parseQuotedValue(args, i);
					positional.add(result.value);
					i += result.consumedArgs - 1; // -1 because loop will increment
				} else {
					positional.add(arg);
				}
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
