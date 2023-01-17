/*
 * Copyright (C) 2013-2017 Steveice10, 2018 - 2023 Una Thompson (unascribed)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.steveice10.opennbt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.util.regex.Pattern;

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
import io.github.steveice10.opennbt.tag.number.NBTShort;

/**
 * A class containing methods for reading/writing stringified NBT tags.
 */
public class SNBTIO {
	/**
	 * Reads stringified root NBTCompound from the given file.
	 *
	 * @param path
	 *            Path of the file.
	 * @return The read compound tag.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static NBTCompound readFile(String path) throws IOException {
		return readFile(new File(path));
	}

	/**
	 * Reads the stringified NBTCompound from the given file.
	 *
	 * @param file
	 *            File to read from.
	 * @return The read compound tag.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static NBTCompound readFile(File file) throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(file));

		NBTTag tag = readTag(in);
		if (!(tag instanceof NBTCompound)) {
			throw new IOException("Root tag is not an NBTCompound!");
		}

		return (NBTCompound) tag;
	}

	/**
	 * Writes the given root NBTCompound to the given file as stringified NBT.
	 *
	 * @param tag
	 *            NBTTag to write.
	 * @param path
	 *            Path to write to.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static void writeFile(NBTCompound tag, String path) throws IOException {
		writeFile(tag, new File(path));
	}

	/**
	 * Writes the given root NBTCompound to the given file as stringified NBT.
	 *
	 * @param tag
	 *            NBTTag to write.
	 * @param file
	 *            File to write to.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static void writeFile(NBTCompound tag, File file) throws IOException {
		writeFile(tag, file, false);
	}

	/**
	 * Writes the given root NBTCompound to the given file as stringified NBT.
	 *
	 * @param tag
	 *            NBTTag to write.
	 * @param path
	 *            Path to write to.
	 * @param linebreak
	 *            Whether the SNBT file should be formated with line breaks or as a single line.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static void writeFile(NBTCompound tag, String path, boolean linebreak) throws IOException {
		writeFile(tag, new File(path), linebreak);
	}

	/**
	 * Writes the given root NBTCompound to the given file as stringified NBT.
	 *
	 * @param tag
	 *            NBTTag to write.
	 * @param file
	 *            File to write to.
	 * @param linebreak
	 *            Whether the SNBT file should be formated with line breaks or as a single line.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static void writeFile(NBTCompound tag, File file, boolean linebreak) throws IOException {
		if (!file.exists()) {
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}

			file.createNewFile();
		}

		OutputStream out = new FileOutputStream(file);

		writeTag(out, tag, linebreak);
		out.close();
	}

	/**
	 * Reads a stringified NBT tag.
	 *
	 * @param in
	 *            Input stream to read from.
	 * @return The read tag, or null if the tag is an end tag.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static NBTTag readTag(InputStream in) throws IOException {
		StringifiedNBTReader reader = new StringifiedNBTReader(in);
		NBTTag t = reader.readNextTag("");
		reader.close();
		return t;
	}

	/**
	 * Writes a stringified NBT tag.
	 *
	 * @param out
	 *            Output stream to write to.
	 * @param tag
	 *            NBTTag to write.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static void writeTag(OutputStream out, NBTTag tag) throws IOException {
		writeTag(out, tag, false);
	}

	/**
	 * Writes a stringified NBT tag.
	 *
	 * @param out
	 *            Output stream to write to.
	 * @param tag
	 *            NBTTag to write.
	 * @param linebreak
	 *            Whether the SNBT should be formated with line breaks or as a single line.
	 * @throws java.io.IOException
	 *             If an I/O error occurs.
	 */
	public static void writeTag(OutputStream out, NBTTag tag, boolean linebreak) throws IOException {
		StringifiedNBTWriter writer = new StringifiedNBTWriter(out);
		writer.writeTag(tag, linebreak);
		writer.close();
	}

	public static class StringifiedNBTReader extends PushbackReader {
		public StringifiedNBTReader(InputStream in) {
			super(new InputStreamReader(in), 32);
		}

		public NBTTag readNextTag(String name) throws IOException {
			skipWhitespace();
			if (lookAhead(0) == '{') {
				return readNBTCompound(name);
			} else if (lookAhead(0) == '[') {
				return readListOrArrayTag(name);
			} else {
				return readPrimitiveTag(name);
			}
		}

		public NBTTag readNBTCompound(String name) throws IOException {
			return parseTag(new NBTCompound(name));
		}

		private NBTTag readListOrArrayTag(String name) throws IOException {
			if (lookAhead(2) == ';') {
				switch (lookAhead(1)) {
					case 'B':
						// Byte array
						return parseTag(new NBTByteArray(name));
					case 'I':
						// Integer array
						return parseTag(new NBTIntArray(name));
					case 'L':
						// Long array
						return parseTag(new NBTLongArray(name));
					default:
						// Treat as list tag
						break;
				}
			}

			// This is a list tag
			return parseTag(new NBTList(name));
		}

		private NBTTag readPrimitiveTag(String name) throws IOException {
			String valueString = readNextSingleValueString(32);
			unread(valueString.toCharArray());
			return parseTag(getTagForStringifiedValue(name, valueString));
		}

