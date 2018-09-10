/*
 * unbted - Una's NBT Editor
 * Copyright (C) 2018 Una Thompson (unascribed)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.unascribed.nbted;

import java.io.IOError;
import java.io.IOException;
import java.util.List;

import org.jline.builtins.Less;
import org.jline.builtins.Source.URLSource;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import io.github.steveice10.opennbt.tag.builtin.Tag;

public class CommandProcessor implements Completer, Highlighter {
	
	private static final String HELP;
	static {
		try {
			HELP = Resources.toString(ClassLoader.getSystemResource("commands-help.txt"), Charsets.UTF_8);
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
	private static final Joiner SLASH_JOINER = Joiner.on('/');
	
	private final Tag root;
	private Tag cursor;
	
	private List<String> pathParts = Lists.newArrayList();
	
	private Terminal terminal;
	private DefaultHistory history = new DefaultHistory();
	
	private List<Command> commands = Lists.newArrayList(
			new Command((args) -> {
				Less less = new Less(terminal);
				less.run(new URLSource(ClassLoader.getSystemResource("warranty.txt"), "warranty.txt"));
			}, "warranty"),
			new Command((args) -> {
				Less less = new Less(terminal);
				less.run(new URLSource(ClassLoader.getSystemResource("license.txt"), "license.txt"));
			}, "copying"),
			new Command((args) -> {
				System.err.println(HELP);
			}, "?", "help")
		);
	
	public CommandProcessor(Tag root) {
		this.root = root;
		this.cursor = root;
	}
	
	public void run() throws Exception {
		terminal = TerminalBuilder.terminal();
		LineReader r = LineReaderBuilder.builder()
				.appName("unbted")
				.completer(this)
				.history(history)
				.terminal(terminal)
				.highlighter(this)
				.variable(LineReader.HISTORY_FILE, System.getProperty("user.home")+"/.unbted_history")
				.build();
		while (true) {
			try {
				System.err.println();
				String prompt;
				if (root == null) {
					prompt = "(new buffer)";
				} else {
					prompt = Strings.nullToEmpty(root.getName())+"/"+SLASH_JOINER.join(pathParts);
				}
				r.readLine(prompt+"\n> ");
				ParsedLine parsed = r.getParsedLine();
				List<String> words = parsed.words();
				if (!words.isEmpty()) {
					String command = words.get(0);
					if (!Strings.isNullOrEmpty(command)) {
						boolean found = false;
						for (Command c : commands) {
							if (c.names.contains(command)) {
								c.exec.exec(words.subList(1, words.size()));
								found = true;
								break;
							}
						}
						if (!found) {
							System.err.println("Unknown command");
						}
					}
				}
			} catch (UserInterruptException e) {
				// ignore
			} catch (EndOfFileException e) {
				dispose();
				break;
			}
		}
	}
	
	public void dispose() throws Exception {
		terminal.close();
		history.save();
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		if (line.wordIndex() == 0) {
			for (Command c : commands) {
				for (String s : c.names) {
					candidates.add(new Candidate(s));
				}
			}
		}
	}

	@Override
	public AttributedString highlight(LineReader reader, String buffer) {
		AttributedStringBuilder asb = new AttributedStringBuilder(buffer.length());
		String command;
		if (buffer.contains(" ")) {
			command = buffer.substring(0, buffer.indexOf(' '));
		} else {
			command = buffer;
		}
		boolean valid = false;
		for (Command c : commands) {
			if (c.names.contains(command)) {
				valid = true;
				break;
			}
		}
		AttributedStyle style = new AttributedStyle();
		if (valid) {
			style = style.foreground(AttributedStyle.BLUE).bold();
		} else {
			style = style.foreground(AttributedStyle.RED).bold();
		}
		asb.append(command, style);
		asb.append(buffer.substring(command.length()));
		return asb.toAttributedString();
	}

}
