package com.unascribed.nbted;

import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.unascribed.miniansi.AnsiCode;
import com.unascribed.miniansi.AnsiStream;

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

public class TagPrinter {
	
	public enum RecurseMode {
		/**
		 * Only print the passed tag, and a summary of its children if it has
		 * them.
		 */
		NONE,
		/**
		 * Print the passed tag and its immediate children.
		 */
		IMMEDIATE_CHILDREN,
		/**
		 * Print the passed tag's immediate children.
		 */
		IMMEDIATE_CHILDREN_ONLY,
		/**
		 * Print the passed tag and all of its descendants.
		 */
		FULL;
		public boolean shouldPrintRoot() {
			return this != IMMEDIATE_CHILDREN_ONLY;
		}
		public boolean shouldPrintChildren() {
			return this != NONE;
		}
		public RecurseMode degrade() {
			switch (this) {
				case NONE: return NONE;
				case IMMEDIATE_CHILDREN: return NONE;
				case IMMEDIATE_CHILDREN_ONLY: return NONE;
				case FULL: return FULL;
				default: throw new AssertionError("missing case for "+this);
			}
		}
	}

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
	
	private static final ImmutableList<String> knownBooleanNames = ImmutableList.of(
			"hardcore"
			);
	private static final ImmutableList<String> likelyBooleanPrefixes = ImmutableList.of(
			"has", "is", "seen", "should", "on", "flag", "bool", "boolean"
			);
	private static final ImmutableList<String> likelyBooleanSuffixes = ImmutableList.of(
			"ing", "locked", "flag", "boolean", "bool"
			);

	
	private final AnsiStream aout;
	private final Function<byte[], String> byteArrayFormatter;
	
	private final Gson gson = new Gson();
	
	public TagPrinter(PrintStream out, Function<byte[], String> byteArrayFormatter) {
		this(new AnsiStream(out), byteArrayFormatter);
	}
	
	public TagPrinter(AnsiStream out, Function<byte[], String> byteArrayFormatter) {
		this.aout = out;
		this.byteArrayFormatter = byteArrayFormatter;
	}
	
