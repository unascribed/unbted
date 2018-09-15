/*
 * Copyright (C) 2013-2017 Steveice10, 2018 Una Thompson (unascribed)
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

public final class NBTDouble extends NBTNumber implements Comparable<NBTDouble> {
	private double value;

	public NBTDouble(String name) {
		this(name, 0);
	}

	public NBTDouble(String name, double value) {
		super(name);
		this.value = value;
	}

	@Override
	public Double numberValue() {
		return this.value;
	}
	
	@Override public byte byteValue() { return (byte)this.value; }
	@Override public short shortValue() { return (short)this.value; }
	@Override public int intValue() { return (int)this.value; }
	@Override public long longValue() { return (long)this.value; }
	@Override public float floatValue() { return (float)this.value; }
	@Override public double doubleValue() { return this.value; }
	@Override public String stringValue() { return Double.toString(this.value); }

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public void read(DataInput in) throws IOException {
		this.value = in.readDouble();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeDouble(this.value);
	}
	
	@Override
	public int compareTo(NBTDouble that) {
		return Double.compare(this.value, that.value);
	}
	
	@Override
	protected boolean equalsChecked(NBTTag that) {
		return this.value == ((NBTDouble)that).value;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public String toString() {
		return "NBTDouble[value="+value+"]";
	}
	
}
