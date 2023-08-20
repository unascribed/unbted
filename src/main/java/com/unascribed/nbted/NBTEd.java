/*
 * unbted - Una's NBT Editor
 * Copyright (C) 2018 - 2023 Una Thompson (unascribed)
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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.jline.builtins.Less;
import org.jline.builtins.Source.URLSource;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.unascribed.miniansi.AnsiStream;
import com.unascribed.nbted.TagPrinter.RecurseMode;

import io.github.steveice10.opennbt.NBTIO;
import io.github.steveice10.opennbt.NBTRegistry;
import io.github.steveice10.opennbt.tag.NBTCompound;
import io.github.steveice10.opennbt.tag.NBTList;
import io.github.steveice10.opennbt.tag.NBTString;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.NBTByteArray;
import io.github.steveice10.opennbt.tag.array.NBTIntArray;
import io.github.steveice10.opennbt.tag.array.NBTLongArray;
import io.github.steveice10.opennbt.tag.number.NBTByte;
import io.github.steveice10.opennbt.tag.number.NBTDouble;
import io.github.steveice10.opennbt.tag.number.NBTFloat;
import io.github.steveice10.opennbt.tag.number.NBTInt;
import io.github.steveice10.opennbt.tag.number.NBTLong;
import io.github.steveice10.opennbt.tag.number.NBTNumber;
import io.github.steveice10.opennbt.tag.number.NBTShort;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class NBTEd {
	public static final String VERSION;
	static {
		String ver = null;
		try {
			ver = Resources.toString(ClassLoader.getSystemResource("version.txt"), Charsets.UTF_8);
		} catch (Exception e) {
		}
		VERSION = ver == null ? "?.?" : ver;
	}
	
	public static boolean VERBOSE = true;
	public static JsonMode JSON_MODE = JsonMode.NONE;
	public static boolean INFER = true;
	public static boolean PAGER = true;
	
	private static final Map<Class<? extends Throwable>, String> commonExceptions = new HashMap<>();
	static {
		commonExceptions.put(IndexOutOfBoundsException.class, "index out-of-bounds");
		commonExceptions.put(AssertionError.class, "assertion");
		commonExceptions.put(IllegalArgumentException.class, "illegal argument");
		commonExceptions.put(IllegalStateException.class, "illegal state");
		commonExceptions.put(NullPointerException.class, "null pointer");
		commonExceptions.put(EOFException.class, "end-of-file");
		commonExceptions.put(IOException.class, "IO");
		commonExceptions.put(IOError.class, "IO");
		commonExceptions.put(UncheckedIOException.class, "IO");
	}
	
	private static final DateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
	
	public static void log(String msg, Object... args) {
		if (VERBOSE) {
			Throwable t = null;
			if (args.length > 0 && args[args.length-1] instanceof Throwable) {
				t = (Throwable)args[args.length-1];
				args = Arrays.copyOfRange(args, 0, args.length-1);
			}
			System.err.print("unbted: ");
			System.err.print(fmt.format(new Date()));
			System.err.print(" ");
			System.err.printf(msg.replace("%", "%%").replace("{}", "%s"), args);
			System.err.println();
			if (t != null) {
				t.printStackTrace();
			}
		}
	}
	
	public static final AnsiStream aout = new AnsiStream(System.out);
	public static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
	public static Terminal terminal;
	
	public static void main(String[] args) throws Exception {
		UncaughtExceptionHandler ueh = (t, e) -> {
			if (VERBOSE) {
				e.printStackTrace();
			} else if ("main".equals(t.getName())) {
				String str = "";
				for (Map.Entry<Class<? extends Throwable>, String> en : commonExceptions.entrySet()) {
					if (en.getKey().isAssignableFrom(e.getClass())) {
						str = en.getValue()+" ";
						break;
					}
				}
				System.err.println("unbted: An unexpected "+str+"error occurred"+(e.getMessage() == null ? "." : ":"));
				if (e.getMessage() != null) {
					System.err.println("unbted:     "+e.getMessage());
				}
				System.err.println("unbted: (run with --verbose for more information)");
				System.exit(3);
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(ueh);
		Thread.currentThread().setUncaughtExceptionHandler(ueh);
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("help", "h")).forHelp();
		OptionSpec<Endianness> endiannessOpt = parser.accepts("endian").withRequiredArg().ofType(Endianness.class)
				.withValuesConvertedBy(new CaseInsensitiveEnumConverter<>(Endianness.class));
		parser.mutuallyExclusive(
			parser.accepts("little-endian").availableUnless("endian"),
			parser.accepts("big-endian").availableUnless("endian")
		);
		OptionSpec<Compression> compressionOpt = parser.acceptsAll(Arrays.asList("compression", "c")).withRequiredArg().ofType(Compression.class)
				.withValuesConvertedBy(new CaseInsensitiveEnumConverter<>(Compression.class));
		parser.acceptsAll(Arrays.asList("debug", "verbose", "d", "v"));
		parser.mutuallyExclusive(
			parser.acceptsAll(Arrays.asList("print", "p")),
			parser.acceptsAll(Arrays.asList("no-print", "n"))
		);
		parser.mutuallyExclusive(
			parser.acceptsAll(Arrays.asList("json", "j")),
			parser.acceptsAll(Arrays.asList("roundtrip-json", "J")),
			parser.acceptsAll(Arrays.asList("convert-nbt", "N"))
		);
		parser.acceptsAll(Arrays.asList("raw", "r"));
		parser.acceptsAll(Arrays.asList("no-pager"));
		parser.acceptsAll(Arrays.asList("version", "V"));
		parser.posixlyCorrect(System.getenv("POSIXLY_CORRECT") != null);
		NonOptionArgumentSpec<String> nonoption = parser.nonOptions().ofType(String.class);
		
		OptionSet set;
		try {
			set = parser.parse(args);
		} catch (OptionException e) {
			System.err.println("unbted: "+e.getMessage());
			printUsage();
			System.exit(1);
			return;
		}
		if (set.has("version")) {
			System.err.println("Una's NBT Editor v"+VERSION);
			System.err.println("Copyright (C) 2018 - 2023 Una Thompson (unascribed)");
			System.err.println("License GPLv3+: GNU GPL version 3 or later <https://gnu.org/licenses/gpl.html>.");
			System.err.println("This is free software: you are free to change and redistribute it.");
			System.err.println("There is NO WARRANTY, to the extent permitted by law.");
			return;
		}
		
		if (!set.has("verbose")) {
			VERBOSE = false;
			Logger root = Logger.getLogger("");
			for (Handler h : root.getHandlers()) {
				root.removeHandler(h);
			}
		}
		
		if (set.has("no-pager")) {
			PAGER = false;
		}
		
		if (set.has("help")) {
			initializeTerminal();
			printHelp();
			return;
		}
		List<String> nonoptions = set.valuesOf(nonoption);
		if (set.has("convert-nbt")) {
			if (nonoptions.size() > 2) {
				System.err.println("unbted: Too many arguments - only two arguments, the input NBT JSON and output NBT files, may be specified");
				System.exit(1);
				return;
			}
		} else if (nonoptions.size() > 1) {
			System.err.println("unbted: Too many arguments - only one argument, the input file, may be specified");
			System.exit(1);
			return;
		}
		File sourceFile;
		ExceptableSupplier<InputStream, IOException> inSupplier;
		if (nonoptions.isEmpty()) {
			sourceFile = null;
			inSupplier = null;
		} else {
			String in = nonoptions.get(0);
			if ("-".equals(in)) {
				byte[] bys = ByteStreams.toByteArray(System.in);
				inSupplier = () -> new ByteArrayInputStream(bys);
				sourceFile = FileInfo.STDIN;
				log("Reading from stdin");
			} else {
				File f = new File(in);
				inSupplier = () -> new FileInputStream(f);
				sourceFile = f;
				log("Reading from file {}", f);
			}
		}
		
		if (set.has("convert-nbt")) {
			if (nonoptions.size() < 2) {
				System.err.println("unbted: Not enough arguments - need input NBT JSON file and output NBT file");
				System.exit(1);
				return;
			}
			Compression compression = set.valueOf(compressionOpt);
			if (compression == null) {
				System.err.println("unbted: A compression method must be specified for conversion from NBT JSON");
				System.exit(1);
				return;
			}
			Endianness endianness = Endianness.BIG;
			if (set.has(endiannessOpt)) {
				endianness = set.valueOf(endiannessOpt);
			} else if (set.has("little-endian")) {
				endianness = Endianness.LITTLE;
			} else if (set.has("big-endian")) {
				endianness = Endianness.BIG;
			}
			String out = nonoptions.get(1);
			ExceptableSupplier<OutputStream, IOException> outSupplier;
			if ("-".equals(out)) {
				outSupplier = () -> System.out;
				log("Writing to stdout");
			} else {
				File f = new File(out);
				outSupplier = () -> new FileOutputStream(f);
				log("Writing to file {}", f);
			}
			try {
				NBTTag tag = loadJson(inSupplier.get());
				try (OutputStream os = compression.wrap(outSupplier.get())) {
					NBTIO.writeTag(endianness.wrap(os), tag);
				} catch (Exception e) {
					log("Error occurred while writing", e);
					System.err.println("unbted: Failed to save "+(sourceFile == FileInfo.STDIN ? "(stdin)" : sourceFile.getAbsolutePath()));
					System.err.println("unbted: Are you sure this is an unbted NBT JSON file?");
					System.exit(2);
					return;
				}
			} catch (Exception e) {
				log("Exception while trying to load NBT file", e);
				System.err.println("unbted: Failed to load "+(sourceFile == FileInfo.STDIN ? "(stdin)" : sourceFile.getAbsolutePath()));
				System.err.println("unbted: Are you sure this is an unbted NBT JSON file?");
				System.exit(2);
				return;
			}
			return;
		}
		
		if (set.has("json")) {
			JSON_MODE = JsonMode.BASIC;
		} else if (set.has("roundtrip-json")) {
			JSON_MODE = JsonMode.ROUNDTRIP;
		}
		if (set.has("raw")) {
			INFER = false;
		}
		Compression compressionMethod = set.valueOf(compressionOpt);
		Compression detectedCompressionMethod = null;
		if (inSupplier != null) {
			try (InputStream is = inSupplier.get()) {
				int magic8 = is.read() & 0xff;
				int magic16 = magic8 | ((is.read() << 8) & 0xff00);
				if (magic16 == GZIPInputStream.GZIP_MAGIC) {
					detectedCompressionMethod = Compression.GZIP;
				} else if (magic8 == 0x78) {
					detectedCompressionMethod = Compression.DEFLATE;
				} else if (magic16 == 0xb528) {
					detectedCompressionMethod = Compression.ZSTD;
				} else {
					detectedCompressionMethod = Compression.NONE;
				}
				log("Compression autodetected as {}", detectedCompressionMethod);
			}
		}
		boolean compressionAutodetected;
		if (compressionMethod == null) {
			if (inSupplier != null) {
				compressionMethod = detectedCompressionMethod;
				log("Using autodetected compression method");
				compressionAutodetected = true;
			} else {
				log("No compression specified for new buffer");
				compressionMethod = null;
				compressionAutodetected = false;
			}
		} else {
			log("Compression set as {}", compressionMethod);
			compressionAutodetected = false;
		}
		Endianness endianness = null;
		if (set.has(endiannessOpt)) {
			endianness = set.valueOf(endiannessOpt);
		} else if (set.has("little-endian")) {
			endianness = Endianness.LITTLE;
		} else if (set.has("big-endian")) {
			endianness = Endianness.BIG;
		}
		boolean isJson = false;
		NBTTag tag = null;
		if (inSupplier != null) {
			try {
				if (compressionMethod != null) {
					final Compression compressionMethodFinal = compressionMethod;
					final ExceptableSupplier<InputStream, IOException> currentSupplier = inSupplier;
					inSupplier = () -> compressionMethodFinal.wrap(currentSupplier.get());
				}
				try (PushbackInputStream is = new PushbackInputStream(inSupplier.get())) {
					int firstByte = is.read();
					is.unread(firstByte);
					if (firstByte == '{') {
						isJson = true;
						log("Detected JSON file");
						tag = loadJson(is);
					} else {
						log("Detected binary file");
						if (endianness != null) {
							tag = NBTIO.readTag(endianness.wrap(is));
						} else {
							try {
								tag = NBTIO.readTag(is, false);
								if (tag == null) throw new RuntimeException("Got null root tag");
								endianness = Endianness.BIG;
								log("Endianness autodetected as big-endian");
							} catch (Exception e) {
								try {
									tag = NBTIO.readTag(is, true);
									if (tag == null) throw new RuntimeException("Got null root tag");
									endianness = Endianness.LITTLE;
									log("Endianness autodetected as little-endian");
								} catch (Exception e2) {
									e2.addSuppressed(e);
									throw e2;
								}
							}
						}
					}
				}
				if (tag == null) throw new RuntimeException("Got null root tag");
			} catch (Exception e) {
				log("Exception while trying to load NBT file", e);
				System.err.println("unbted: Failed to load "+(sourceFile == FileInfo.STDIN ? "(stdin)" : sourceFile.getAbsolutePath()));
				if (!compressionAutodetected) {
					System.err.println("unbted: Are you sure "+compressionMethod+" is the correct compression method?");
					if (detectedCompressionMethod != null && detectedCompressionMethod != compressionMethod) {
						System.err.println("unbted: It looks like "+detectedCompressionMethod+" to me");
					}
				} else if (isJson) {
					System.err.println("unbted: Are you sure this is an unbted NBT JSON file?");
				} else {
					System.err.print("unbted: Are you sure this is an NBT file?");
					if (endianness != null) {
						if (endianness == Endianness.ZZAZZ) {
							System.err.print(" (Maybe it's not in a joke format?)");
						} else {
							System.err.print(" (Maybe it's ");
							System.err.print(endianness == Endianness.LITTLE ? "big" : "little");
							System.err.print("-endian?)");
						}
					}
					System.err.println();
				}
				System.exit(2);
				return;
			}
		} else {
			endianness = Endianness.BIG;
		}
		// allow gc, especially for fully-buffered stdin
		inSupplier = null;
		TagPrinter printer = new TagPrinter(System.out);
		if (!set.has("no-print")) {
			if (JSON_MODE != JsonMode.NONE) {
				JsonElement e = toJson(tag, JSON_MODE == JsonMode.ROUNDTRIP);
				if (JSON_MODE == JsonMode.ROUNDTRIP) {
					JsonObject obj = new JsonObject();
					obj.addProperty("_unbted", 1);
					obj.addProperty("rootType", getTypePrefix(tag));
					obj.addProperty("rootName", tag == null ? "" : tag.getName());
					obj.add("root", e);
					e = obj;
				}
				StringWriter sw = new StringWriter();
				JsonWriter jw = new JsonWriter(sw);
				jw.setIndent("  ");
				jw.setLenient(true);
				gson.toJson(e, jw);
				aout.println(sw.toString());
			} else {
				printer.printTag(tag, "", INFER, RecurseMode.FULL);
			}
		}
		if (!set.has("print")) {
			initializeTerminal();
			System.err.println("Una's NBT Editor v"+VERSION);
			System.err.println("Copyright (C) 2018 - 2023 Una Thompson (unascribed)");
			System.err.println("This program comes with ABSOLUTELY NO WARRANTY; for details type `warranty`.");
			System.err.println("This is free software, and you are welcome to redistribute it under certain");
			System.err.println("conditions; type `copying` for details.");
			System.err.println();
			System.err.println("Type `help` for help");
			CommandProcessor cp = new CommandProcessor(tag, printer, new FileInfo(sourceFile, compressionMethod, compressionAutodetected, endianness, isJson));
			cp.run();
		}
	}
	
	private static void initializeTerminal() throws IOException {
		terminal = TerminalBuilder.terminal();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				terminal.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}

	private static NBTTag loadJson(InputStream is) {
		JsonObject json = gson.fromJson(new InputStreamReader(is, Charsets.UTF_8), JsonObject.class);
		JsonElement unbtedMarker = json.get("_unbted");
		if (unbtedMarker != null) {
			int version = unbtedMarker.getAsInt();
			if (version > 1) {
				System.err.println("unbted: This looks like an NBT JSON file, but it's of a version newer than I know how to read. ("+version+")");
				System.err.println("unbted: Aborting.");
				System.exit(2);
				return null;
			} else {
				log("Looks like NBT JSON");
				return fromJson(json.get("rootType").getAsString()+":"+json.get("rootName").getAsString(), json.get("root"));
			}
		} else {
			System.err.println("unbted: This looks like a JSON file, but it's not an NBT JSON file.");
			System.err.println("unbted: Aborting.");
			System.exit(2);
			return null;
		}
	}

	public static String getTypePrefix(NBTTag tag) {
		if (tag == null) {
			return "null";
		} else if (tag instanceof NBTList) {
			NBTList li = (NBTList)tag;
			if (li.getElementType() != null) {
				return "list<"+getTypePrefix(li.get(0))+">";
			}
			return "list<?>";
		} else {
			return NBTRegistry.typeNameForTag(tag);
		}
	}
	
	private static NBTTag fromJson(String name, JsonElement ele) {
		int colon = name.indexOf(':');
		if (colon == -1) throw new IllegalArgumentException("All keys in an unbted NBT JSON file must be prefixed with their type");
		String type = name.substring(0, colon);
		name = name.substring(colon+1);
		if ("null".equals(type)) {
			return null;
		} else if ("byte".equals(type)) {
			return new NBTByte(name, ele.getAsByte());
		} else if ("double".equals(type)) {
			return new NBTDouble(name, ele.getAsDouble());
		} else if ("float".equals(type)) {
			return new NBTFloat(name, ele.getAsFloat());
		} else if ("int".equals(type)) {
			return new NBTInt(name, ele.getAsInt());
		} else if ("long".equals(type)) {
			return new NBTLong(name, ele.getAsLong());
		} else if ("short".equals(type)) {
			return new NBTShort(name, ele.getAsShort());
		} else if ("compound".equals(type)) {
			NBTCompound out = new NBTCompound(name);
			for (Map.Entry<String, JsonElement> en : ele.getAsJsonObject().entrySet()) {
				if ("_unbted".equals(en.getKey())) continue;
				out.put(fromJson(en.getKey(), en.getValue()));
			}
			return out;
		} else if (type.startsWith("list<")) {
			int closer = type.lastIndexOf('>');
			if (closer == -1) {
				throw new IllegalArgumentException("Expected closing > in list type, didn't find one (for "+type+")");
			}
			String innerType = type.substring(5, closer);
			if ("?".equals(innerType)) {
				if (ele == null || ele.getAsJsonArray().size() == 0) {
					return new NBTList(name);
				} else {
					throw new IllegalArgumentException("Cannot have list of unknown type with elements");
				}
			} else {
				NBTList out = new NBTList(name);
				for (JsonElement child : ele.getAsJsonArray()) {
					out.add(fromJson(innerType+":", child));
				}
				return out;
			}
		} else if ("string".equals(type)) {
			return new NBTString(name, ele.getAsString());
		} else if ("byte-array".equals(type)) {
			return new NBTByteArray(name, BaseEncoding.base64().decode(ele.getAsString()));
		} else if ("int-array".equals(type)) {
			JsonArray arr = ele.getAsJsonArray();
			int[] out = new int[arr.size()];
			for (int i = 0; i < out.length; i++) {
				out[i] = arr.get(i).getAsInt();
			}
			return new NBTIntArray(name, out);
		} else if ("long-array".equals(type)) {
			JsonArray arr = ele.getAsJsonArray();
			long[] out = new long[arr.size()];
			for (int i = 0; i < out.length; i++) {
				out[i] = arr.get(i).getAsLong();
			}
			return new NBTLongArray(name, out);
		} else {
			throw new IllegalArgumentException("Unknown type "+type+" when parsing key "+type+":"+name);
		}
	}
	
	public static JsonElement toJson(NBTTag tag, boolean roundTrip) {
		if (tag == null) {
			return JsonNull.INSTANCE;
		} else if (tag instanceof NBTCompound) {
			JsonObject out = new JsonObject();
			NBTCompound in = (NBTCompound)tag;
			for (NBTTag t : in.values()) {
				out.add((roundTrip ? getTypePrefix(t)+":" : "")+t.getName(), toJson(t, roundTrip));
			}
			if (!roundTrip) {
				List<String> keys = Lists.newArrayList(out.keySet());
				Collections.sort(keys);
				JsonObject sorted = new JsonObject();
				for (String k : keys) {
					if (k.endsWith("Least") && sorted.has(k.replaceFirst("Least$", ""))) {
						continue;
					}
					if (k.endsWith("Most") && out.has(k.replaceFirst("Most$", "Least"))) {
						String basek = k.replaceFirst("Most$", "");
						String k2 = basek+"Least";
						if (out.get(k) instanceof JsonPrimitive && out.get(k2) instanceof JsonPrimitive) {
							JsonPrimitive p1 = (JsonPrimitive)out.get(k);
							JsonPrimitive p2 = (JsonPrimitive)out.get(k2);
							if (p1.isNumber() && p2.isNumber()) {
								sorted.add(basek, new JsonPrimitive(new UUID(p1.getAsLong(), p2.getAsLong()).toString()));
								continue;
							}
						}
					}
					sorted.add(k, out.get(k));
				}
				out = sorted;
			}
			return out;
		} else if (tag instanceof NBTList) {
			JsonArray out = new JsonArray();
			NBTList in = (NBTList)tag;
			for (NBTTag t : in) {
				out.add(toJson(t, roundTrip));
			}
			return out;
		} else if (tag instanceof NBTNumber) {
			return new JsonPrimitive(((NBTNumber)tag).numberValue());
		} else if (tag instanceof NBTString) {
			return new JsonPrimitive(((NBTString)tag).stringValue());
		} else if (tag instanceof NBTByteArray) {
			return new JsonPrimitive(BaseEncoding.base64().encode(((NBTByteArray)tag).getValue()));
		} else if (tag instanceof NBTIntArray) {
			NBTIntArray arr = ((NBTIntArray)tag);
			if (!roundTrip && arr.size() == 4) {
				return new JsonPrimitive(UUIDs.fromIntArray(arr.getValue()).toString());
			}
			JsonArray out = new JsonArray();
			for (int v : arr.getValue()) { out.add(v); }
			return out;
		} else if (tag instanceof NBTLongArray) {
			JsonArray out = new JsonArray();
			for (long v : ((NBTLongArray)tag).getValue()) { out.add(v); }
			return out;
		} else {
			throw new IllegalArgumentException("Don't know how to convert "+tag.getClass().getSimpleName()+" to JSON");
		}
	}

	private static void printUsage() {
		System.err.println("Usage: unbted [options] [file]");
		System.err.println("See `unbted --help` for detailed usage information");
	}
	
	private static void printHelp() throws Exception {
		displayEmbeddedFileInPager("switches-help.txt");
	}
	
	public static void displayEmbeddedFileInPager(String file) throws Exception {
		if (PAGER && !"dumb".equals(terminal.getType())) {
			Less less = new Less(NBTEd.terminal, new File("").toPath());
			less.run(Lists.newArrayList(new URLSource(ClassLoader.getSystemResource(file), file)));
		} else {
			Resources.copy(ClassLoader.getSystemResource(file), System.err);
		}
	}
	
}
