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

import java.io.Closeable;
import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
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
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;
import com.unascribed.miniansi.AnsiCode;
import com.unascribed.nbted.TagPrinter.RecurseMode;

import io.github.steveice10.opennbt.NBTIO;
import io.github.steveice10.opennbt.tag.builtin.CompoundTag;
import io.github.steveice10.opennbt.tag.builtin.Tag;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;

public class CommandProcessor implements Completer, Highlighter {
	
	private static final NumberFormat ONE_FRAC_FMT = new DecimalFormat("#,##0.0");
	private static final NumberFormat TWO_FRAC_FMT = new DecimalFormat("#,##0.00");
	
	private static final Joiner SLASH_JOINER = Joiner.on('/');
	private static final Splitter SLASH_SPLITTER = Splitter.on('/');
	
	private static final Joiner SPACE_JOINER = Joiner.on(' ');
	private static final Joiner NONE_JOINER = Joiner.on("");
	
	private static final Pattern ECHO_ESCAPE = Pattern.compile("(?:\\\\0([0-7]{1,3})|\\\\x([0-9a-fA-F]{1,2})|\\\\u([0-9a-fA-F]{1,4})|\\\\U([0-9a-fA-F]{1,8}))");
	
	private boolean running = false;
	
	private FileInfo fileInfo;
	
	private LineReader reader;
	
	private Tag root;
	private Tag cursor;
	
	private final TagPrinter printer;
	private final DefaultHistory history = new DefaultHistory();
	private final Map<String, Command> commands = Maps.newHashMap();
	
	private boolean dirty = false;
	
