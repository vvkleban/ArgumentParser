import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Name:		ArgumentParser
 * Version:		0.1
 * Date:		2013-02-27
 * Author:		Volodymyr Kleban
 * 
 * Copyright (C) S.W.I.F.T. sc. 2013. All rights reserved.
 *
 *
 * This software and its associated documentation contain
 * proprietary, confidential and trade secret information of
 * S.W.I.F.T. sc. and except as provided by written agreement
 * with S.W.I.F.T. sc.
 * a) no part may be disclosed, distributed, reproduced,
 *    transmitted, transcribed, stored in a retrieval system,
 *    adapted or translated in any form or by any means
 *    electronic, mechanical, magnetic, optical, chemical,
 *    manual or otherwise, and
 * b) the recipient is not entitled to discover through reverse
 *    engineering or reverse compiling or other such techniques
 *    or processes the trade secrets contained in the software
 *    code or to use any such trade secrets contained therein or
 *    in the documentation.
 */
public class ArgumentParser {
	
	private static final Pattern OPTION_PATTERN= Pattern.compile("-(\\w)");
	private static final Pattern ARGUMENT_PATTERN= Pattern.compile("([^\\-\\s].*)");
	private static final Pattern OPTION_WITH_ARGUMENT_PATTERN= Pattern.compile("-(\\w)\\s*([^\\-\\s].*)");
	
	/**
	 * Create new argument parser with the optString for argument validation
	 * 
	 * @param optString - A string to specify the way arguments are expected<br>
	 * This parser only handles short (one letter) options, and the following syntax:<br>
	 * [ ] - indicates optional arguments<br>
	 * | - indicates mutually exclusive arguments<br>
	 * { } - delimits a set of mutually exclusive arguments<br> 
	 * EBNF of optString:<br>
	 * optString = options ;<br>
	 * options = (option block)+ ;<br>
	 * option block = option | optional arguments | choice ;<br>
	 * optional arguments = '[', options, ']' ;<br>
	 * choice = '{', options, { '|', options }, '}' ;<br>
	 * option = option without value | option with value ;<br>
	 * option with value = option, ":" ;<br>
	 * option without value= "A" .. "Z" | "a" .. "z" | "0" .. "9" ;<br>
	 * @throws ArgumentException 
	 */
	public ArgumentParser(String optString) throws ArgumentException {
		this.optString_= optString;
		options_= new HashSet<OptionBlock>();
		if (!parseOptions(options_) || index_ < optString_.length())
			throw new ArgumentException("optString is invalid. Please consult EBNF of optString for this module");
	}
	
	/**
	 * Parse arguments into a Map<String, String> and validate them against the optString provided at construction
	 * 
	 * @param args - Tokenized command line arguments i.e. the ones from here: main(<b>String[] args</b>)
	 * @return Parsed arguments. Options with no arguments will map to empty strings
	 * @throws ArgumentException if arguments are wrong or unparseable
	 */
	public Map<String, String> parseArguments(String [] args) throws ArgumentException {
		Map<String, String> result= new HashMap<String, String>();
		int index= 0;
		while (index < args.length) {
			Matcher m;
			if ((m= OPTION_PATTERN.matcher(args[index])).matches()) {
				Matcher m2;
				if (index + 1 < args.length && (m2= ARGUMENT_PATTERN.matcher(args[index + 1])).matches()) {
					if (result.put(m.group(1), m2.group(1)) != null)
						throw new ArgumentException("Option \"" + m.group(1) + "\" was specified more than once");
					index += 2;
				} else {
					if (result.put(m.group(1), "") != null)
						throw new ArgumentException("Option \"" + m.group(1) + "\" was specified more than once");
					index++;
				}
			} else if ((m= OPTION_WITH_ARGUMENT_PATTERN.matcher(args[index])).matches()) {
				if (result.put(m.group(1), m.group(2)) != null)
					throw new ArgumentException("Option \"" + m.group(1) + "\" was specified more than once");
				index++;
			} else {
				throw new ArgumentException("Failed to parse arguments: " + detokenizeArguments(args));
			}
		}
		Map<String, String> copy= new HashMap<String, String>(result);
		for (OptionBlock o : options_) {
			if (!o.matches(copy))
				throw new ArgumentException("Failed to parse arguments: " + detokenizeArguments(args));
		}
		if (!copy.isEmpty())
			throw new ArgumentException("Failed to parse arguments: " + detokenizeArguments(args));
		return result;
	}
	
