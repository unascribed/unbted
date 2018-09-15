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

package io.github.steveice10.opennbt.tag;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * All tags must have a constructor that accepts (String, NBTParent) for
 * reflective construction during file load.
 */
public abstract class NBTTag {
	private String name;
	private NBTParent parent;

	public NBTTag(String name) {
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}
	
	public final NBTParent getParent() {
		return parent;
	}
	
	protected final void setParent(NBTParent parent) {
		if (this.parent != null && parent != null && parent != this.parent) {
			throw new IllegalStateException("Tag already has a parent, it must be removed from its old parent first");
		}
		this.parent = parent;
	}
	
	public final void removeFromParent() {
		if (parent != null) {
			parent.remove(this);
		}
	}
	
	public abstract String stringValue();

	public abstract void read(DataInput in) throws IOException;
	public abstract void write(DataOutput out) throws IOException;

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (obj.getClass() != this.getClass()) return false;
		return equalsChecked((NBTTag)obj);
	}
	
	protected abstract boolean equalsChecked(NBTTag that);
	@Override
	public abstract int hashCode();
	@Override
	public abstract String toString();
	
}
