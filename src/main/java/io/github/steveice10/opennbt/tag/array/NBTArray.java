package io.github.steveice10.opennbt.tag.array;

import io.github.steveice10.opennbt.tag.NBTTag;

public abstract class NBTArray extends NBTTag {

	public NBTArray(String name) {
		super(name);
	}
	
	public abstract int length();

}
