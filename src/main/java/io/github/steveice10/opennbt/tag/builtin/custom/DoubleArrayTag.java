/*
 * Copyright (C) 2013-2017 Steveice10
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

package io.github.steveice10.opennbt.tag.builtin.custom;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import io.github.steveice10.opennbt.tag.builtin.Tag;

/**
 * A tag containing a double array.
 */
public class DoubleArrayTag extends Tag implements Extension {
	private double[] value;

	/**
	 * Creates a tag with the specified name.
	 *
	 * @param name The name of the tag.
	 */
	public DoubleArrayTag(String name, Tag parent) {
		this(name, parent, new double[0]);
	}

	/**
	 * Creates a tag with the specified name.
	 *
	 * @param name  The name of the tag.
	 * @param value The value of the tag.
	 */
	public DoubleArrayTag(String name, Tag parent, double[] value) {
		super(name, parent);
		this.value = value;
	}

	@Override
	public double[] getValue() {
		return this.value.clone();
	}

	/**
	 * Sets the value of this tag.
	 *
	 * @param value New value of this tag.
	 */
	public void setValue(double[] value) {
		if(value == null) {
			return;
		}

		this.value = value.clone();
	}

	/**
	 * Gets a value in this tag's array.
	 *
	 * @param index Index of the value.
	 * @return The value at the given index.
	 */
	public double getValue(int index) {
		return this.value[index];
	}

	/**
	 * Sets a value in this tag's array.
	 *
	 * @param index Index of the value.
	 * @param value Value to set.
	 */
	public void setValue(int index, double value) {
		this.value[index] = value;
	}

	/**
	 * Gets the length of this tag's array.
	 *
	 * @return This tag's array length.
	 */
	public int length() {
		return this.value.length;
	}

	@Override
	public void read(DataInput in) throws IOException {
		this.value = new double[in.readInt()];
		for(int index = 0; index < this.value.length; index++) {
			this.value[index] = in.readDouble();
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.value.length);
		for(int index = 0; index < this.value.length; index++) {
			out.writeDouble(this.value[index]);
		}
	}
}
