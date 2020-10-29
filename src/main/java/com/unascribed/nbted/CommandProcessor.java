/*
 * unbted - Una's NBT Editor
 * Copyright (C) 2018 - 2020 Una Thompson (unascribed)
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;
import com.google.common.primitives.Ints;

import com.google.gson.JsonObject;
import com.unascribed.miniansi.AnsiCode;
import com.unascribed.nbted.TagPrinter.RecurseMode;

import io.github.steveice10.opennbt.NBTIO;
import io.github.steveice10.opennbt.NBTRegistry;
import io.github.steveice10.opennbt.tag.NBTCompound;
import io.github.steveice10.opennbt.tag.NBTIndexed;
import io.github.steveice10.opennbt.tag.NBTList;
import io.github.steveice10.opennbt.tag.NBTParent;
import io.github.steveice10.opennbt.tag.NBTString;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.NBTByteArray;
import io.github.steveice10.opennbt.tag.array.support.NBTArrayFake;
import io.github.steveice10.opennbt.tag.number.NBTByte;
import io.github.steveice10.opennbt.tag.number.NBTDouble;
import io.github.steveice10.opennbt.tag.number.NBTFloat;
import io.github.steveice10.opennbt.tag.number.NBTInt;
import io.github.steveice10.opennbt.tag.number.NBTLong;
import io.github.steveice10.opennbt.tag.number.NBTNumber;
import io.github.steveice10.opennbt.tag.number.NBTShort;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;

import static com.unascribed.nbted.CommandException.*;
import static com.unascribed.nbted.ResolvePathOption.*;

public class CommandProcessor implements Completer, Highlighter {
	
	private static final NumberFormat ONE_FRAC_FMT = new DecimalFormat("#,##0.0");
	private static final NumberFormat TWO_FRAC_FMT = new DecimalFormat("#,##0.00");
	
	private static final Joiner SPACE_JOINER = Joiner.on(' ');
	private static final Joiner NONE_JOINER = Joiner.on("");
	
	private static final Pattern ECHO_ESCAPE = Pattern.compile("(?:\\\\0([0-7]{1,3})|\\\\x([0-9a-fA-F]{1,2})|\\\\u([0-9a-fA-F]{1,4})|\\\\U([0-9a-fA-F]{1,8}))");
	
	private static final Pattern PATH_SEGMENT = Pattern.compile("(?:^|\\/)([^\\[\\]\\/]*?)(?:$|\\/)|\\[([^\\[\\]]*?)\\]");
	
	private boolean running = false;
	
	private FileInfo fileInfo;
	
	private LineReader reader;
	
	private NBTTag root;
	private NBTTag cursor;
	
	private final TagPrinter printer;
	private final DefaultHistory history = new DefaultHistory();
	private final Map<String, Command> commands = Maps.newHashMap();
	
	private boolean dirty = false;
	
	public CommandProcessor(NBTTag _root, TagPrinter _printer, FileInfo _fileInfo) {
		this.root = _root;
		this.cursor = root;
		this.printer = _printer;
		this.fileInfo = _fileInfo;
		addCommand(Command.create()
			.name("warranty")
			.description("show GPLv3 warranty sections")
			.usage("{}")
			.action((set, args) -> {
				if (args.size() > 0) throw new CommandUsageException("Too many arguments");
				NBTEd.displayEmbeddedFileInPager("warranty.txt");
			}));
		addCommand(Command.create()
			.name("copying")
			.description("show GPLv3 text")
			.usage("{}")
			.action((set, args) -> {
				if (args.size() > 0) throw new CommandUsageException("Too many arguments");
				NBTEd.displayEmbeddedFileInPager("license.txt");
			}));
		addCommand(Command.create()
			.name("help").aliases("?", "h")
			.description("show help")
			.usage("{}")
			.action((set, args) -> {
				if (args.size() > 0) throw new CommandUsageException("Too many arguments");
				NBTEd.displayEmbeddedFileInPager("commands-help.txt");
			}));
		addCommand(Command.create()
			.name("rem").aliases("comment", "remark", "#", "//")
			.description("do nothing")
			.usage("{} [anything]..."));
		addCommand(Command.create()
			.name("echo")
			.description("print arguments")
			.usage("{} [anything]...")
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
			.usage("{} <path>")
			.completer(pathCompleter(true))
			.action((set, args) -> {
				if (args.size() > 1) throw new CommandUsageException("Too many arguments");
				if (args.isEmpty()) {
					cursor = root;
				} else {
					cursor = resolvePath(args.get(0), PARENTS_ONLY).leaf;
				}
			}));
		addCommand(Command.create()
			.name("ls").aliases("dir", "get")
			.description("list tag contents")
			.usage("{} [path]...")
			.completer(pathCompleter(false))
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("R", "recursive"), "list all descendants recursively");
				parser.acceptsAll(Arrays.asList("d", "directory"), "print just the tag, no children");
				parser.acceptsAll(Arrays.asList("s", "include-self"), "print the tag in addition to children");
				parser.acceptsAll(Arrays.asList("r", "raw"), "do not infer types");
				parser.acceptsAll(Arrays.asList("base64"), "print byte arrays as base64");
				parser.acceptsAll(Arrays.asList("l", "1", "a", "A"), "ignored");
			})
			.action((alias, set, args) -> {
				boolean infer = NBTEd.INFER;
				if (set.has("raw")) {
					infer = false;
				}
				Iterable<NBTTag> tags;
				if (args.isEmpty()) {
					tags = Collections.singleton(cursor);
				} else {
					tags = Iterables.transform(args, s -> resolvePath(s).leaf);
				}
				for (NBTTag tag : tags) {
					if (set.has("directory")) {
						printer.printTag(tag, "", infer, RecurseMode.NONE, true);
					} else {
						RecurseMode mode = RecurseMode.IMMEDIATE_CHILDREN_ONLY;
						if (set.has("recursive")) {
							mode = RecurseMode.FULL;
						} else if (set.has("include-self") || "get".equals(alias)) {
							mode = RecurseMode.IMMEDIATE_CHILDREN;
						}
						printer.printTag(tag, "", infer, mode, true);
					}
				}
			}));
		addCommand(Command.create()
			.name("rm").aliases("del", "rmdir", "rd")
			.description("delete a tag")
			.usage("{} <path>...")
			.completer(pathCompleter(false))
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("r", "recursive"), "delete non-empty compounds too");
				parser.acceptsAll(Arrays.asList("f", "force"), "silence errors and keep going");
			})
			.action((set, args) -> {
				if (args.isEmpty()) throw new CommandUsageException("Missing argument");
				// REVERSE ORDER - cursor is at index 0, root is at (size()-1)
				List<NBTTag> contextParents = Lists.newArrayList();
				{
					NBTTag t = cursor;
					while (t != null) {
						contextParents.add(t);
						t = asTag(t.getParent());
					}
				}
				for (String s : args) {
					try {
						NBTTag t = resolvePath(s).leaf;
						if (t instanceof NBTCompound) {
							NBTCompound ct = (NBTCompound)t;
							if (!ct.isEmpty() && !set.has("recursive")) {
								throw new CommandException(VALUE_CMDSPECIFIC_1, "Refusing to delete non-empty compound "+getPath(t)+" - add -r to override");
							}
						}
						NBTTag parent = asTag(t.getParent());
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
							if (parent instanceof NBTCompound) {
								NBTCompound ct = (NBTCompound)parent;
								if (ct.contains(t.getName())) {
									ct.remove(t.getName());
									dirty = true;
								} else {
									throw new ConsistencyError("Tried to delete tag from parent, but it's not in its parent!?");
								}
							} else {
								throw new CommandException(VALUE_NYI, "Tried to delete tag with non-compound parent (NYI)");
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
			.name("exit").aliases("abort", "quit", "q")
			.description("exit without saving")
			.usage("{}")
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("f", "force"), "don't confirm with unsaved changes");
			})
			.action((alias, set, args) -> {
				if (!args.isEmpty()) throw new CommandUsageException("Too many arguments");
				if (dirty && !alias.equals("abort") && !set.has("force")) {
					if (!prompt("There are unsaved changes. Are you sure you want to exit?", false)) {
						return;
					}
				}
				stop();
			}));
		addCommand(Command.create()
			.name("info")
			.description("print information about the loaded file")
			.usage("{}")
			.options((parser) -> {
				parser.acceptsAll(Arrays.asList("si", "s"), "use si units instead of binary");
			})
			.action((set, args) -> {
				if (!args.isEmpty()) throw new CommandUsageException("Too many arguments");
				System.out.print("Root tag name: ");
				System.out.println(root == null ? "(no root tag)" : Strings.isNullOrEmpty(root.getName()) ? "(none)" : root.getName());
				System.out.print("File: ");
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
				System.out.print(fileInfo.compressionMethod == null ? "Not set" : fileInfo.compressionMethod);
				if (fileInfo.compressionMethod != null && fileInfo.compressionAutodetected) {
					System.out.print(" (detected)");
				}
				System.out.println();
				if (!fileInfo.isJson) {
					System.out.print("Endianness: ");
					System.out.println(fileInfo.endianness);
				}
				System.out.print("File size: ...calculating...");
				System.out.flush();
				OutputStream out = ByteStreams.nullOutputStream();
				CountingOutputStream compressedCounter = null;
				if (fileInfo.compressionMethod != Compression.NONE && fileInfo.compressionMethod != null) {
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
			.usage("{} [file]")
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
				parser.mutuallyExclusive(
						parser.acceptsAll(Arrays.asList("roundtrip-json", "json", "j", "J"), "write in roundtrip NBT JSON format"),
						parser.acceptsAll(Arrays.asList("nbt", "N"), "write in NBT format")
				);
				parser.acceptsAll(Arrays.asList("force", "f"), "just do it, don't ask questions");
			})
			.action((set, args) -> {
				if (args.size() > 1) throw new CommandUsageException("Too many arguments");
				if (root == null) {
					throw new CommandException(VALUE_TAG_NOT_FOUND, "Nothing to write");
				}
				boolean json;
				if (set.has("roundtrip-json")) {
					json = true;
				} else if (set.has("nbt")) {
					json = false;
				} else {
					json = fileInfo.isJson;
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
				if (set.has("compression")) {
					compression = (Compression)set.valueOf("compression");
				} else {
					compression = fileInfo.compressionMethod;
				}
				if (compression == null) {
					if (json) {
						compression = Compression.NONE;
					} else {
						throw new CommandException(VALUE_CMDSPECIFIC_1, "No compression format specified, please specify one with -c");
					}
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
					throw new CommandException(VALUE_CMDSPECIFIC_2, "No file specified");
				}
				if (!set.has("force")) {
					if (json) {
						 if (outFile.getName().endsWith(".dat") || outFile.getName().endsWith(".nbt")) {
							if (!prompt("You are saving a JSON file with an NBT extension. Are you sure you want to do this?", false)) {
								return;
							}
						} else if (compression == Compression.DEFLATE) {
							if (!prompt("You are saving a JSON file with DEFLATE compression. This generally does not make sense. Are you sure you want to do this?", true)) {
								return;
							}
						} else if (compression == Compression.GZIP) {
							if (!outFile.getName().endsWith(".json.gz")) {
								if (!prompt("You are saving a gzipped JSON file with an extension other than .json.gz. Are you sure you want to do this?", false)) {
									return;
								}
							}
						} else {
							if (!outFile.getName().endsWith(".json")) {
								if (!prompt("You are saving a JSON file with an extension other than .json. Are you sure you want to do this?", false)) {
									return;
								}
							}
						}
					} else {
						if (outFile.getName().endsWith(".json.gz") || outFile.getName().endsWith(".json")) {
							if (!prompt("You are saving an NBT file with a JSON extension. Are you sure you want to do this?", false)) {
								return;
							}
						} else if (!outFile.getName().endsWith(".dat") && !outFile.getName().endsWith(".nbt")) {
							if (!prompt("You are saving an NBT file with a nonstandard extension. Are you sure you want to do this?", true)) {
								return;
							}
						}
					}
				}
				try {
					try (OutputStream out = compression.wrap(new FileOutputStream(outFile))) {
						if (json) {
							try (OutputStreamWriter osw = new OutputStreamWriter(out, Charsets.UTF_8)) {
								JsonObject obj = new JsonObject();
								obj.addProperty("_unbted", 1);
								obj.addProperty("rootType", NBTEd.getTypePrefix(root));
								obj.addProperty("rootName", root.getName());
								obj.add("root", NBTEd.toJson(root, true));
								NBTEd.gson.toJson(obj, osw);
							}
						} else {
							if (!(root instanceof NBTCompound)) {
								System.err.println("unbted: save: warning: NBT files with non-compound roots are poorly supported");
							}
							NBTIO.writeTag(endianness.wrap(out), root);
						}
						if (fileInfo.sourceFile == null || outFile == fileInfo.sourceFile || set.has("default")) {
							fileInfo = new FileInfo(outFile, compression, false, endianness, json);
						}
						dirty = false;
					}
				} catch (Exception e) {
					NBTEd.log("Error occurred while writing", e);
					// TODO detect common exceptions and print useful messages
					throw new CommandException(VALUE_GENERAL_ERROR, "An error occurred while writing");
				}
			}));
		addCommand(Command.create()
			.name("mkdir")
			.description("create compounds")
			.usage("{} <path>...")
			.completer(pathCompleter(true))
			.options((parser) -> {
				parser.accepts("p", "ignored");
			})
			.action((set, args) -> {
				if (args.isEmpty()) throw new CommandUsageException("Missing argument");
				for (String s : args) {
					NBTTag tag = resolvePath(s, NO_ERROR).leaf;
					if (tag == null) {
						commands.get("set").execute("set", "--type=compound", s);
					} else if (!(tag instanceof NBTCompound)) {
						throw new CommandException(VALUE_WONT_OVERWRITE, s+" already exists and is not a compound");
					}
				}
			}));
		addCommand(Command.create()
			.name("set").aliases("put", "new", "create", "add")
			.description("write values")
			.usage("{} <path> [data]...")
			.completer(pathCompleter(false))
			.options((parser) -> {
				parser.accepts("no-overwrite", "error when attempting to overwrite");
				parser.accepts("shift", "move list values aside instead of overwriting");
				parser.accepts("type", "type of value to set").withRequiredArg();
				List<OptionSpecBuilder> exclusive = Lists.newArrayListWithCapacity(NBTRegistry.allByTypeName().size());
				for (String s : NBTRegistry.allByTypeName().keySet()) {
					exclusive.add(parser.accepts(s, "equivalent to --type="+s).availableUnless("type"));
				}
				exclusive.add(parser.accepts("uuid", "equivalent to --type=uuid").availableUnless("type"));
				parser.mutuallyExclusive(exclusive.toArray(new OptionSpecBuilder[exclusive.size()]));
			})
			.action((alias, set, args) -> {
				if (args.isEmpty()) throw new CommandUsageException("Not enough arguments");
				boolean noOverwrite = set.has("no-overwrite");
				if ("create".equals(alias) || "new".equals(alias)) noOverwrite = true;
				boolean shift = set.has("shift");
				if ("add".equals(alias)) shift = true;
				String path = args.get(0);
				boolean uuid = false;
				Class<? extends NBTTag> explicitType = null;
				if (set.has("type")) {
					String typeStr = set.valueOf("type").toString();
					if ("uuid".equals(typeStr)) {
						uuid = true;
					} else {
						explicitType = NBTRegistry.classByTypeName(typeStr);
						if (explicitType == null) throw new CommandException(VALUE_BAD_USAGE, "Unrecognized type "+set.valueOf("type"));
					}
				} else {
					if (set.has("uuid")) {
						uuid = true;
					} else {
						for (Map.Entry<String, Class<? extends NBTTag>> en : NBTRegistry.allByTypeName().entrySet()) {
							if (set.has(en.getKey())) {
								explicitType = en.getValue();
								break;
							}
						}
					}
				}
				String str = SPACE_JOINER.join(args.subList(1, args.size()));
				if (root == null) {
					if (uuid) {
						throw new CommandException(VALUE_CMDSPECIFIC_4, "UUIDs are actually two tags, and cannot be the root of a file");
					}
					if (explicitType == null) {
						throw new CommandException(VALUE_CMDSPECIFIC_3, "An explicit type must be specified to create new tags");
					}
					NBTTag tag = NBTRegistry.createInstance(explicitType, path);
					try {
						parseAndSet(tag, str);
					} catch (NumberFormatException e) {
						throw new CommandException(VALUE_CMDSPECIFIC_2, "Invalid number "+str);
					}
					root = tag;
					cursor = tag;
					dirty = true;
					return;
				}
				String pathNoTrailingSlashes = path;
				while (pathNoTrailingSlashes.endsWith("/")) {
					pathNoTrailingSlashes = pathNoTrailingSlashes.substring(0, pathNoTrailingSlashes.length()-1);
				}
				ResolvedPath p = resolvePath(path, CREATE_PARENTS, SOFT_IOOBE);
				if (p.leaf == null && explicitType == null) {
					ResolvedPath maybe = resolvePath(pathNoTrailingSlashes+"Most", NO_ERROR);
					if (maybe.leaf != null && maybe.leaf instanceof NBTLong) {
						maybe = resolvePath(pathNoTrailingSlashes+"Least", NO_ERROR);
						if (maybe.leaf != null && maybe.leaf instanceof NBTLong) {
							uuid = true;
						}
					}
				}
				if (uuid) {
					UUID u;
					try {
						u = UUID.fromString(str);
					} catch (IllegalArgumentException e) {
						throw new CommandException(VALUE_BAD_USAGE, str+" is not a valid UUID");
					}
					commands.get("set").execute("set", "--type=long", "--", pathNoTrailingSlashes+"Most", Long.toString(u.getMostSignificantBits()));
					commands.get("set").execute("set", "--type=long", "--", pathNoTrailingSlashes+"Least", Long.toString(u.getLeastSignificantBits()));
					return;
				}
				if (p.leaf != null && !(p.immediateParent instanceof NBTIndexed)) {
					if (noOverwrite) {
						throw new CommandException(VALUE_WONT_OVERWRITE, "Refusing to overwrite existing tag");
					}
					if (p.leaf instanceof NBTIndexed && shift) {
						NBTIndexed idx = (NBTIndexed)p.leaf;
						if (explicitType != null && explicitType != idx.getElementType()) {
							throw new CommandException(VALUE_CMDSPECIFIC_1, "Explicit type "+NBTRegistry.typeNameFromClass(explicitType)+" is incompatible with list type "+NBTRegistry.typeNameFromClass(idx.getElementType()));
						}
						if (explicitType == null && idx.getElementType() == null) {
							throw new CommandException(VALUE_CMDSPECIFIC_1, "Must specify an explicit type to add an initial tag to a list");
						}
						NBTTag tag = NBTRegistry.createInstance(idx.getElementType(), "");
						try {
							parseAndSet(tag, str);
						} catch (NumberFormatException e) {
							throw new CommandException(VALUE_CMDSPECIFIC_2, "Invalid number "+str);
						}
						idx.add(tag);
					} else {
						if (explicitType != null && explicitType != p.leaf.getClass()) {
							throw new CommandException(VALUE_CMDSPECIFIC_1, "Explicit type "+NBTRegistry.typeNameFromClass(explicitType)+" is incompatible with existing type "+NBTRegistry.typeNameFromClass(p.leaf.getClass()));
						}
						try {
							parseAndSet(p.leaf, str);
							dirty = true;
						} catch (NumberFormatException e) {
							throw new CommandException(VALUE_CMDSPECIFIC_2, "Invalid number "+str);
						}
					}
				} else if (p.immediateParent != null) {
					if (explicitType == null && p.immediateParent instanceof NBTIndexed) {
						explicitType = ((NBTIndexed)p.immediateParent).getElementType();
					}
					if (explicitType == null) {
						throw new CommandException(VALUE_CMDSPECIFIC_3, "An explicit type must be specified to create new tags");
					}
					String name = path.substring(p.parentPath.length());
					NBTTag tag = NBTRegistry.createInstance(explicitType, p.immediateParent instanceof NBTIndexed ? "" : name);
					try {
						parseAndSet(tag, str);
					} catch (NumberFormatException e) {
						throw new CommandException(VALUE_CMDSPECIFIC_2, "Invalid number "+str);
					}
					if (p.immediateParent instanceof NBTIndexed) {
						NBTIndexed li = (NBTIndexed)p.immediateParent;
						Integer idx = Ints.tryParse(name.endsWith("]") ? name.substring(0, name.length()-1) : name);
						if (idx == null || idx < 0) {
							throw new CommandException(VALUE_TAG_NOT_FOUND, name+" is not a valid list index");
						}
						if (idx > li.size()) {
							throw new CommandException(VALUE_TAG_NOT_FOUND, idx+" is out of bounds");
						}
						if (idx == li.size()) {
							li.add(tag);
						} else {
							if (shift) {
								li.add(idx, tag);
							} else {
								li.set(idx, tag);
							}
						}
					} else if (p.immediateParent instanceof NBTCompound) {
						((NBTCompound)p.immediateParent).put(tag);
					} else {
						throw new CommandException(VALUE_GENERAL_ERROR, "Unrecognized parent type "+p.immediateParent.getClass().getSimpleName());
					}
					dirty = true;
				} else {
					throw new CommandException(VALUE_TAG_NOT_FOUND, "Failed to resolve path");
				}
			}));
	}
	
	private boolean prompt(String str, boolean def) {
		try {
			reader.setOpt(Option.DISABLE_HIGHLIGHTER);
			reader.unsetOpt(Option.ERASE_LINE_ON_FINISH);
			while (true) {
				String line = reader.readLine(str+(def ? " [Y/n] " : " [y/N] "));
				if (line.trim().isEmpty()) line = def ? "y" : "n";
				switch (line.charAt(0)) {
					case 'n':
					case 'N':
						return false;
					case 'y':
					case 'Y':
						return true;
					default:
						System.err.println("Unrecognized choice "+line+". Enter Y or N.");
						break;
				}
			}
		} catch (UserInterruptException | EndOfFileException e) {
			return false;
		} finally {
			reader.unsetOpt(Option.DISABLE_HIGHLIGHTER);
			reader.setOpt(Option.ERASE_LINE_ON_FINISH);
		}
	}

	private void parseAndSet(NBTTag tag, String str) {
		if (tag instanceof NBTNumber) {
			if ("true".equals(str)) {
				str = "1";
			} else if ("false".equals(str)) {
				str = "0";
			}
		}
		if (tag instanceof NBTByte) {
			((NBTByte)tag).setValue(Byte.decode(str.trim()));
		} else if (tag instanceof NBTShort) {
			((NBTShort)tag).setValue(Short.decode(str.trim()));
		} else if (tag instanceof NBTInt) {
			((NBTInt)tag).setValue(Integer.decode(str.trim()));
		} else if (tag instanceof NBTLong) {
			((NBTLong)tag).setValue(Long.decode(str.trim()));
		} else if (tag instanceof NBTFloat) {
			((NBTFloat)tag).setValue(Float.parseFloat(str));
		} else if (tag instanceof NBTDouble) {
			((NBTDouble)tag).setValue(Double.parseDouble(str));
		} else if (tag instanceof NBTString) {
			((NBTString)tag).setValue(str);
		} else if (tag instanceof NBTByteArray) {
			try {
				((NBTByteArray)tag).setValue(BaseEncoding.base64().decode(str));
			} catch (IllegalArgumentException e) {
				throw new CommandException(VALUE_BAD_USAGE, "Invalid base64");
			}
		} else if (!str.trim().isEmpty()) {
			throw new CommandException(VALUE_BAD_USAGE, "Tags of type "+NBTRegistry.typeNameFromClass(tag.getClass())+" cannot be created with a value");
		} else if (tag instanceof NBTParent) {
			((NBTParent)tag).clear();
		}
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

	private Completer pathCompleter(boolean parentOnly) {
		return (reader, line, candidates) -> {
			if (line.word().startsWith("-") && line.wordIndex() < line.words().indexOf("--")) return;
			ResolvedPath p = resolvePath(line.word(), NO_ERROR, parentOnly ? PARENTS_ONLY : null);
			NBTParent subject;
			if (p.leaf != null && p.leaf instanceof NBTParent) {
				subject = (NBTParent)p.leaf;
			} else {
				subject = p.immediateParent;
			}
			if (subject != null) {
				if (subject instanceof NBTCompound) {
					String parentPath = Strings.nullToEmpty(p.parentPath);
					if (!parentPath.isEmpty() && !p.parentPath.endsWith("/")) {
						parentPath += "/";
					}
					for (NBTTag tag : subject) {
						String suffix = tag instanceof NBTCompound ? "/" : "";
						String str = parentPath+tag.getName()+suffix;
						if (!parentOnly || tag instanceof NBTParent) {
							candidates.add(new Candidate(str, str, null, null, suffix, null, !(tag instanceof NBTParent)));
						}
					}
					if (p.parentPath == null || !p.parentPath.endsWith("/")) {
						candidates.add(new Candidate(parentPath, parentPath, null, null, "/", null, false));
					}
				} else if (subject instanceof NBTList) {
					String parentPath = Strings.nullToEmpty(p.leaf != null ? line.word() : p.parentPath);
					if (parentPath.endsWith("/")) {
						parentPath = parentPath.substring(0, parentPath.length()-1);
					}
					for (int i = 0; i < subject.size(); i++) {
						NBTTag tag = ((NBTList)subject).get(i);
						String suffix = tag instanceof NBTCompound ? "/" : "";
						String str = parentPath+"["+i+"]"+suffix;
						if (!parentOnly || tag instanceof NBTParent) {
							candidates.add(new Candidate(str, str, null, null, suffix, null, !(tag instanceof NBTParent)));
						}
					}
					if (!candidates.isEmpty() && !parentPath.endsWith("[")) {
						candidates.add(new Candidate(parentPath+"[", parentPath+"[", null, null, "[", null, false));
					}
				}
			}
		};
	}

	private ResolvedPath resolvePath(String path, ResolvePathOption... optionsArr) throws CommandException {
		List<ResolvePathOption> options = Arrays.asList(optionsArr);
		NBTTag cursorWork = cursor;
		if (path.startsWith("/")) {
			cursorWork = root;
		}
		NBTParent immediateParent = cursorWork == null ? null : cursorWork.getParent();
		// shhhhhhHHHHHH
		path = path.replace("/", "//").replace("[", "/[").replace("]//", "]/");
		Matcher m = PATH_SEGMENT.matcher(path);
		String parentPath = "";
		try {
			while (m.find()) {
				String seg = MoreObjects.firstNonNull(m.group(1), m.group(2)).replace("//", "/");
				if (".".equals(seg) || seg.isEmpty()) {
					// this cast is safe due to checks lower in the loop
					// we'll never reach this point if cursorWork is not
					// an NBTParent
					immediateParent = (NBTParent)cursorWork;
					parentPath = path.substring(0, m.start());
					continue;
				}
				if ("..".equals(seg)) {
					if (cursorWork == null) throw new CommandException(VALUE_TAG_NOT_FOUND, "Cannot traverse above nothing");
					if (cursorWork.getParent() == null) throw new CommandException(VALUE_TAG_NOT_FOUND, "Cannot traverse above root");
					cursorWork = asTag(cursorWork.getParent());
					immediateParent = cursorWork.getParent();
					parentPath = path.substring(0, m.start());
					continue;
				}
				if (cursorWork instanceof NBTCompound) {
					NBTCompound c = (NBTCompound)cursorWork;
					immediateParent = c;
					parentPath = path.substring(0, m.start());
					if (c.contains(seg)) {
						cursorWork = c.get(seg);
					} else {
						if (options.contains(CREATE_PARENTS)) {
							if (m.hitEnd()) {
								return new ResolvedPath(immediateParent, null, parentPath.replace("/[", "[").replace("//", "/"), null);
							}
							cursorWork = new NBTCompound(seg);
							c.put(cursorWork);
						} else {
							throw new CommandException(VALUE_TAG_NOT_FOUND, path.substring(0, m.end()).replace("/[", "[").replace("//", "/")+" does not exist");
						}
					}
				} else if (cursorWork instanceof NBTIndexed) {
					NBTIndexed l = (NBTIndexed)cursorWork;
					Integer i = Ints.tryParse(seg);
					immediateParent = l;
					parentPath = path.substring(0, m.start());
					if (i == null || i < 0) {
						throw new CommandException(VALUE_TAG_NOT_FOUND, seg+" is not a valid list index");
					}
					if (i >= l.size()) {
						if (options.contains(SOFT_IOOBE)) {
							return new ResolvedPath(immediateParent, null, parentPath.replace("/[", "[").replace("//", "/"), null);
						}
						throw new CommandException(VALUE_TAG_NOT_FOUND, seg+" is out of bounds");
					}
					cursorWork = l.get(i);
				} else {
					throw new CommandException(VALUE_TAG_NOT_FOUND, "Cannot traverse into "+(cursorWork == null ? "null" : NBTRegistry.typeNameFromClass(cursorWork.getClass())));
				}
			}
			if (!options.contains(PARENTS_ONLY) || cursorWork instanceof NBTParent) {
				return new ResolvedPath(immediateParent, cursorWork, parentPath.replace("/[", "[").replace("//", "/"), getPath(cursorWork));
			} else {
				throw new CommandException(VALUE_TAG_NOT_FOUND, (cursorWork == null ? "null" : NBTRegistry.typeNameFromClass(cursorWork.getClass()))+" is not valid here");
			}
		} catch (CommandException e) {
			if (!options.contains(NO_ERROR)) throw e;
			return new ResolvedPath(immediateParent, null, parentPath.replace("/[", "[").replace("//", "/"), null);
		}
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
								if (NBTEd.VERBOSE) {
									e.printStackTrace();
								}
								System.err.println(reader.getAppName()+": "+commandStr+": "+e.getMessage());
								if (e instanceof CommandUsageException) {
									System.err.println(reader.getAppName()+": "+commandStr+": usage: "+command.getUsage(commandStr));
								}
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

	private String getPath(NBTTag t) {
		List<String> parts = Lists.newArrayList();
		while (t != null) {
			NBTParent parent = t.getParent();
			if (t instanceof NBTArrayFake) {
				parts.add("["+((NBTArrayFake)t).getIndex()+"]");
			} else if (parent instanceof NBTList) {
				parts.add("["+((NBTList)parent).indexOf(t)+"]");
			} else if (parent instanceof NBTCompound) {
				parts.add("/"+t.getName());
			} else {
				parts.add(t.getName());
			}
			t = asTag(parent);
		}
		if (parts.isEmpty()) {
			return "(empty file)";
		} else {
			String str = NONE_JOINER.join(Lists.reverse(parts));
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
		Matcher matcher = HIGHLIGHTED.matcher(buffer.substring(commandStr.length()).replace("''", "'\f'").replace("\"\"", "\"\f\""));
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
			if ("'\f'".equals(body)) {
				body = "''";
			} else if ("\"\f\"".equals(body)) {
				body = "\"\"";
			}
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
	
	private NBTTag asTag(NBTParent parent) {
		// always safe
		return (NBTTag)parent;
	}

}
