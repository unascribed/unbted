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

package io.github.steveice10.opennbt.tag;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import com.google.common.base.Objects;

import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTReader;
import io.github.steveice10.opennbt.SNBTIO.StringifiedNBTWriter;

public class NBTString extends NBTTag implements Comparable<NBTString> {
	private String value;

	public NBTString(String name) {
		this(name, "");
	}

	public NBTString(String name, String value) {
		super(name);
		this.value = value;
	}

	@Override
	public String stringValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public void read(DataInput in) throws IOException {
		this.value = in.readUTF();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(this.value);
	}

	@Override
	public void destringify(StringifiedNBTReader in) throws IOException {
		String s = in.readNextSingleValueString();
		if (s.charAt(0) == '"') {
			value = s.substring(1, s.length() - 1).replaceAll("\\\\\"", "\"");
		} else if (s.charAt(0) == '\'') {
			value = s.substring(1, s.length() - 1).replaceAll("\\\\\'", "'");
		} else {
			value = s;
		}
	}

	@Override
	public void stringify(StringifiedNBTWriter out, boolean linebreak, int depth) throws IOException {
		if (value.matches("(?!\\d+)[\\w\\d]*")) {
			out.append(value);
			return;
		}
		if (value.contains("\"")) {
			if (value.contains("'")) {
				StringBuilder sb = new StringBuilder("\"");
				sb.append(value.replaceAll("\"", "\\\\\""));
				sb.append("\"");
				out.append(sb.toString());
				return;
			}
			StringBuilder sb = new StringBuilder("'");
			sb.append(value);
			sb.append("'");
			out.append(sb.toString());
			return;
		}
		StringBuilder sb = new StringBuilder("\"");
		sb.append(value);
		sb.append("\"");
		out.append(sb.toString());
	}

	@Override
	public int compareTo(NBTString that) {
		return this.value.compareTo(that.value);
	}
	
	@Override
	protected boolean equalsChecked(NBTTag that) {
		return Objects.equal(this.value, ((NBTString)that).value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return "NBTString[value="+this.value+"]";
	}
	
}