	/**
	 * De-tokenize arguments and return the string
	 * 
	 * @param args - String[] of command line arguments
	 * @return String with quoted arguments
	 */
	private String detokenizeArguments(String [] args) {
		StringBuilder result= new StringBuilder();
		for (String parameter : args)
			result.append(" \"").append(parameter.replaceAll("(\\\\|\\\")", "\\\\$1")).append("\"");
		if (result.length() < 1)
			return "\"\"";
		return result.toString();
	}

	
	/**
	 * The input for optString parsing
	 */
	private int index_;
	private String optString_;

	/**
	 * The structure for arguments validation
	 * Created by parsing optString
	 */
	private Set<OptionBlock> options_;
	
	/**
	 * Parse:<br> 
	 * option = option without value | option with value ;
	 * 
	 * @param options - options structure to parse optString into
	 * @return true is parsed successfully
	 */
	private boolean parseOption(Set<OptionBlock> options) {
		char c;
		if ((c = peekChar()) != 0 && (( c >= 'A' && c <= 'Z' ) || ( c >= 'a' && c <= 'z' ) || (c >= '0' && c <= '9'))) {
			skipChar();
			char arg;
			if ((arg = peekChar()) != 0 && arg == ':') {
				skipChar();
				options.add(new Option(c, true));
			}
			else
				options.add(new Option(c, false));
			return true;
		}
		return false;
	}
	
	/**
	 * Parse:<br>
	 * optional arguments = '[', options, ']' ;
	 * 
	 * @param options - options structure to parse optString into
	 * @return true is parsed successfully
	 * @throws ArgumentException if starts as an optional block, but is unparseable as such
	 */
	private boolean parseOptionalArguments(Set<OptionBlock> options) throws ArgumentException {
		char c;
		if ((c = peekChar()) != 0 && c =='[') {
			skipChar();
			Set<OptionBlock> optionalSet= new HashSet<OptionBlock>();
			if (parseOptions(optionalSet) && (c = peekChar()) != 0 && c ==']') {
				skipChar();
				options.add(new OptionalArguments(optionalSet));
				return true;
			}
			throw new ArgumentException("Optional arguments have to be closed with ']' before position " + index_);
		}
		return false;
	}

	/**
	 * Parse:<br>
	 * choice = '{', options, { '|', options }, '}' ;
	 * 
	 * @param options - options structure to parse optString into
	 * @return true is parsed successfully
	 * @throws ArgumentException if starts as a choice block, but is unparseable as such
	 */
	private boolean parseChoice(Set<OptionBlock> options) throws ArgumentException {
		char c;
		if ((c = peekChar()) != 0 && c =='{') {
			skipChar();
			Set<Set<OptionBlock>> choice= new HashSet<Set<OptionBlock>>();
			Set<OptionBlock> optionSet= new HashSet<OptionBlock>();
			if (parseOptions(optionSet)) {
				choice.add(optionSet);
				while ((c = peekChar()) != 0 && c =='|') {
					skipChar();
					optionSet= new HashSet<OptionBlock>();
					if (!parseOptions(optionSet))
						throw new ArgumentException("Options must follow the '|' before " + index_);
					choice.add(optionSet);
				}
				if ((c = peekChar()) != 0 && c =='}') {
					skipChar();
					options.add(new Choice(choice));
					return true;
				}
				throw new ArgumentException("Choice has to be closed by '}' before " + index_);
			}
			throw new ArgumentException("Failed to parse any options afrer '{' at " + index_);
		}
		return false;
	}
	
