/*
 * Copyright (C) 2013-2017 Steveice10, 2018 - 2020 Una Thompson (unascribed)
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

package io.github.steveice10.opennbt.tag.array;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import io.github.steveice10.opennbt.tag.NBTTag;

public class NBTByteArray extends NBTArray {
	private byte[] value;

	public NBTByteArray(String name) {
		this(name, new byte[0]);
	}

	public NBTByteArray(String name, byte[] value) {
		super(name);
		this.value = value;
	}

	public byte[] getValue() {
		return this.value.clone();
	}

	public void setValue(byte[] value) {
		if (value == null) return;
		this.value = value.clone();
	}

	public byte getValue(int index) {
		return this.value[index];
	}

	public void setValue(int index, byte value) {
		this.value[index] = value;
	}
	
	@Override
	public String stringValue() {
		return Arrays.toString(value);
	}

	@Override
	public int length() {
		return this.value.length;
	}

	@Override
	public void read(DataInput in) throws IOException {
		this.value = new byte[in.readInt()];
		in.readFully(this.value);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.value.length);
		out.write(this.value);
	}

	@Override
	protected boolean equalsChecked(NBTTag that) {
		return Arrays.equals(this.value, ((NBTByteArray)that).value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.value);
	}

	@Override
	public String toString() {
		return "NBTByteArray"+Arrays.toString(this.value);
	}
	
}
