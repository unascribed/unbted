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

package io.github.steveice10.opennbt;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import io.github.steveice10.opennbt.tag.NBTCompound;
import io.github.steveice10.opennbt.tag.NBTList;
import io.github.steveice10.opennbt.tag.NBTString;
import io.github.steveice10.opennbt.tag.NBTTag;
import io.github.steveice10.opennbt.tag.array.NBTByteArray;
import io.github.steveice10.opennbt.tag.array.NBTIntArray;
import io.github.steveice10.opennbt.tag.array.NBTLongArray;
import io.github.steveice10.opennbt.tag.number.NBTByte;
import io.github.steveice10.opennbt.tag.number.NBTDouble;
import io.github.steveice10.opennbt.tag.number.NBTFloat;
import io.github.steveice10.opennbt.tag.number.NBTInt;
import io.github.steveice10.opennbt.tag.number.NBTLong;
import io.github.steveice10.opennbt.tag.number.NBTShort;

public class NBTRegistry {
	private static final BiMap<Integer, Class<? extends NBTTag>> byId = HashBiMap.create();
	private static final BiMap<String, Class<? extends NBTTag>> byTypeName = HashBiMap.create();
	private static final Map<Class<? extends NBTTag>, Constructor<? extends NBTTag>> constructors = Maps.newHashMap();

	static {
		register( 1, "byte", NBTByte.class);
		register( 2, "short", NBTShort.class);
		register( 3, "int", NBTInt.class);
		register( 4, "long", NBTLong.class);
		register( 5, "float", NBTFloat.class);
		register( 6, "double", NBTDouble.class);
		register( 7, "byte-array", NBTByteArray.class);
		register( 8, "string", NBTString.class);
		register( 9, "list", NBTList.class);
		register(10, "compound", NBTCompound.class);
		register(11, "int-array", NBTIntArray.class);
		register(12, "long-array", NBTLongArray.class);
	}

	private static void register(int id, String typeName, Class<? extends NBTTag> clazz) {
		if (byId.containsValue(clazz) || byTypeName.containsValue(clazz))
			throw new IllegalArgumentException(clazz.getSimpleName()+" already registered");
		
		if (byId.containsKey(id)) throw new IllegalArgumentException("Tag ID "+id+" already exists");
		if (byTypeName.containsKey(typeName)) throw new IllegalArgumentException("Tag type name "+typeName+" already exists");
		byId.put(id, clazz);
		byTypeName.put(typeName, clazz);
		try {
			Constructor<? extends NBTTag> c = clazz.getDeclaredConstructor(String.class);
			c.setAccessible(true);
			constructors.put(clazz, c);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to register "+clazz.getSimpleName()+" - is there a constructor taking a String?", e);
		}
	}

	/**
	 * @return The tag class with the given id, or null if it cannot be found.
	 */
	public static Class<? extends NBTTag> classById(int id) {
		if (!byId.containsKey(id)) return null;
		return byId.get(id);
	}

	/**
	 * @return The id of the given tag class, or -1 if it cannot be found.
	 */
	public static int idForClass(Class<? extends NBTTag> clazz) {
		if (!byId.inverse().containsKey(clazz)) return -1;
		return byId.inverse().get(clazz);
	}
	
	public static BiMap<Integer, Class<? extends NBTTag>> allById() {
		return Maps.unmodifiableBiMap(byId);
	}
	
	/**
	 * @return The tag class with the given type name, or null if it cannot be found.
	 */
	public static Class<? extends NBTTag> classByTypeName(String typeName) {
		if (!byTypeName.containsKey(typeName)) return null;
		return byTypeName.get(typeName);
	}

	/**
	 * @return The type name of the given tag class, or null if it cannot be found.
	 */
	public static String typeNameFromClass(Class<? extends NBTTag> clazz) {
		if (!byTypeName.inverse().containsKey(clazz)) return null;
		return byTypeName.inverse().get(clazz);
	}
	
	public static String typeNameForTag(NBTTag tag) {
		if (tag == null) return "null";
		return typeNameFromClass(tag.getClass());
	}
	
	public static BiMap<String, Class<? extends NBTTag>> allByTypeName() {
		return Maps.unmodifiableBiMap(byTypeName);
	}

	/**
	 * Creates an instance of the tag with the given id, using the String constructor.
	 *
	 * @param id	  Id of the tag.
	 * @param tagName Name to give the tag.
	 * @return The created tag.
	 * @throws IOException If an error occurs while creating the tag.
	 */
	public static NBTTag createInstance(int id, String tagName) throws IOException {
		Class<? extends NBTTag> clazz = classById(id);
		if (clazz == null) throw new IOException("Could not find tag with ID "+id);
		return createInstance(clazz, tagName);
	}
	
	public static NBTTag createInstance(Class<? extends NBTTag> clazz, String tagName) throws IOException {
		try {
			return constructors.get(clazz).newInstance(tagName);
		} catch (Throwable e) {
			throw new IOException("Failed to create instance of tag "+clazz.getSimpleName(), e);
		}
	}
	
}