	public void printTag(Tag tag, String prefix, boolean infer, RecurseMode recurse) {
		if (tag == null) {
			return;
		}
		if (tag instanceof CompoundTag) {
			CompoundTag ct = (CompoundTag)tag;
			if (recurse.shouldPrintRoot()) {
				aout.print(prefix);
				aout.print("compound", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
				printName(tag.getName(), false);
			}
			if (recurse.shouldPrintChildren()) {
				if (ct.getValue().isEmpty()) {
					if (recurse.shouldPrintRoot()) aout.println(" {}", AnsiCode.RESET);
				} else {
					if (recurse.shouldPrintRoot()) aout.println(" {", AnsiCode.RESET);
					String childPrefix = recurse.shouldPrintRoot() ? prefix+"  " : prefix;
					for (Tag t : ct.getValue().values()) {
						if (infer) {
							if (t.getName().endsWith("Most") && ct.contains(t.getName().replaceFirst("Most$", "Least"))) {
								Tag most = t;
								Tag least = ct.get(t.getName().replaceFirst("Most$", "Least"));
								if (most instanceof LongTag && least instanceof LongTag) {
									UUID u = new UUID((long)most.getValue(), (long)least.getValue());
									printBasic(u, t.getName().replaceFirst("Most$", ""), "~uuid", AnsiCode.FG_YELLOW_INTENSE, childPrefix);
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
						printTag(t, childPrefix, infer, recurse.degrade());
					}
					if (recurse.shouldPrintRoot()) {
						aout.print(prefix);
						aout.println("}");
					}
				}
			} else if (recurse.shouldPrintRoot()) {
				aout.print(" (", AnsiCode.RESET);
				aout.print(ct.size());
				aout.print(" child");
				if (ct.size() != 1) {
					aout.print("ren");
				}
				aout.println(")");
			}
		} else if (tag instanceof ListTag) {
			ListTag lt = (ListTag)tag;
			if (lt.getValue().isEmpty()) {
				if (recurse.shouldPrintRoot()) {
					aout.print(prefix);
					aout.print("list", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
					printName(tag.getName(), false);
					if (recurse.shouldPrintChildren()) {
						aout.println(" []", AnsiCode.RESET);
					} else {
						aout.println(" (0 children)", AnsiCode.RESET);
					}
				}
			} else {
				boolean forgeRegistry = true;
				if (recurse.shouldPrintChildren()) {
					Class<?> registryType = null;
					if (infer) {
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
				} else {
					forgeRegistry = false;
				}
				if (forgeRegistry) {
					if (recurse.shouldPrintRoot()) {
						aout.print(prefix);
						aout.print("~registry", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
						printName(tag.getName(), false);
						aout.println(" [", AnsiCode.RESET);
					}
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
							if (recurse.shouldPrintRoot()) aout.print("  ");
							printName(ct.get("K").getValue().toString(), true);
							printVal(ct.get("V").getValue());
							continue;
						}
					}
					if (recurse.shouldPrintRoot()) {
						aout.print(prefix);
						aout.println("]");
					}
				} else {
					if (recurse.shouldPrintRoot()) {
						aout.print(prefix);
						aout.print("list", AnsiCode.FG_WHITE_INTENSE, AnsiCode.BOLD);
						printName(tag.getName(), false);
					}
					if (recurse.shouldPrintChildren()) {
						if (recurse.shouldPrintRoot()) aout.println(" [", AnsiCode.RESET);
						for (Tag t : lt) {
							printTag(t, recurse.shouldPrintRoot() ? prefix+"  " : prefix, infer, recurse);
						}
						if (recurse.shouldPrintRoot()) {
							aout.print(prefix);
							aout.println("]");
						}
					} else if (recurse.shouldPrintRoot()) {
						aout.print(" (", AnsiCode.RESET);
						aout.print(lt.size());
						aout.print(" child");
						if (lt.size() != 1) {
							aout.print("ren");
						}
						aout.println(")");
					}
				}
			}
		} else if (tag instanceof ByteTag) {
			if (infer && (byte)tag.getValue() == 0 || (byte)tag.getValue() == 1) {
				String lowerName = tag.getName().toLowerCase(Locale.ROOT);
				boolean maybeBoolean = false;
				if (knownBooleanNames.contains(lowerName)) {
					maybeBoolean = true;
				} else {
					for (String s : likelyBooleanPrefixes) {
						if (lowerName.startsWith(s)) {
							maybeBoolean = true;
							break;
						}
					}
					if (!maybeBoolean) {
						for (String s : likelyBooleanSuffixes) {
							if (lowerName.endsWith(s)) {
								maybeBoolean = true;
								break;
							}
						}
					}
				}
				if (maybeBoolean) {
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
			if (infer) {
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
						printBasic(recurse.shouldPrintChildren() ? "..." : jstr, tag.getName(), "~json", AnsiCode.FG_RED_INTENSE, prefix);
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
	
	public void printBasic(Tag tag, String type, AnsiCode color, String prefix) {
		Object val = tag.getValue();
		if (val instanceof String) {
			val = "\""+escaper.escape((String)val)+"\"";
		}
		printBasic(val, tag.getName(), type, color, prefix);
	}

	public void printBasic(Object val, String name, String type, AnsiCode color, String prefix) {
		aout.print(prefix);
		aout.print(type, color);
		printName(name, true);
		printVal(val);
	}

	public void printVal(Object val) {
		aout.print(AnsiCode.FG_GREEN);
		if (val instanceof byte[]) {
			byte[] bys = (byte[])val;
			String str = byteArrayFormatter.apply(bys);
			if (NBTEd.TRUNCATE && str.length() > 32) {
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

	public void printName(String name, boolean equals) {
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

}
