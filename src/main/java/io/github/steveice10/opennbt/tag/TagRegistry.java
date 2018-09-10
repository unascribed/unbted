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

package io.github.steveice10.opennbt.tag;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import io.github.steveice10.opennbt.tag.builtin.ByteArrayTag;
import io.github.steveice10.opennbt.tag.builtin.ByteTag;
import io.github.steveice10.opennbt.tag.builtin.CompoundTag;
import io.github.steveice10.opennbt.tag.builtin.DoubleTag;
import io.github.steveice10.opennbt.tag.builtin.FloatTag;
import io.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import io.github.steveice10.opennbt.tag.builtin.IntTag;
import io.github.steveice10.opennbt.tag.builtin.ListTag;
import io.github.steveice10.opennbt.tag.builtin.LongArrayTag;
import io.github.steveice10.opennbt.tag.builtin.LongTag;
import io.github.steveice10.opennbt.tag.builtin.ShortTag;
import io.github.steveice10.opennbt.tag.builtin.StringTag;
import io.github.steveice10.opennbt.tag.builtin.Tag;
import io.github.steveice10.opennbt.tag.builtin.custom.DoubleArrayTag;
import io.github.steveice10.opennbt.tag.builtin.custom.Extension;
import io.github.steveice10.opennbt.tag.builtin.custom.FloatArrayTag;
import io.github.steveice10.opennbt.tag.builtin.custom.ShortArrayTag;
import io.github.steveice10.opennbt.tag.builtin.custom.StringArrayTag;

/**
 * A registry containing different tag classes.
 */
public class TagRegistry {
    private static final Map<Integer, Class<? extends Tag>> idToTag = new HashMap<>();
    private static final Map<Class<? extends Tag>, Integer> tagToId = new HashMap<>();
    
    public static boolean NEWTAGS_ENABLED = true;
    public static boolean EXTENSIONS_ENABLED = false;

    static {
        register(1, ByteTag.class);
        register(2, ShortTag.class);
        register(3, IntTag.class);
        register(4, LongTag.class);
        register(5, FloatTag.class);
        register(6, DoubleTag.class);
        register(7, ByteArrayTag.class);
        register(8, StringTag.class);
        register(9, ListTag.class);
        register(10, CompoundTag.class);
        register(11, IntArrayTag.class);
        register(12, LongArrayTag.class);

        register(60, DoubleArrayTag.class);
        register(61, FloatArrayTag.class);
        register(65, ShortArrayTag.class);
        register(66, StringArrayTag.class);
    }

    /**
     * Registers a tag class.
     *
     * @param id  ID of the tag.
     * @param tag Tag class to register.
     * @throws TagRegisterException If an error occurs while registering the tag.
     */
    public static void register(int id, Class<? extends Tag> tag) {
        if(idToTag.containsKey(id)) {
            throw new IllegalArgumentException("Tag ID \"" + id + "\" is already in use.");
        }

        if(tagToId.containsKey(tag)) {
            throw new IllegalArgumentException("Tag \"" + tag.getSimpleName() + "\" is already registered.");
        }

        idToTag.put(id, tag);
        tagToId.put(tag, id);
    }

    /**
     * Gets the tag class with the given id.
     *
     * @param id Id of the tag.
     * @return The tag class with the given id, or null if it cannot be found.
     */
    public static Class<? extends Tag> getClassFor(int id) {
    	if (id >= 60 && !EXTENSIONS_ENABLED) return null;
    	// replace old OpenNBT extended long[] with new vanilla long[]
    	if (EXTENSIONS_ENABLED && id == 62) id = 12;
    	if (id == 11 || id == 12 && !NEWTAGS_ENABLED) return null;
        if(!idToTag.containsKey(id)) {
            return null;
        }

        return idToTag.get(id);
    }

    /**
     * Gets the id of the given tag class.
     *
     * @param clazz The tag class to get the id of.
     * @return The id of the given tag class, or -1 if it cannot be found.
     */
    public static int getIdFor(Class<? extends Tag> clazz) {
    	if (Extension.class.isAssignableFrom(clazz) && !EXTENSIONS_ENABLED) return -1;
    	if ((clazz == IntArrayTag.class || clazz == LongArrayTag.class) && !NEWTAGS_ENABLED) return -1;
        if(!tagToId.containsKey(clazz)) {
            return -1;
        }

        return tagToId.get(clazz);
    }

    /**
     * Creates an instance of the tag with the given id, using the String constructor.
     *
     * @param id      Id of the tag.
     * @param tagName Name to give the tag.
     * @return The created tag.
     * @throws TagCreateException If an error occurs while creating the tag.
     */
    public static Tag createInstance(int id, String tagName) {
        Class<? extends Tag> clazz = idToTag.get(id);
        if(clazz == null) {
            throw new IllegalArgumentException("Could not find tag with ID \"" + id + "\".");
        }

        try {
            Constructor<? extends Tag> constructor = clazz.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(tagName);
        } catch(Exception e) {
            throw new RuntimeException("Failed to create instance of tag \"" + clazz.getSimpleName() + "\".", e);
        }
    }
}
