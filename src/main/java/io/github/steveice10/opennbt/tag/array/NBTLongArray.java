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
import java.util.Iterator;

import com.google.common.collect.AbstractIterator;
import com.google.common.primitives.Longs;

import io.github.steveice10.opennbt.tag.NBTParent;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.support.NBTFakeLong;
import io.github.steveice10.opennbt.tag.number.NBTLong;

public class NBTLongArray extends NBTArray implements NBTParent {
	private long[] value;

	public NBTLongArray(String name) {
		this(name, new long[0]);
	}

	public NBTLongArray(String name, long[] value) {
		super(name);
		this.value = value;
	}

	public long[] getValue() {
		return this.value.clone();
	}

	public void setValue(long[] value) {
		if (value == null) return;
		this.value = value.clone();
	}

	public long getValue(int index) {
		return this.value[index];
	}

	public void setValue(int index, long value) {
		this.value[index] = value;
	}
	
	@Override
	public String stringValue() {
		return Arrays.toString(value);
	}

	@Override
	public void read(DataInput in) throws IOException {
		this.value = new long[in.readInt()];
		for (int i = 0; i < this.value.length; i++) {
			this.value[i] = in.readLong();
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.value.length);
		for (int i = 0; i < this.value.length; i++) {
			out.writeLong(this.value[i]);
		}
	}
	
	@Override
	protected boolean equalsChecked(NBTTag that) {
		return Arrays.equals(this.value, ((NBTLongArray)that).value);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.value);
	}

	@Override
	public String toString() {
		return "NBTLongArray"+Arrays.toString(this.value);
	}
	
	@Override
	public Class<? extends NBTTag> getElementType() {
		return NBTLong.class;
	}

	@Override
	public Iterator<NBTTag> iterator() {
		return new AbstractIterator<NBTTag>() {
			private int idx = -1;
			
			@Override
			protected NBTTag computeNext() {
				idx++;
				if (idx >= value.length) return endOfData();
				return new NBTFakeLong(NBTLongArray.this, idx);
			}
		};
	}
	
	@Override
	public NBTFakeLong get(int idx) {
		if (idx < 0 || idx >= value.length) throw new ArrayIndexOutOfBoundsException(idx);
		return new NBTFakeLong(this, idx);
	}
	
	@Override
	public boolean add(int idx, NBTTag tag) {
		if (tag instanceof NBTLong) {
			long[] lhs = Arrays.copyOfRange(value, 0, idx);
			long[] mid = new long[] {((NBTLong) tag).longValue()};
			long[] rhs = Arrays.copyOfRange(value, idx, value.length);
			value = Longs.concat(lhs, mid, rhs);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean add(NBTTag tag) {
		if (tag instanceof NBTLong) {
			value = Arrays.copyOf(value, value.length+1);
			value[value.length-1] = ((NBTLong) tag).longValue();
			return true;
		}
		return false;
	}
	
	@Override
	public NBTTag set(int idx, NBTTag tag) {
		if (tag instanceof NBTLong) {
			long orig = value[idx];
			value[idx] = ((NBTLong) tag).longValue();
			return new NBTLong("", orig);
		}
		throw new ClassCastException(tag.getClass().getSimpleName()+" is not NBTLong");
	}

	@Override
	public boolean remove(NBTTag tag) {
		if (tag instanceof NBTFakeLong) {
			NBTFakeLong nfb = (NBTFakeLong)tag;
			if (nfb.getParent() == this) {
				long[] lhs = Arrays.copyOfRange(value, 0, nfb.getIndex());
				long[] rhs = Arrays.copyOfRange(value, nfb.getIndex()+1, value.length);
				value = Longs.concat(lhs, rhs);
				return true;
			}
		}
		return false;
	}

	@Override
	public int size() {
		return value.length;
	}

	@Override
	public boolean isEmpty() {
		return value.length == 0;
	}

	@Override
	public void clear() {
		value = new long[0];
	}
	
}
