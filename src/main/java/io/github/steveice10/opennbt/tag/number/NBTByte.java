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

package io.github.steveice10.opennbt.tag.number;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import io.github.steveice10.opennbt.tag.NBTTag;

public class NBTByte extends NBTNumber implements Comparable<NBTByte> {
	private byte value;

	public NBTByte(String name) {
		this(name, (byte) 0);
	}

	public NBTByte(String name, byte value) {
		super(name);
		this.value = value;
	}

	protected byte getValue() {
		return value;
	}
	
	@Override
	public Byte numberValue() {
		return this.getValue();
	}
	
	@Override public byte byteValue() { return this.getValue(); }
	@Override public short shortValue() { return this.getValue(); }
	@Override public int intValue() { return this.getValue(); }
	@Override public long longValue() { return this.getValue(); }
	@Override public float floatValue() { return this.getValue(); }
	@Override public double doubleValue() { return this.getValue(); }
	@Override public String stringValue() { return Byte.toString(this.getValue()); }
	
	public void setValue(byte value) {
		this.value = value;
	}

	@Override
	public void read(DataInput in) throws IOException {
		this.value = in.readByte();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeByte(this.getValue());
	}

	@Override
	public int compareTo(NBTByte that) {
		return Byte.compare(this.getValue(), that.getValue());
	}
	
	@Override
	protected boolean equalsChecked(NBTTag that) {
		return this.getValue() == ((NBTByte)that).getValue();
	}

	@Override
	public int hashCode() {
		return Byte.hashCode(getValue());
	}

	@Override
	public String toString() {
		return "NBTByte[value="+getValue()+"]";
	}

}
