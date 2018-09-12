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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.unascribed.nbted.TagPrinter.RecurseMode;

import io.github.steveice10.opennbt.tag.builtin.CompoundTag;
import io.github.steveice10.opennbt.tag.builtin.Tag;

public class CommandProcessor implements Completer, Highlighter {
	
	private static final Joiner SLASH_JOINER = Joiner.on('/');
	private static final Splitter SLASH_SPLITTER = Splitter.on('/');
	
	private final Tag root;
	private Tag cursor;
	
	private final TagPrinter printer;
	private final DefaultHistory history = new DefaultHistory();
	private final Map<String, Command> commands = Maps.newHashMap();
	
	public CommandProcessor(Tag root, TagPrinter printer) {
		this.root = root;
		this.cursor = root;
		this.printer = printer;
		Completer dirCompleter = (reader, line, candidates) -> {
			if (line.wordIndex() == 1) {
				if (cursor instanceof CompoundTag) {
					String word = line.word();
					int cur = line.wordCursor();
					Tag tag = cursor;
					String left = word.substring(0, cur);
					String prefix = "";
					if (left.contains("/")) {
						List<String> split = SLASH_SPLITTER.splitToList(left);
						for (int i = 0; i < split.size()-1; i++) {
							String s = split.get(i);
							if (tag instanceof CompoundTag && ((CompoundTag)tag).contains(s)) {
								prefix += s+"/";
								tag = ((CompoundTag)tag).get(s);
							} else {
								return;
							}
						}
						left = split.get(split.size()-1);
					}
					String right = word.substring(cur, word.length());
					Pattern search = Pattern.compile(Pattern.quote(left)+".*"+Pattern.quote(right), Pattern.CASE_INSENSITIVE);
					if (tag instanceof CompoundTag) {
						for (Tag t : ((CompoundTag)tag).values()) {
							if (t instanceof CompoundTag && search.matcher(t.getName()).matches()) {
								String suff = ((CompoundTag)t).isEmpty() ? "" : "/";
								candidates.add(new Candidate(prefix+t.getName()+suff, t.getName()+suff, null, null, suff, null, ((CompoundTag)t).isEmpty()));
							}
						}
					}
				}
			}
		};
		addCommand(Command.create()
			.name("warranty")
			.description("print GPLv3 warranty sections")
			.action((set, args) -> {
				if (args.size() > 0) throw new CommandException("Too many arguments");
				NBTEd.displayEmbeddedFileInPager("warranty.txt");
			}));
		addCommand(Command.create()
			.name("copying")
			.description("print GPLv3 text")
			.action((set, args) -> {
				if (args.size() > 0) throw new CommandException("Too many arguments");
				NBTEd.displayEmbeddedFileInPager("license.txt");
			}));
		addCommand(Command.create()
			.name("help").aliases("?", "h")
			.description("print help")
			.action((set, args) -> {
				if (args.size() > 0) throw new CommandException("Too many arguments");
				NBTEd.displayEmbeddedFileInPager("commands-help.txt");
			}));
		addCommand(Command.create()
			.name("cd")
			.description("change command context")
			.completer(dirCompleter)
			.action((set, args) -> {
				if (args.size() > 1) throw new CommandException("Too many arguments");
				if (args.isEmpty()) {
					cursor = root;
				} else {
					cursor = resolvePath(args.get(0), true);
				}
			}));
		addCommand(Command.create()
			.name("ls").aliases("dir")
			.description("change command context")
			.completer(dirCompleter)
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("R", "recursive"));
				parser.acceptsAll(Arrays.asList("d", "directory"));
				parser.acceptsAll(Arrays.asList("r", "raw"));
				parser.acceptsAll(Arrays.asList("l", "1", "a", "A"));
			})
			.action((set, args) -> {
				boolean infer = NBTEd.INFER;
				if (set.has("raw")) {
					infer = false;
				}
				if (args.size() > 1) throw new CommandException("Too many arguments");
				Tag tag = args.isEmpty() ? cursor : resolvePath(args.get(0), true);
				if (set.has("directory")) {
					printer.printTag(tag, "", infer, RecurseMode.NONE);
				} else {
					printer.printTag(tag, "", infer, set.has("recursive") ? RecurseMode.FULL : RecurseMode.IMMEDIATE_CHILDREN_ONLY);
				}
			}));
	}
	
	private void addCommand(Command command) {
		for (String s : command.getAllNames()) {
			if (commands.containsKey(s)) throw new IllegalArgumentException("Duplicate command name "+s);
			commands.put(s, command);
		}
	}

	private Tag resolvePath(String string, boolean error) throws CommandException {
		Tag cursorWork = cursor;
		List<String> parts = Lists.newArrayList(SLASH_SPLITTER.split(string));
		for (int i = 0; i <= parts.size(); i++) {
			String s = i == parts.size() ? null : parts.get(i);
			if (cursorWork instanceof CompoundTag) {
				if (s != null) {
					if (s.equals("..")) {
						cursorWork = cursorWork.getParent();
					} else if (s.equals(".") || s.isEmpty()) {
						continue;
					} else {
						CompoundTag c = (CompoundTag)cursorWork;
						if (c.contains(s)) {
							cursorWork = c.get(s);
						} else {
							if (error) {
								throw new CommandException("No such element with key "+s);
							} else {
								return null;
							}
						}
					}
				}
			} else if (cursorWork == null) {
				if (error) {
					throw new CommandException("No such element with key "+s);
				} else {
					return null;
				}
			} else {
				if (error) {
					throw new CommandException("Cannot cd into non-compound");
				} else {
					return null;
				}
			}
		}
		return cursorWork;
	}

	public void run() throws Exception {
		LineReader r = LineReaderBuilder.builder()
				.appName("unbted")
				.completer(this)
				.history(history)
				.terminal(NBTEd.terminal)
				.highlighter(this)
				.variable(LineReader.HISTORY_FILE, System.getProperty("user.home")+"/.unbted_history")
				.build();
		while (true) {
			try {
				System.err.println();
				r.readLine("["+getPath()+"]> ");
				ParsedLine parsed = r.getParsedLine();
				List<String> words = parsed.words();
				if (!words.isEmpty()) {
					String commandStr = words.get(0);
					if (!Strings.isNullOrEmpty(commandStr)) {
						if (Strings.isNullOrEmpty(words.get(words.size()-1))) {
							words = words.subList(0, words.size()-1);
						}
						Command command = commands.get(commandStr);
						if (command != null) {
							try {
								command.execute(words.subList(1, words.size()));
							} catch (CommandException e) {
								System.err.println(e.getMessage());
							}
						} else {
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
	
	private String getPath() {
		List<String> parts = Lists.newArrayList();
		Tag t = cursor;
		while (t != null) {
			parts.add(t.getName());
			t = t.getParent();
		}
		if (parts.isEmpty()) {
			return "(new file)";
		} else {
			return SLASH_JOINER.join(Lists.reverse(parts));
		}
	}
	
	public void dispose() throws Exception {
		history.save();
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		if (line.wordIndex() == 0) {
			for (Command c : commands.values()) {
				for (String s : c.getAllNames()) {
					candidates.add(new Candidate(s, s, null, c.getDescription(), null, c.getName(), true));
				}
			}
		} else if (line.words().size() > 1) {
			List<String> words = line.words();
			String commandStr = words.get(0);
			Command command = commands.get(commandStr);
			if (command != null) {
				command.complete(reader, line, candidates);
			}
		}
	}

	private static final Pattern HIGHLIGHTED = Pattern.compile("([^\\\\]|^)([\"'](?:.*?[^\\\\]|)([\\\"']|$)|\\\\.)");
	
	@Override
	public AttributedString highlight(LineReader reader, String buffer) {
		AttributedStringBuilder asb = new AttributedStringBuilder(buffer.length());
		ParsedLine parsed = reader.getParser().parse(buffer, buffer.length(), ParseContext.COMPLETE);
		String command;
		if (!parsed.words().isEmpty()) {
			command = parsed.words().get(0);
		} else {
			command = buffer;
		}
		boolean valid = commands.containsKey(command);
		AttributedStyle commandStyle = new AttributedStyle();
		if (valid) {
			commandStyle = commandStyle.foreground(AttributedStyle.BLUE);
		} else {
			commandStyle = commandStyle.foreground(AttributedStyle.RED).bold();
		}
		asb.append(command, commandStyle);
		AttributedStyle basicStyle = new AttributedStyle().foreground(AttributedStyle.BLUE).bold();
		Matcher matcher = HIGHLIGHTED.matcher(buffer.substring(command.length()));
		StringBuffer scratch = new StringBuffer();
		while (matcher.find()) {
			AttributedStyle stringStyle = new AttributedStyle();
			if (matcher.group(3) == null) {
				stringStyle = stringStyle.foreground(AttributedStyle.CYAN);
			} else if (matcher.group(3).isEmpty()) {
				stringStyle = stringStyle.foreground(AttributedStyle.RED).bold();
			} else {
				stringStyle = stringStyle.foreground(AttributedStyle.YELLOW);
			}
			scratch.setLength(0);
			matcher.appendReplacement(scratch, "$1\0$2\0");
			String head = scratch.substring(0, scratch.indexOf("\0"));
			String body = scratch.substring(scratch.indexOf("\0")+1, scratch.lastIndexOf("\0"));
			String tail = scratch.substring(scratch.lastIndexOf("\0")+1);
			asb.append(head, basicStyle);
			asb.append(body, stringStyle);
			asb.append(tail, basicStyle);
		}
		scratch.setLength(0);
		matcher.appendTail(scratch);
		asb.append(scratch, basicStyle);
		return asb.toAttributedString();
	}

}
