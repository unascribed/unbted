package io.github.steveice10.opennbt.tag;

public abstract class NBTParent extends NBTTag implements Iterable<NBTTag> {

	public NBTParent(String name) {
		super(name);
	}
	
	public abstract boolean remove(NBTTag tag);
	public abstract int size();
	
}
