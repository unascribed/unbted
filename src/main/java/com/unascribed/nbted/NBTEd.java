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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.jline.builtins.Less;
import org.jline.builtins.Source.URLSource;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.unascribed.miniansi.AnsiStream;
import com.unascribed.nbted.TagPrinter.RecurseMode;

import io.github.steveice10.opennbt.NBTIO;
import io.github.steveice10.opennbt.tag.NBTCompound;
import io.github.steveice10.opennbt.tag.NBTList;
import io.github.steveice10.opennbt.tag.NBTString;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.NBTByteArray;
import io.github.steveice10.opennbt.tag.array.NBTIntArray;
import io.github.steveice10.opennbt.tag.array.NBTLongArray;
import io.github.steveice10.opennbt.tag.number.NBTNumber;
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
	public static boolean JSON = false;
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
	private static final Gson gson = new Gson();
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
		parser.acceptsAll(Arrays.asList("json", "j"));
		parser.acceptsAll(Arrays.asList("raw", "r"));
		parser.acceptsAll(Arrays.asList("no-pager"));
		parser.posixlyCorrect(System.getenv("POSIXLY_CORRECT") != null);
		NonOptionArgumentSpec<String> nonoption = parser.nonOptions().ofType(String.class);
		
		OptionSet set;
		try {
			set = parser.parse(args);
		} catch (OptionException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(1);
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
		
		terminal = TerminalBuilder.terminal();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				terminal.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
		
		if (set.has("help")) {
			printHelp();
			return;
		}
		String in = set.valueOf(nonoption);
		File sourceFile;
		ExceptableSupplier<InputStream, IOException> inSupplier;
		if (in == null || in.isEmpty()) {
			sourceFile = null;
			inSupplier = null;
		} else { 
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
		if (set.has("json")) {
			JSON = true;
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
		NBTTag tag = null;
		if (inSupplier != null) {
			try {
				if (compressionMethod != null) {
					final Compression compressionMethodFinal = compressionMethod;
					final ExceptableSupplier<InputStream, IOException> currentSupplier = inSupplier;
					inSupplier = () -> compressionMethodFinal.wrap(currentSupplier.get());
				}
				if (endianness != null) {
					try (InputStream is = inSupplier.get()) {
						tag = NBTIO.readTag(endianness.wrap(is));
					}
				} else {
					try {
						try (InputStream is = inSupplier.get()) {
							tag = NBTIO.readTag(is, false);
						}
						if (tag == null) throw new RuntimeException("Got null root tag");
						endianness = Endianness.BIG;
						log("Endianness autodetected as big-endian");
					} catch (Exception e) {
						try {
							try (InputStream is = inSupplier.get()) {
								tag = NBTIO.readTag(is, true);
							}
							if (tag == null) throw new RuntimeException("Got null root tag");
							endianness = Endianness.LITTLE;
							log("Endianness autodetected as little-endian");
						} catch (Exception e2) {
							e2.addSuppressed(e);
							throw e2;
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
			if (JSON) {
				JsonElement e = toJson(tag);
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
			System.err.println("Una's NBT Editor v"+VERSION);
			System.err.println("Copyright (C) 2018 Una Thompson (unascribed)");
			System.err.println("This program comes with ABSOLUTELY NO WARRANTY; for details type `warranty`."); 
			System.err.println("This is free software, and you are welcome to redistribute it under certain");
			System.err.println("conditions; type `copying` for details.");
			System.err.println();
			System.err.println("Type `help` for help");
			CommandProcessor cp = new CommandProcessor(tag, printer, new FileInfo(sourceFile, compressionMethod, compressionAutodetected, endianness));
			cp.run();
		}
	}
	
	private static JsonElement toJson(NBTTag tag) {
		if (tag == null) {
			return JsonNull.INSTANCE;
		} else if (tag instanceof NBTCompound) {
			JsonObject out = new JsonObject();
			NBTCompound in = (NBTCompound)tag;
			for (NBTTag t : in.values()) {
				out.add(t.getName(), toJson(t));
			}
			return out;
		} else if (tag instanceof NBTList) {
			JsonArray out = new JsonArray();
			NBTList in = (NBTList)tag;
			for (NBTTag t : in) {
				out.add(toJson(t));
			}
			return out;
		} else if (tag instanceof NBTNumber) {
			return new JsonPrimitive(((NBTNumber)tag).numberValue());
		} else if (tag instanceof NBTString) {
			return new JsonPrimitive(((NBTString)tag).stringValue());
		} else if (tag instanceof NBTByteArray) {
			return new JsonPrimitive(BaseEncoding.base64().encode(((NBTByteArray)tag).getValue()));
		} else if (tag instanceof NBTIntArray) {
			JsonArray out = new JsonArray();
			for (int v : ((NBTIntArray)tag).getValue()) { out.add(v); }
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
			Less less = new Less(NBTEd.terminal);
			less.run(new URLSource(ClassLoader.getSystemResource(file), file));
		} else {
			Resources.copy(ClassLoader.getSystemResource(file), System.err);
		}
	}
	
}