		public String readNextSingleValueString() throws IOException {
			return readNextSingleValueString(Integer.MAX_VALUE);
		}

		// Used when expecting to unread to limit read to the length of the pushback buffer.
		public String readNextSingleValueString(int maxReadLenght) throws IOException {
			String valueString;
			if (lookAhead(0) == '\'' || lookAhead(0) == '\"') {
				char c = (char) read();
				valueString = c + readUntil(maxReadLenght, true, c);
			} else {
				valueString = readUntil(maxReadLenght, false, ',', '}', ']', '\r', '\n', '\t');
			}
			return valueString;
		}

		static final Pattern byteTagValuePattern = Pattern.compile("[-+]?\\d+[bB]");
		static final Pattern doubleTagValuePattern = Pattern.compile("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))[dD]");
		static final Pattern floatTagValuePattern = Pattern.compile("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))[fF]");
		static final Pattern intTagValuePattern = Pattern.compile("[-+]?\\d+");
		static final Pattern longTagValuePattern = Pattern.compile("[-+]?\\d+[lL]");
		static final Pattern shortTagValuePattern = Pattern.compile("[-+]?\\d+[sS]");

		private NBTTag getTagForStringifiedValue(String name, String stringifiedValue) {
			if (byteTagValuePattern.matcher(stringifiedValue).matches()) {
				// Byte
				return new NBTByte(name);
			} else if (doubleTagValuePattern.matcher(stringifiedValue).matches()) {
				// Double
				return new NBTDouble(name);
			} else if (floatTagValuePattern.matcher(stringifiedValue).matches()) {
				// Float
				return new NBTFloat(name);
			} else if (intTagValuePattern.matcher(stringifiedValue).matches()) {
				// Integer
				return new NBTInt(name);
			} else if (longTagValuePattern.matcher(stringifiedValue).matches()) {
				// Long
				return new NBTLong(name);
			} else if (shortTagValuePattern.matcher(stringifiedValue).matches()) {
				// Short
				return new NBTShort(name);
			}
			// String
			return new NBTString(name);
		}

		public NBTTag parseTag(NBTTag tag) throws IOException {
			tag.destringify(this);
			return tag;
		}

		public void skipWhitespace() throws IOException {
			char c;
			while ((c = (char) read()) != -1) {
				if (c == '\t' || c == '\r' || c == '\n' || c == ' ') {
					continue;
				} else {
					unread(c);
					return;
				}
			}
		}

		public char readSkipWhitespace() throws IOException {
			skipWhitespace();
			return (char) read();
		}

		public String readUntil(boolean includeEndChar, char... endChar) throws IOException {
			return readUntil(Integer.MAX_VALUE, includeEndChar, endChar);
		}

		// Used when expecting to unread to limit read to the length of the pushback buffer.
		public String readUntil(int maxReadLenght, boolean includeEndChar, char... endChar) throws IOException {
			StringBuilder sb = new StringBuilder();
			boolean escapeEnd = false;
			int reads = 0;
			char c;
			while (++reads < maxReadLenght && (c = (char) read()) != -1) {
				if (c == '\\') {
					sb.append(c);
					escapeEnd = true;
					continue;
				}
				if (!escapeEnd && matchesAny(c, endChar)) {
					if (includeEndChar) {
						sb.append(c);
					} else {
						unread(c);
					}
					break;
				}
				sb.append(c);
				escapeEnd = false;
			}
			return sb.toString();
		}

		public char lookAhead(int offset) throws IOException {
			char[] future = new char[offset + 1];
			read(future);
			unread(future);
			return future[offset];
		}

		public static boolean matchesAny(char c, char[] matchable) {
			for (char m : matchable) {
				if (c == m)
					return true;
			}
			return false;
		}
	}

	public static class StringifiedNBTWriter extends OutputStreamWriter {

		public StringifiedNBTWriter(OutputStream out) {
			super(out);
		}

		public void writeTag(NBTTag tag, boolean linebreak) throws IOException {
			writeTag(tag, linebreak, 0);
			flush();
		}

		public void writeTag(NBTTag tag, boolean linebreak, int depth) throws IOException {
			if (linebreak && depth > 0) {
				append('\n');
				indent(depth);
			}

			if (tag.getName() != null && !tag.getName().equals("")) {
				appendTagName(tag.getName());

				append(':');
				append(' ');
			}

			if (tag instanceof NBTCompound) {
				tag.stringify(this, linebreak, depth);
			} else if (tag instanceof NBTList) {
				tag.stringify(this, linebreak, depth);
			} else {
				tag.stringify(this, linebreak, depth);
			}
		}

		public static Pattern nonEscapedTagName = Pattern.compile("(?!\\d+)[\\w\\d]*");

		public void appendTagName(String tagName) throws IOException {
			if (!nonEscapedTagName.matcher(tagName).matches()) {
				append('"');
				append(tagName.replaceAll("\\\"", "\\\""));
				append('"');
			} else {
				append(tagName);
			}
		}

		public void indent(int depth) throws IOException {
			for (int i = 0; i < depth; i++) {
				append('\t');
			}
		}
	}
}