package io.github.steveice10.opennbt.tag.number;

import io.github.steveice10.opennbt.tag.NBTTag;

public abstract class NBTNumber extends NBTTag {

	public NBTNumber(String name) {
		super(name);
	}
	
	public abstract Number numberValue();
	
	public abstract byte byteValue();
	public abstract short shortValue();
	public abstract int intValue();
	public abstract long longValue();
	public abstract float floatValue();
	public abstract double doubleValue();
	public boolean booleanValue() { return intValue() != 0; }
	
}
