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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.unascribed.miniansi.AnsiCode;
import com.unascribed.miniansi.AnsiStream;

import io.github.steveice10.opennbt.NBTIO;
import io.github.steveice10.opennbt.tag.TagRegistry;
import io.github.steveice10.opennbt.tag.builtin.ByteArrayTag;
import io.github.steveice10.opennbt.tag.builtin.ByteTag;
import io.github.steveice10.opennbt.tag.builtin.CompoundTag;
import io.github.steveice10.opennbt.tag.builtin.DoubleTag;
import io.github.steveice10.opennbt.tag.builtin.FloatTag;
import io.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import io.github.steveice10.opennbt.tag.builtin.IntTag;
import io.github.steveice10.opennbt.tag.builtin.ListTag;
import io.github.steveice10.opennbt.tag.builtin.LongArrayTag;
import io.github.steveice10.opennbt.tag.builtin.LongTag;
import io.github.steveice10.opennbt.tag.builtin.ShortTag;
import io.github.steveice10.opennbt.tag.builtin.StringTag;
import io.github.steveice10.opennbt.tag.builtin.Tag;
import io.github.steveice10.opennbt.tag.builtin.custom.DoubleArrayTag;
import io.github.steveice10.opennbt.tag.builtin.custom.FloatArrayTag;
import io.github.steveice10.opennbt.tag.builtin.custom.ShortArrayTag;
import io.github.steveice10.opennbt.tag.builtin.custom.StringArrayTag;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;

public class NBTEd {

