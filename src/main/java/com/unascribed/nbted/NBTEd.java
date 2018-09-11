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

import java.io.BufferedInputStream;
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
import java.util.Locale;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.jline.builtins.Less;
import org.jline.builtins.Source.URLSource;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import com.google.common.base.Function;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.unascribed.miniansi.AnsiStream;
import com.unascribed.nbted.TagPrinter.RecurseMode;

import io.github.steveice10.opennbt.NBTIO;
import io.github.steveice10.opennbt.tag.TagRegistry;
import io.github.steveice10.opennbt.tag.builtin.CompoundTag;
import io.github.steveice10.opennbt.tag.builtin.ListTag;
import io.github.steveice10.opennbt.tag.builtin.Tag;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;

public class NBTEd {
	public static boolean VERBOSE = false;
	public static boolean JSON = false;
	public static boolean TRUNCATE = true;
	public static boolean DECIMAL_BYTES = false;
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
			System.err.println(fmt.format(new Date())+" "+String.format(msg.replace("%", "%%").replace("{}", "%s"), args));
		}
	}
	
	public static final AnsiStream aout = new AnsiStream(System.out);
	private static final Gson gson = new Gson();
	public static Terminal terminal;
	
	private static Function<byte[], String> byteArrayFormatter;
	
	public static void main(String[] args) throws Exception {
		UncaughtExceptionHandler ueh = (t, e) -> {
			if (VERBOSE) {
				e.printStackTrace();
			} else {
				String str = "";
				for (Map.Entry<Class<? extends Throwable>, String> en : commonExceptions.entrySet()) {
					if (en.getKey().isAssignableFrom(e.getClass())) {
						str = en.getValue()+" ";
						break;
					}
				}
				System.err.println("An unexpected "+str+"error occurred"+(e.getMessage() == null ? "." : ":"));
				if (e.getMessage() != null) {
					System.err.println("    "+e.getMessage());
				}
				System.err.println("(run with --verbose for more information)");
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(ueh);
		Thread.currentThread().setUncaughtExceptionHandler(ueh);
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("help", "h", "?")).forHelp();
		parser.mutuallyExclusive(
			parser.acceptsAll(Arrays.asList("extended", "x")),
			parser.acceptsAll(Arrays.asList("old", "o"))
		);
		parser.acceptsAll(Arrays.asList("little", "le", "l"));
		OptionSpec<Compression> compression = parser.acceptsAll(Arrays.asList("compression", "c")).withRequiredArg().ofType(Compression.class)
				.withValuesConvertedBy(new ValueConverter<Compression>() {

					@Override
					public Compression convert(String value) {
						return Compression.valueOf(value.toUpperCase(Locale.ROOT));
					}

					@Override
					public Class<? extends Compression> valueType() {
						return Compression.class;
					}

					@Override
					public String valuePattern() {
						return "";
					}
					
				});
		parser.acceptsAll(Arrays.asList("debug", "verbose", "d", "v"));
		parser.mutuallyExclusive(
			parser.acceptsAll(Arrays.asList("print", "p")),
			parser.acceptsAll(Arrays.asList("no-print", "n"))
		);
		parser.acceptsAll(Arrays.asList("strict", "json", "s", "j"));
		parser.mutuallyExclusive(
				parser.acceptsAll(Arrays.asList("base64", "6")),
				parser.acceptsAll(Arrays.asList("decimal", "d"))
			);
		parser.acceptsAll(Arrays.asList("full", "f"));
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
		if (set.has("verbose")) {
			VERBOSE = true;
		} else {
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
		InputStream is;
		if (in == null || in.isEmpty()) {
			is = null;
		} else { 
			if ("-".equals(in)) {
				is = System.in;
				log("Reading from stdin");
			} else {
				File f = new File(in);
				is = new FileInputStream(f);
				log("Reading from file {}", f);
			}
			is = new BufferedInputStream(is);
		}
		if (set.has("extended")) {
			TagRegistry.EXTENSIONS_ENABLED = true;
			log("OpenNBT extensions enabled (except the dangerous Serializable tags)");
		}
		if (set.has("old")) {
			TagRegistry.NEWTAGS_ENABLED = false;
			log("Newer NBT additions disabled (i.e. int and long arrays)");
		}
		if (set.has("json")) {
			JSON = true;
			TRUNCATE = false;
		} else if (set.has("full")) {
			TRUNCATE = false;
		}
		if (set.has("base64")) {
			byteArrayFormatter = BaseEncoding.base64()::encode;
		} else if (set.has("decimal")) {
			byteArrayFormatter = Arrays::toString;
			DECIMAL_BYTES = true;
		} else {
			byteArrayFormatter = BaseEncoding.base16().upperCase()::encode;
		}
		if (set.has("raw")) {
			INFER = false;
		}
		Compression c = set.valueOf(compression);
		if (c == null) {
			if (is != null) {
				is.mark(2);
				int magic8 = is.read() & 0xff;
				int magic16 = magic8 | ((is.read() << 8) & 0xff00);
				is.reset();
				if (magic16 == GZIPInputStream.GZIP_MAGIC) {
					c = Compression.GZIP;
				} else if (magic8 == 0x78) {
					c = Compression.DEFLATE;
				} else {
					c = Compression.NONE;
				}
				log("Compression autodetected as {}", c);
			} else {
				log("No compression specified for new buffer, defaulting to GZip");
				c = Compression.GZIP;
			}
		} else {
			log("Compression set as {}", c);
		}
		is = c.wrap(is);
		Tag tag = is == null ? null : NBTIO.readTag(is, set.has("little"), null);
		TagPrinter printer = new TagPrinter(System.out, byteArrayFormatter);
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
			System.err.println("Una's NBT Editor v1.0");
			System.err.println("Copyright (C) 2018 Una Thompson (unascribed)");
			System.err.println("This program comes with ABSOLUTELY NO WARRANTY; for details type `warranty`."); 
			System.err.println("This is free software, and you are welcome to redistribute it under certain");
			System.err.println("conditions; type `copying` for details.");
			System.err.println();
			System.err.println("Type `help` for help");
			CommandProcessor cp = new CommandProcessor(tag, printer);
			cp.run();
		}
	}
	
	private static JsonElement toJson(Tag tag) {
		if (tag instanceof CompoundTag) {
			JsonObject out = new JsonObject();
			CompoundTag in = (CompoundTag)tag;
			for (Tag t : in.getValue().values()) {
				out.add(t.getName(), toJson(t));
			}
			return out;
		} else if (tag instanceof ListTag) {
			JsonArray out = new JsonArray();
			ListTag in = (ListTag)tag;
			for (Tag t : in) {
				out.add(toJson(t));
			}
			return out;
		}
		Object val = tag.getValue();
		return bareToJson(val);
	}
	
	private static JsonElement bareToJson(Object val) {
		if (val instanceof String) {
			return new JsonPrimitive((String)val);
		} else if (val instanceof Number) {
			return new JsonPrimitive((Number)val);
		} else if (val instanceof byte[]) {
			if (DECIMAL_BYTES) {
				JsonArray out = new JsonArray();
				for (byte v : (byte[])val) { out.add(v); }
				return out;
			} else {
				return new JsonPrimitive(byteArrayFormatter.apply((byte[])val));
			}
		} else if (val instanceof short[]) {
			JsonArray out = new JsonArray();
			for (short v : (short[])val) { out.add(v); }
			return out;
		} else if (val instanceof int[]) {
			JsonArray out = new JsonArray();
			for (int v : (int[])val) { out.add(v); }
			return out;
		} else if (val instanceof long[]) {
			JsonArray out = new JsonArray();
			for (long v : (long[])val) { out.add(v); }
			return out;
		} else if (val instanceof float[]) {
			JsonArray out = new JsonArray();
			for (float v : (float[])val) { out.add(v); }
			return out;
		} else if (val instanceof double[]) {
			JsonArray out = new JsonArray();
			for (double v : (double[])val) { out.add(v); }
			return out;
		} else if (val instanceof Object[]) {
			JsonArray out = new JsonArray();
			for (Object v : (Object[])val) { out.add(bareToJson(v)); }
			return out;
		} else {
			throw new IllegalArgumentException("Don't know how to convert "+val+" ("+val.getClass().getSimpleName()+") to JSON");
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
