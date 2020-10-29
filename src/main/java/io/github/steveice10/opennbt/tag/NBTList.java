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

package io.github.steveice10.opennbt.tag;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import io.github.steveice10.opennbt.NBTRegistry;

public class NBTList extends NBTTag implements NBTParent, NBTIndexed {
	private Class<? extends NBTTag> type;
	private final List<NBTTag> list = Lists.newArrayList();

	/**
	 * Creates an empty list tag with the specified name and no defined type.
	 */
	public NBTList(String name) {
		super(name);
		this.type = null;
	}

	/**
	 * Creates an empty list tag with the specified name and type.
	 */
	public NBTList(String name, Class<? extends NBTTag> type) {
		this(name);
		this.type = type;
	}

	/**
	 * Creates a list tag with the specified name and value.
	 * The list tag's type will be set to that of the first tag being added, or null if the given list is empty.
	 * @throws IllegalArgumentException If all tags in the list are not of the same type.
	 */
	public NBTList(String name, List<NBTTag> value) throws IllegalArgumentException {
		this(name);
		for (NBTTag tag : value) {
			this.add(tag);
		}
	}

	/**
	 * @return The ListTag's element type, or null if the list does not yet have a defined type.
	 */
	@Override
	public Class<? extends NBTTag> getElementType() {
		return this.type;
	}

	@Override
	public boolean add(NBTTag tag) {
		return add(size(), tag);
	}
	
	/**
	 * Adds a tag to this list tag.
	 * If the list does not yet have a type, it will be set to the type of the tag being added.
	 *
	 * @param tag Tag to add. Should not be null.
	 * @return If the list was changed as a result.
	 * @throws IllegalArgumentException If the tag's type differs from the list tag's type.
	 */
	@Override
	public boolean add(int idx, NBTTag tag) throws IllegalArgumentException {
		if (tag == null) return false;
		checkOrAdoptType(tag);

		this.list.add(idx, tag);
		tag.setParent(this);
		return true;
	}
	
	@Override
	public NBTTag set(int idx, NBTTag tag) throws IllegalArgumentException {
		if (tag == null) return null;
		checkOrAdoptType(tag);
		
		NBTTag old = this.list.set(idx, tag);
		tag.setParent(this);
		if (old != null) {
			old.setParent(null);
		}
		return old;
	}

	private void checkOrAdoptType(NBTTag tag) {
		// If empty list, use this as tag type.
		if (this.type == null) {
			this.type = tag.getClass();
		} else if (tag.getClass() != this.type) {
			throw new IllegalArgumentException("Attempted to add an "+tag.getClass().getSimpleName()+" to a NBTList of type "+type.getSimpleName());
		}
	}

	/**
	 * Removes a tag from this list tag.
	 *
	 * @param tag Tag to remove.
	 * @return If the list contained the tag.
	 */
	@Override
	public boolean remove(NBTTag tag) {
		boolean b = this.list.remove(tag);
		if (b) {
			tag.setParent(null);
			if (this.isEmpty()) {
				this.type = null;
			}
		}
		return b;
	}
	
	public <T extends NBTTag> T remove(int index) {
		T t = (T) this.list.remove(index);
		if (t != null) {
			t.setParent(null);
			if (this.isEmpty()) {
				this.type = null;
			}
		}
		return t;
	}

	/**
	 * Gets the tag at the given index of this list tag.
	 *
	 * @param <T>   Type of tag to get
	 * @param index Index of the tag.
	 * @return The tag at the given index.
	 */
	@Override
	public <T extends NBTTag> T get(int index) {
		return (T) this.list.get(index);
	}

	/**
	 * Gets the number of tags in this list tag.
	 *
	 * @return The size of this list tag.
	 */
	@Override
	public int size() {
		return this.list.size();
	}
	
	@Override
	public boolean isEmpty() {
		return this.list.isEmpty();
	}
	
	@Override
	public void clear() {
		for (NBTTag tag : list) {
			tag.setParent(null);
		}
		this.list.clear();
		this.type = null;
	}
	
	public int indexOf(NBTTag tag) {
		// reimplemented for identity comparison
		// List::indexOf uses equals
		for (int i = 0; i < size(); i++) {
			if (get(i) == tag) return i;
		}
		return -1;
	}
	
	@Override
	public String stringValue() {
		return "["+Joiner.on(", ").join(list.stream().map(NBTTag::stringValue).iterator())+"]";
	}

	@Override
	public Iterator<NBTTag> iterator() {
		return Iterators.unmodifiableIterator(this.list.iterator());
	}

	@Override
	public void read(DataInput in) throws IOException {
		clear();

		int id = in.readUnsignedByte();
		if(id != 0) {
			this.type = NBTRegistry.classById(id);
			if (this.type == null) throw new IOException("Unknown tag ID in NBTList "+id);
		}

		int count = in.readInt();
		for (int i = 0; i < count; i++) {
			NBTTag tag = NBTRegistry.createInstance(id, "");
			tag.read(in);
			this.add(tag);
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		if (this.type == null) {
			out.writeByte(0);
		} else {
			int id = NBTRegistry.idForClass(this.type);
			if (id == -1) throw new IOException("NBTList contains unregistered tag class "+this.type.getSimpleName());
			out.writeByte(id);
		}

		out.writeInt(this.list.size());
		for (NBTTag tag : this.list) {
			tag.write(out);
		}
	}

	@Override
	protected boolean equalsChecked(NBTTag that) {
		return this.type == ((NBTList)that).type
				&& Objects.equal(this.list, ((NBTList)that).list);
	}

	@Override
	public int hashCode() {
		return this.list.hashCode();
	}

	@Override
	public String toString() {
		return "NBTList<"+(this.type == null ? "null" : this.type.getSimpleName())+">"+this.list;
	}
	
}