	/**
	 * Parse:<br>
	 * option block = option | optional arguments | choice ;
	 * 
	 * @param options - options structure to parse optString into
	 * @return true is parsed successfully
	 * @throws ArgumentException if starts as a block of options (or an option), but is unparseable as such
	 */
	private boolean parseOptionBlock(Set<OptionBlock> options) throws ArgumentException {
		return (parseOption(options) || parseOptionalArguments(options) || parseChoice(options));
	}

	/**
	 * Parse:<br>
	 * options = (option block)+ ;
	 * 
	 * @param options - options structure to parse optString into
	 * @return true is parsed successfully
	 * @throws ArgumentException if unparseable as optString
	 */
	private boolean parseOptions(Set<OptionBlock> options) throws ArgumentException {
		if (!parseOptionBlock(options))
			throw new ArgumentException("Failed to parse any options at " + index_);
		while(parseOptionBlock(options));
		return true;
	}
	
	private char peekChar() {
		if (index_ < optString_.length())
			return optString_.charAt(index_);
		return 0;
	}
	
	private void skipChar() {
		index_++;
	}
	
	/**
	 * A block of options (or an option) 
	 * 
	 * @author vkleban
	 */
	private interface OptionBlock {
		/**
		 * @param arguments
		 * @return if some part of arguments matches this OptionBlock, <b>remove this part from the map</b> 
		 * and return true. Return false otherwise
		 */
		public boolean matches(Map<String, String> arguments);
	}
	
	/**
	 * Option (with or without argument) container
	 * 
	 * @author vkleban
	 */
	private class Option implements OptionBlock {

		private boolean withArgument;
		private char option;

		public Option(char option, boolean withArgument) {
			this.option= option;
			this.withArgument= withArgument;
		}
		
		@Override
		public boolean matches(Map<String, String> arguments) {
			String argument= arguments.remove(String.valueOf(option));
			if (argument == null)
				return false;
			if (argument.isEmpty() ^ withArgument)
				return true;
			// The option matched, but the argument condition is wrong, so put it back
			arguments.put(String.valueOf(option), argument);
			return false;
		}
	}
	
	/**
	 * Optional block container
	 * 
	 * @author vkleban
	 */
	private class OptionalArguments implements OptionBlock {

		private Set<OptionBlock> options;
		
		public OptionalArguments(Set<OptionBlock> options) {
			this.options= options;
		}
		
		@Override
		public boolean matches(Map<String, String> arguments) {
			Map<String, String> copy= new HashMap<String, String>(arguments);
			// Unconditionally return true, since the arguments are optional
			// The only thing we do here - if the arguments match, the map is modified
			for (OptionBlock o : options)
				if (!o.matches(copy))
					return true; // yes, it does not match, but return true anyway, preserving original arguments
			// If everything matched, modify the arguments
			arguments.clear();
			arguments.putAll(copy);
			return true;
		}
		
	}
	
	/**
	 * Choice block container 
	 * 
	 * @author vkleban
	 */
	private class Choice implements OptionBlock {
		
		private Set<Set<OptionBlock>> choice;
		
		public Choice(Set<Set<OptionBlock>> choice) {
			this.choice= choice;
		}

		@Override
		public boolean matches(Map<String, String> arguments) {
			for (Set<OptionBlock> options : choice) {
				Map<String, String> copy= new HashMap<String, String>(arguments);
				boolean matches= true;
				for (OptionBlock o : options) {
					if (!o.matches(copy)) {
						matches= false;
						break;
					}
				}
				if (matches) {
					arguments.clear();
					arguments.putAll(copy);
					return true;
				}
			}
			return false;
		}
		
	}
}