	public CommandProcessor(Tag _root, TagPrinter _printer, FileInfo _fileInfo) {
		this.root = _root;
		this.cursor = root;
		this.printer = _printer;
		this.fileInfo = _fileInfo;
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
			.name("rem").aliases("comment", "remark", "#", "//")
			.description("do nothing"));
		addCommand(Command.create()
			.name("echo")
			.description("print arguments")
			.options((parser) -> {
				parser.accepts("e", "parse escapes");
				parser.accepts("n", "don't emit a newline");
				parser.accepts("s", "don't separate arguments with spaces");
			})
			.action((set, args) -> {
				String str = (set.has("s") ? NONE_JOINER : SPACE_JOINER).join(args);
				if (set.has("e")) {
					str = str
							.replace("\\\\", "\\\0\\\0")
							.replace("\\a", "\u0007")
							.replace("\\b", "\b")
							.replace("\\e", "\u001B")
							.replace("\\f", "\f")
							.replace("\\n", "\n")
							.replace("\\r", "\r")
							.replace("\\t", "\t")
							.replace("\\v", "\u000B");
					if (str.contains("\\c")) {
						str = str.substring(0, str.indexOf("\\c"));
					}
					Matcher matcher = ECHO_ESCAPE.matcher(str);
					StringBuffer buf = new StringBuffer();
					while (matcher.find()) {
						int codepoint;
						switch (matcher.group(0).charAt(1)) {
							case '0':
								codepoint = Integer.parseInt(matcher.group(1), 8);
								break;
							case 'x':
								codepoint = Integer.parseInt(matcher.group(2), 16);
								break;
							case 'u':
								codepoint = Integer.parseInt(matcher.group(3), 16);
								break;
							case 'U':
								codepoint = Integer.parseInt(matcher.group(4), 16);
								break;
							default: throw new AssertionError("Unrecognized escape "+matcher.group(0));
						}
						matcher.appendReplacement(buf, Matcher.quoteReplacement(new String(Character.toChars(codepoint))));
					}
					matcher.appendTail(buf);
					str = buf.toString();
					str = str
							.replace("\\0", "\0")
							.replace("\\\0\\\0", "\\");
				}
				System.out.print(str);
				if (!set.has("n")) {
					System.out.println();
				}
			}));
		addCommand(Command.create()
			.name("cd")
			.description("change command context")
			.completer(pathCompleter(true))
			.action((set, args) -> {
				if (args.size() > 1) throw new CommandException("Too many arguments");
				if (args.isEmpty()) {
					cursor = root;
				} else {
					cursor = resolvePath(args.get(0), true, true);
				}
			}));
		addCommand(Command.create()
			.name("ls").aliases("dir")
			.description("list compound contents")
			.completer(pathCompleter(false))
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("R", "recursive"), "list all descendants recursively");
				parser.acceptsAll(Arrays.asList("d", "directory"), "print just the compound, no children");
				parser.acceptsAll(Arrays.asList("r", "raw"), "do not infer types");
				parser.acceptsAll(Arrays.asList("l", "long"), "print values");
				parser.acceptsAll(Arrays.asList("1", "a", "A"), "ignored");
			})
			.action((set, args) -> {
				boolean infer = NBTEd.INFER;
				if (set.has("raw")) {
					infer = false;
				}
				if (args.size() > 1) throw new CommandException("Too many arguments");
				Tag tag = args.isEmpty() ? cursor : resolvePath(args.get(0), true, false);
				if (set.has("directory")) {
					printer.printTag(tag, "", infer, RecurseMode.NONE, set.has("l"));
				} else {
					printer.printTag(tag, "", infer, set.has("recursive") ? RecurseMode.FULL : RecurseMode.IMMEDIATE_CHILDREN_ONLY, set.has("l"));
				}
			}));
		addCommand(Command.create()
			.name("rm").aliases("del", "rmdir", "rd")
			.description("delete a tag")
			.completer(pathCompleter(false))
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("r", "recursive"), "delete non-empty compounds too");
				parser.acceptsAll(Arrays.asList("f", "force"), "silence errors and keep going");
			})
			.action((set, args) -> {
				if (args.isEmpty()) throw new CommandException("Missing argument");
				// REVERSE ORDER - cursor is at index 0, root is at (size()-1)
				List<Tag> contextParents = Lists.newArrayList();
				{
					Tag t = cursor;
					while (t != null) {
						contextParents.add(t);
						t = t.getParent();
					}
				}
				for (String s : args) {
					try {
						Tag t = resolvePath(s, set.has("force"), false);
						if (t == null) {
							throw new CommandException("No such tag with path "+s);
						}
						if (t instanceof CompoundTag) {
							CompoundTag ct = (CompoundTag)t;
							if (!ct.isEmpty() && !set.has("recursive")) {
								throw new CommandException("Refusing to delete non-empty compound "+getPath(t)+" - add -r to override");
							}
						}
						Tag parent = t.getParent();
						if (parent == null) {
							if (t != root) {
								throw new ConsistencyError("Tag has no parent but isn't the root!?");
							}
							root = null;
							cursor = null;
							dirty = true;
							// no action can possibly be more destructive than deleting the root, so break
							break;
						} else {
							if (parent instanceof CompoundTag) {
								CompoundTag ct = (CompoundTag)parent;
								if (ct.contains(t.getName())) {
									ct.remove(t.getName());
									dirty = true;
								} else {
									throw new ConsistencyError("Tried to delete tag from parent, but it's not in its parent!?");
								}
							} else {
								throw new CommandException("Tried to delete tag with non-compound parent (NYI)");
							}
						}
						int idx = contextParents.indexOf(t);
						if (idx != -1) {
							NBTEd.log("Deleted parent of current context, walking up to closest non-deleted path");
							contextParents.subList(0, idx+1).clear();
							if (contextParents.isEmpty()) {
								cursor = null;
							} else {
								cursor = contextParents.get(0);
							}
						}
					} catch (Exception e) {
						if (!set.has("force")) {
							throw e;
						}
					}
				}
			}));
		addCommand(Command.create()
			.name("exit").aliases("abort")
			.description("exit without saving")
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("f", "force"), "don't confirm with unsaved changes");
			})
			.action((alias, set, args) -> {
				if (!args.isEmpty()) throw new CommandException("Too many arguments");
				if (dirty && !alias.equals("abort") && !set.has("force")) {
					try {
						boolean cont = true;
						reader.setOpt(Option.DISABLE_HIGHLIGHTER);
						reader.unsetOpt(Option.ERASE_LINE_ON_FINISH);
						while (cont) {
							String line = reader.readLine("There are unsaved changes. Are you sure you want to exit? [y/N] ");
							if (line.trim().isEmpty()) line = "n";
							switch (line.charAt(0)) {
								case 'n':
								case 'N':
									return;
								case 'y':
								case 'Y':
									cont = false;
									break;
								default:
									System.err.println("Unrecognized choice "+line+". Enter Y or N.");
									break;
							}
						}
					} catch (UserInterruptException | EndOfFileException e) {
						return;
					} finally {
						reader.unsetOpt(Option.DISABLE_HIGHLIGHTER);
						reader.setOpt(Option.ERASE_LINE_ON_FINISH);
					}
				}
				stop();
			}));
		addCommand(Command.create()
			.name("info")
			.description("print information about the loaded file")
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("si", "s"), "use si units instead of binary");
			})
			.action((set, args) -> {
				if (!args.isEmpty()) throw new CommandException("Too many arguments");
				System.out.print("Root tag name: ");
				System.out.println(root == null ? "(no root tag)" : Strings.isNullOrEmpty(root.getName()) ? "(none)" : root.getName());
				System.out.print("Loaded from: ");
				String str;
				if (fileInfo.sourceFile == FileInfo.STDIN)  {
					str = "(stdin)";
				} else if (fileInfo.sourceFile == null) {
					str = "(nowhere)";
				} else {
					str = fileInfo.sourceFile.getAbsolutePath();
				}
				System.out.println(str);
				System.out.print("Compression method: ");
				System.out.print(fileInfo.compressionMethod);
				if (fileInfo.compressionAutodetected) {
					System.out.print(" (detected)");
				}
				System.out.println();
				System.out.print("Endianness: ");
				System.out.println(fileInfo.endianness);
				System.out.print("File size: ...calculating...");
				OutputStream out = ByteStreams.nullOutputStream();
				CountingOutputStream compressedCounter = null;
				if (fileInfo.compressionMethod != Compression.NONE) {
					out = fileInfo.compressionMethod.wrap(compressedCounter = new CountingOutputStream(out));
				}
				CountingOutputStream counter = new CountingOutputStream(out);
				if (root != null) {
					NBTIO.writeTag(fileInfo.endianness.wrap(counter), root);
				}
				System.out.print("\r                            ");
				System.out.print("\rFile size: ");
				System.out.print(humanReadableBytes(counter.getCount(), set.has("si")));
				if (compressedCounter != null) {
					System.out.print(" (");
					System.out.print(humanReadableBytes(compressedCounter.getCount(), set.has("si")));
					double compare = compressedCounter.getCount()/(double)counter.getCount();
					System.out.print(" compressed, ");
					System.out.print(TWO_FRAC_FMT.format(compare*100));
					System.out.print("%)");
				}
				System.out.println();
			}));
		addCommand(Command.create()
			.name("save")
			.description("write the nbt file to disk")
			.completer(new Completers.FileNameCompleter())
			.options((parser) -> {
				parser.accepts("endian", "write in the given endianness").withRequiredArg().ofType(Endianness.class)
						.withValuesConvertedBy(new CaseInsensitiveEnumConverter<>(Endianness.class));
				parser.mutuallyExclusive(
					parser.accepts("little-endian", "write in little-endian format").availableUnless("endian"),
					parser.accepts("big-endian", "write in big-endian format").availableUnless("endian")
				);
				parser.acceptsAll(Arrays.asList("compression", "c"), "write with the given compression format").withRequiredArg().ofType(Compression.class)
						.withValuesConvertedBy(new CaseInsensitiveEnumConverter<>(Compression.class));
				parser.acceptsAll(Arrays.asList("default", "d"), "update default file");
			})
			.action((set, args) -> {
				if (args.size() > 1) throw new CommandException("Too many arguments");
				if (root == null) {
					throw new CommandException("Nothing to write");
				}
				Endianness endianness;
				if (set.has("endian")) {
					endianness = (Endianness)set.valueOf("endian");
				} else if (set.has("little-endian")) {
					endianness = Endianness.LITTLE;
				} else if (set.has("big-endian")) {
					endianness = Endianness.BIG;
				} else {
					endianness = fileInfo.endianness;
				}
				Compression compression;
				boolean compressionAutodetected;
				if (set.has("compression")) {
					compression = (Compression)set.valueOf("compression");
					compressionAutodetected = false;
				} else {
					compression = fileInfo.compressionMethod;
					compressionAutodetected = fileInfo.compressionAutodetected;
				}
				if (compression == null) {
					throw new CommandException("No compression format specified, please specify one with -c");
				}
				File outFile;
				if (fileInfo.sourceFile == FileInfo.STDIN) {
					outFile = null;
				} else {
					outFile = fileInfo.sourceFile;
				}
				if (!args.isEmpty()) {
					String str = args.get(0);
					if (str.startsWith("~/")) {
						outFile = new File(System.getProperty("user.home")+str.substring(1));
					} else {
						outFile = new File(str);
					}
				}
				if (outFile == null) {
					throw new CommandException("No file specified");
				}
				try {
					DataOutput out = endianness.wrap(compression.wrap(new FileOutputStream(outFile)));
					try (Closeable c = (Closeable)out) {
						NBTIO.writeTag(out, root);
						if (outFile == fileInfo.sourceFile || set.has("default")) {
							fileInfo = new FileInfo(outFile, compression, compressionAutodetected, endianness);
						}
						dirty = false;
					}
				} catch (Exception e) {
					NBTEd.log("Error occurred while writing", e);
					// TODO detect common exceptions and print useful messages
					throw new CommandException("An error occurred while writing");
				}
			}));
	}
	
	private static String humanReadableBytes(long bytes, boolean si) {
		String c = si ? "" : "i";
		double divisor = si ? 1000 : 1024;
		if (bytes > divisor) {
			double kb = bytes/divisor;
			if (kb > divisor) {
				double mb = kb/divisor;
				if (mb > divisor) {
					// !? why would you have an NBT file this large
					double gb = mb/divisor;
					if (gb > divisor) {
						// ...
						double tb = gb/divisor;
						return TWO_FRAC_FMT.format(tb)+"T"+c+"B";
					}
					return TWO_FRAC_FMT.format(gb)+"G"+c+"B";
				}
				return ONE_FRAC_FMT.format(mb)+"M"+c+"B";
			}
			return ONE_FRAC_FMT.format(kb)+"K"+c+"B";
		}
		return bytes+"B";
	}
	
	private void addCommand(Command command) {
		for (String s : command.getAllNames()) {
			if (commands.containsKey(s)) throw new IllegalArgumentException("Duplicate command name "+s);
			commands.put(s, command);
		}
	}

	private Completer pathCompleter(boolean compoundOnly) {
		return (reader, line, candidates) -> {
			if (line.word().startsWith("-") && line.wordIndex() < line.words().indexOf("--")) return;
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
				if (tag instanceof CompoundTag) {
					for (Tag t : ((CompoundTag)tag).values()) {
						if (!compoundOnly || t instanceof CompoundTag) {
							boolean leaf = !(t instanceof CompoundTag) || ((CompoundTag)t).isEmpty();
							String suff = leaf ? "" : "/";
							candidates.add(new Candidate(prefix+t.getName()+suff, t.getName()+suff, null, null, suff, null, leaf));
						}
					}
				} else if (!compoundOnly) {
					candidates.add(new Candidate(prefix+left, tag.getName(), null, null, null, null, true));
				}
			}
		};
	}
	
	private Tag resolvePath(String string, boolean error, boolean compoundOnly) throws CommandException {
		Tag cursorWork = cursor;
		if (string.startsWith("/")) {
			cursorWork = root;
		}
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
								throw new CommandException("No such tag with path "+string);
							} else {
								return null;
							}
						}
					}
				}
			} else if (cursorWork == null) {
				if (error) {
					throw new CommandException("No such tag with path "+string);
				} else {
					return null;
				}
			} else {
				if (s == null && !compoundOnly) continue;
				if (error) {
					throw new CommandException("Cannot cd into non-compound "+s);
				} else {
					return null;
				}
			}
		}
		return cursorWork;
	}

	public void run() throws Exception {
		if (running) return;
		reader = LineReaderBuilder.builder()
				.appName("unbted")
				.completer(this)
				.history(history)
				.terminal(NBTEd.terminal)
				.highlighter(this)
				.variable(LineReader.HISTORY_FILE, System.getProperty("user.home")+"/.unbted_history")
				.option(Option.CASE_INSENSITIVE, true)
				.option(Option.ERASE_LINE_ON_FINISH, true)
				.build();
		running = true;
		while (running) {
			String prompt = "["+getPath(cursor)+"]> ";
			try {
				String raw;
				List<String> words;
				try {
					raw = reader.readLine("%{"+AnsiCode.RESET+"%}"+(dirty ? AnsiCode.FG_YELLOW_INTENSE+"*" : " ")+"%{"+AnsiCode.RESET+"%}"+prompt);
					ParsedLine parsed = reader.getParsedLine();
					words = parsed.words();
				} catch (EndOfFileException e) {
					raw = "exit";
					words = Collections.singletonList("exit");
				}
				System.err.print(AnsiCode.RESET);
				System.err.print((dirty ? AnsiCode.FG_YELLOW_INTENSE+"*" : " "));
				System.err.print(AnsiCode.RESET);
				System.err.print(prompt);
				System.err.print(highlight(reader, raw).toAnsi(NBTEd.terminal));
				System.err.println();
				if (!words.isEmpty()) {
					String commandStr = words.get(0);
					if (!Strings.isNullOrEmpty(commandStr)) {
						if (Strings.isNullOrEmpty(words.get(words.size()-1))) {
							words = words.subList(0, words.size()-1);
						}
						Command command = commands.get(commandStr);
						if (command != null) {
							try {
								command.execute(commandStr, words.subList(1, words.size()));
							} catch (CommandException e) {
								System.err.println(reader.getAppName()+": "+commandStr+": "+e.getMessage());
							}
						} else {
							System.err.println(reader.getAppName()+": Unknown command");
						}
					}
				}
			} catch (UserInterruptException e) {
				System.err.print((dirty ? AnsiCode.FG_YELLOW_INTENSE+"*" : " "));
				System.err.print(AnsiCode.RESET);
				System.err.print(prompt);
				System.err.print(highlight(reader, e.getPartialLine()).toAnsi(NBTEd.terminal));
				System.err.print(AnsiCode.RESET);
				System.err.print("^C");
				System.err.println();
			}
		}
	}
	
	private String getPath(Tag t) {
		List<String> parts = Lists.newArrayList();
		while (t != null) {
			parts.add(t.getName());
			t = t.getParent();
		}
		if (parts.isEmpty()) {
			return "(empty file)";
		} else {
			String str = SLASH_JOINER.join(Lists.reverse(parts));
			if (str.trim().isEmpty()) {
				str = "/";
			}
			return str;
		}
	}
	
	public void stop() throws Exception {
		if (!running) return;
		running = false;
		history.save();
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		if (reader.isSet(Option.DISABLE_HIGHLIGHTER)) return;
		if (line.wordIndex() == 0) {
			for (Map.Entry<String, Command> en : commands.entrySet()) {
				candidates.add(new Candidate(en.getKey(), en.getKey(), null, en.getValue().getDescription(), null, en.getValue().getName(), true));
			}
		} else if (line.words().size() > 1) {
			List<String> words = line.words();
			String commandStr = words.get(0);
			Command command = commands.get(commandStr);
			if (command != null) {
				if (!line.words().contains("--") || line.wordIndex() < line.words().indexOf("--")) {
					OptionParser parser = new OptionParser();
					try {
						command.setupOptionParser(parser);
						Map<OptionSpec<?>, String> firstEncounters = Maps.newHashMap();
						for (Map.Entry<String, OptionSpec<?>> en : parser.recognizedOptions().entrySet()) {
							if (en.getValue() instanceof OptionDescriptor) {
								OptionDescriptor od = (OptionDescriptor)en.getValue();
								if (od.representsNonOptions()) continue;
								String str = "-"+(en.getKey().length() > 1 ? "-" : "")+en.getKey();
								String key = firstEncounters.computeIfAbsent(en.getValue(), (spec) -> en.getKey());
								candidates.add(new Candidate(str+(od.requiresArgument() ? "=" : ""), str, line.word().startsWith("-") ? null : "options", od.description(), null, key, true));
							}
						}
					} catch (Exception e) {
						NBTEd.log("Exception while attempting to complete options", e);
					}
					
				}
				command.complete(reader, line, candidates);
			}
		}
	}

	private static final Pattern HIGHLIGHTED = Pattern.compile("([^\\\\]|^)([\"'](?:.*?[^\\\\]|)([\\\"']|$)|\\\\.)");
	
	@Override
	public AttributedString highlight(LineReader reader, String buffer) {
		AttributedStringBuilder asb = new AttributedStringBuilder(buffer.length());
		ParsedLine parsed = reader.getParser().parse(buffer, buffer.length(), ParseContext.COMPLETE);
		String commandStr;
		if (!parsed.words().isEmpty()) {
			commandStr = parsed.words().get(0);
		} else {
			commandStr = buffer;
		}
		Command command = commands.get(commandStr);
		AttributedStyle commandStyle = new AttributedStyle();
		if (command != null) {
			if ("rem".equals(command.getName())) {
				asb.append(buffer, new AttributedStyle().foreground(AttributedStyle.RED));
				return asb.toAttributedString();
			} else {
				commandStyle = commandStyle.foreground(AttributedStyle.BLUE);
			}
		} else {
			commandStyle = commandStyle.foreground(AttributedStyle.RED).bold();
		}
		asb.append(commandStr, commandStyle);
		AttributedStyle basicStyle = new AttributedStyle().foreground(AttributedStyle.BLUE).bold();
		Matcher matcher = HIGHLIGHTED.matcher(buffer.substring(commandStr.length()));
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