	private static final String HELP;
	static {
		try {
			HELP = Resources.toString(ClassLoader.getSystemResource("switches-help.txt"), Charsets.UTF_8);
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
	public static boolean VERBOSE = false;
	public static boolean JSON = false;
	public static boolean TRUNCATE = true;
	public static boolean DECIMAL_BYTES = false;
	public static boolean INFER = true;
	
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
	
	private static Function<byte[], String> byteArrayFormatter;
	
	private static final Escaper escaper = Escapers.builder()
				.addEscape('\u0000', "\\0")
				.addEscape('\u0001', "\\x01")
				.addEscape('\u0002', "\\x02")
				.addEscape('\u0003', "\\x03")
				.addEscape('\u0004', "\\x04")
				.addEscape('\u0005', "\\x05")
				.addEscape('\u0006', "\\x06")
				.addEscape('\u0007', "\\a")
				.addEscape('\u0008', "\\b")
				.addEscape('\u0009', "\\t")
				.addEscape('\n', "\\n")
				.addEscape('\u000B', "\\v")
				.addEscape('\u000C', "\\f")
				.addEscape('\r', "\\r")
				.addEscape('\u000E', "\\x0E")
				.addEscape('\u000F', "\\x0F")
				.addEscape('\u0010', "\\x10")
				.addEscape('\u0011', "\\x11")
				.addEscape('\u0012', "\\x12")
				.addEscape('\u0013', "\\x13")
				.addEscape('\u0014', "\\x14")
				.addEscape('\u0015', "\\x15")
				.addEscape('\u0016', "\\x16")
				.addEscape('\u0017', "\\x17")
				.addEscape('\u0018', "\\x18")
				.addEscape('\u0019', "\\x19")
				.addEscape('\u001A', "\\x1A")
				.addEscape('\u001B', "\\e")
				.addEscape('\u001C', "\\x1C")
				.addEscape('\u001D', "\\x1D")
				.addEscape('\u001E', "\\x1E")
				.addEscape('\u001F', "\\x1F")
				.addEscape('\u007F', "\\x7F")
				.addEscape('\\', "\\\\")
				.build();
	
	private static final ImmutableSet<String> knownBooleans = ImmutableSet.of("hardcore");
	
	public static void main(String[] args) throws Exception {
		UncaughtExceptionHandler ueh = (t, e) -> {
			if (VERBOSE) {
				e.printStackTrace();
			} else {
				String str = "";
				for (Map.Entry<Class<? extends Throwable>, String> en : commonExceptions.entrySet()) {
					if (en.getKey().isAssignableFrom(e.getClass())) {
						str = en.getValue();
						break;
					}
				}
				System.err.println("An unexpected "+str+" error occurred"+(e.getMessage() == null ? "." : ":"));
				if (e.getMessage() != null) {
					System.err.println("    "+e.getMessage());
				}
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(ueh);
		Thread.currentThread().setUncaughtExceptionHandler(ueh);
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("help", "h", "?")).forHelp();
		parser.mutuallyExclusive(
			parser.acceptsAll(Arrays.asList("extended", "e")),
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
		Tag tag = is == null ? null : NBTIO.readTag(is);
		if (!set.has("no-print")) {
			printTag(tag, "");
		}
		if (!set.has("print")) {
			System.err.println("Una's NBT Editor v1.0 - Copyright (C) 2018 Una Thompson (unascribed)");
			System.err.println("This program comes with ABSOLUTELY NO WARRANTY; for details type `warranty`."); 
			System.err.println("This is free software, and you are welcome to redistribute it");
			System.err.println("under certain conditions; type `copying` for details.");
			System.err.println();
			System.err.println("Enter ? for help");
			CommandProcessor cp = new CommandProcessor(tag);
			cp.run();
		}
	}
	
	private static void printTag(Tag tag, String prefix) {
		if (tag == null) {
			aout.print("(new buffer)", AnsiCode.FG_BLACK_INTENSE);
			aout.println(AnsiCode.RESET);
			return;
		}
		if (JSON) {
			JsonElement e = toJson(tag);
			StringWriter sw = new StringWriter();
			JsonWriter jw = new JsonWriter(sw);
			jw.setIndent("  ");
			jw.setLenient(true);
			gson.toJson(e, jw);
			aout.println(sw.toString());
		} else {
			if (tag instanceof CompoundTag) {
				CompoundTag ct = (CompoundTag)tag;
				aout.print(prefix);
				aout.print("compound", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
				printName(tag.getName(), false);
				if (ct.getValue().isEmpty()) {
					aout.println(" {}", AnsiCode.RESET);
				} else {
					aout.println(" {", AnsiCode.RESET);
					for (Tag t : ct.getValue().values()) {
						if (INFER) {
							if (t.getName().endsWith("Most") && ct.contains(t.getName().replaceFirst("Most$", "Least"))) {
								Tag most = t;
								Tag least = ct.get(t.getName().replaceFirst("Most$", "Least"));
								if (most instanceof LongTag && least instanceof LongTag) {
									UUID u = new UUID((long)most.getValue(), (long)least.getValue());
									printBasic(u, t.getName().replaceFirst("Most$", ""), "~uuid", AnsiCode.FG_YELLOW_INTENSE, prefix+"  ");
									continue;
								}
							}
							if (t.getName().endsWith("Least") && ct.contains(t.getName().replaceFirst("Least$", "Most"))) {
								Tag most = ct.get(t.getName().replaceFirst("Least$", "Most"));
								Tag least = t;
								if (most instanceof LongTag && least instanceof LongTag) {
									continue;
								}
							}
						}
						printTag(t, prefix+"  ");
					}
					aout.print(prefix);
					aout.println("}");
				}
			} else if (tag instanceof ListTag) {
				ListTag lt = (ListTag)tag;
				if (lt.getValue().isEmpty()) {
					aout.print(prefix);
					aout.print("list", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
					printName(tag.getName(), false);
					aout.println(" []", AnsiCode.RESET);
				} else {
					boolean forgeRegistry = true;
					Class<?> registryType = null;
					if (INFER) {
						for (Tag t : lt.getValue()) {
							if (t instanceof CompoundTag) {
								CompoundTag ct = (CompoundTag)t;
								if (ct.size() == 2 && ct.contains("K") && ct.contains("V") && ct.get("V").getValue() instanceof Comparable) {
									if (registryType == null) {
										registryType = ct.get("V").getClass();
										continue;
									} else if (registryType == ct.get("V").getClass()) {
										continue;
									}
								}
							}
							forgeRegistry = false;
							break;
						}
					} else {
						forgeRegistry = false;
					}
					if (forgeRegistry) {
						aout.print(prefix);
						aout.print("~registry", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
						printName(tag.getName(), false);
						aout.println(" [", AnsiCode.RESET);
						// these assumptions are safe due to the checks above that set the forgeRegistry flag
						@SuppressWarnings({"unchecked", "rawtypes"})
						List<CompoundTag> copy = (List)lt.getValue();
						copy.sort(new Comparator<CompoundTag>() {
							@Override
							@SuppressWarnings({"unchecked", "rawtypes"})
							public int compare(CompoundTag a, CompoundTag b) {
								return ((Comparable)a.get("V").getValue()).compareTo(b.get("V").getValue());
							}
						});
						for (Tag t : copy) {
							CompoundTag ct = (CompoundTag)t;
							if (ct.size() == 2 && ct.contains("K") && ct.contains("V")) {
								aout.print(prefix);
								aout.print("  ");
								printName(ct.get("K").getValue().toString(), true);
								printVal(ct.get("V").getValue());
								continue;
							}
						}
						aout.print(prefix);
						aout.println("]");
					} else {
						aout.print(prefix);
						aout.print("list", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
						printName(tag.getName(), false);
						aout.println(" [", AnsiCode.RESET);
						for (Tag t : lt) {
							printTag(t, prefix+"  ");
						}
						aout.print(prefix);
						aout.println("]");
					}
				}
			} else if (tag instanceof ByteTag) {
				if (INFER && (byte)tag.getValue() == 0 || (byte)tag.getValue() == 1) {
					String lowerName = tag.getName().toLowerCase(Locale.ROOT);
					if (knownBooleans.contains(lowerName) || lowerName.startsWith("has") ||
							lowerName.startsWith("is") || lowerName.startsWith("seen") ||
							lowerName.startsWith("should") || lowerName.startsWith("on") ||
							lowerName.endsWith("ing") || lowerName.endsWith("locked") ||
							lowerName.startsWith("flag") || lowerName.endsWith("flag") ||
							lowerName.endsWith("bool") || lowerName.endsWith("boolean") ||
							lowerName.startsWith("bool") || lowerName.startsWith("boolean")) {
						printBasic((byte)tag.getValue() != 0, tag.getName(), "~bool", AnsiCode.FG_YELLOW, prefix);
						return;
					}
				}
				printBasic(tag, "byte", AnsiCode.FG_YELLOW, prefix);
			} else if (tag instanceof ShortTag) {
				printBasic(tag, "short", AnsiCode.FG_YELLOW, prefix);
			} else if (tag instanceof IntTag) {
				printBasic(tag, "int", AnsiCode.FG_YELLOW, prefix);
			} else if (tag instanceof LongTag) {
				printBasic(tag, "long", AnsiCode.FG_YELLOW, prefix);
			} else if (tag instanceof FloatTag) {
				printBasic(tag, "float", AnsiCode.FG_MAGENTA, prefix);
			} else if (tag instanceof DoubleTag) {
				printBasic(tag, "double", AnsiCode.FG_MAGENTA, prefix);
			} else if (tag instanceof StringTag) {
				if (INFER) {
					String str = (String)tag.getValue();
					if (str.startsWith("{") || str.startsWith("[")) {
						try {
							JsonElement je = gson.fromJson(str, JsonElement.class);
							StringWriter sw = new StringWriter();
							JsonWriter jw = new JsonWriter(sw);
							jw.setIndent("  ");
							jw.setLenient(true);
							gson.toJson(je, jw);
							String jstr = sw.toString()
									.replaceAll("[\\]\\[]", AnsiCode.RESET+"$1")
									.replace("\n", "\n"+prefix+"  "+AnsiCode.FG_BLUE_INTENSE)
									.replace(":", AnsiCode.RESET+":"+AnsiCode.FG_GREEN)
									.replace(",", AnsiCode.RESET+",")
									.replace("{", AnsiCode.RESET+"{")
									.replace("}", AnsiCode.RESET+"}")
									;
							printBasic(jstr, tag.getName(), "~json", AnsiCode.FG_RED_INTENSE, prefix);
							return;
						} catch (Exception e) {}
					}
				}
				printBasic(tag, "string", AnsiCode.FG_RED, prefix);
			} else if (tag instanceof ByteArrayTag) {
				printBasic(tag, "byte[]", AnsiCode.FG_YELLOW_INTENSE, prefix);
			} else if (tag instanceof IntArrayTag) {
				printBasic(tag, "int[]", AnsiCode.FG_YELLOW_INTENSE, prefix);
			} else if (tag instanceof LongArrayTag) {
				printBasic(tag, "long[]", AnsiCode.FG_YELLOW_INTENSE, prefix);
			} else if (tag instanceof StringArrayTag) {
				printBasic(tag, "!string[]", AnsiCode.FG_RED_INTENSE, prefix);
			} else if (tag instanceof ShortArrayTag) {
				printBasic(tag, "!short[]", AnsiCode.FG_YELLOW_INTENSE, prefix);
			} else if (tag instanceof FloatArrayTag) {
				printBasic(tag, "!float[]", AnsiCode.FG_MAGENTA_INTENSE, prefix);
			} else if (tag instanceof DoubleArrayTag) {
				printBasic(tag, "!double[]", AnsiCode.FG_MAGENTA_INTENSE, prefix);
			}
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

	private static void printBasic(Tag tag, String type, AnsiCode color, String prefix) {
		Object val = tag.getValue();
		if (val instanceof String) {
			val = "\""+escaper.escape((String)val)+"\"";
		}
		printBasic(val, tag.getName(), type, color, prefix);
	}
	
	private static void printBasic(Object val, String name, String type, AnsiCode color, String prefix) {
		aout.print(prefix);
		aout.print(type, color);
		printName(name, true);
		printVal(val);
	}
	
	private static void printVal(Object val) {
		aout.print(AnsiCode.FG_GREEN);
		if (val instanceof byte[]) {
			byte[] bys = (byte[])val;
			String str = byteArrayFormatter.apply(bys);
			if (TRUNCATE && str.length() > 32) {
				aout.print(str.substring(0, 33));
				aout.print("... (");
				aout.print(str.length()-32);
				aout.print(" more)");
			} else {
				aout.print(str);
			}
		} else if (val instanceof short[]) {
			aout.print(Arrays.toString((short[])val));
		} else if (val instanceof int[]) {
			aout.print(Arrays.toString((int[])val));
		} else if (val instanceof long[]) {
			aout.print(Arrays.toString((long[])val));
		} else if (val instanceof float[]) {
			aout.print(Arrays.toString((float[])val));
		} else if (val instanceof double[]) {
			aout.print(Arrays.toString((double[])val));
		} else if (val instanceof Object[]) {
			aout.print(Arrays.toString((Object[])val));
		} else {
			aout.print(val);
		}
		aout.println(AnsiCode.RESET);
	}
	
	private static void printName(String name, boolean equals) {
		if (name != null && !name.isEmpty()) {
			aout.print(" \"", AnsiCode.FG_BLUE_INTENSE);
			aout.print(escaper.escape(name));
			aout.print("\"");
			if (equals) {
				aout.print(" = ", AnsiCode.RESET);
			}
		} else if (equals) {
			aout.print(" ");
		}
	}

	private static void printUsage() {
		System.err.println("Usage: unbted [options] [file]");
		System.err.println("See `unbted --help` for detailed usage information");
	}
	
	private static void printHelp() {
		System.err.println(HELP);
	}
	
}
