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

package io.github.steveice10.opennbt.tag.builtin;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Represents an NBT tag.
 * <p>
 * All tags must have a constructor with a single string parameter for reading tags (can be any visibility).
 * Tags should also have setter methods specific to their value types.
 */
public abstract class Tag implements Cloneable {
    private String name;

    /**
     * Creates a tag with the specified name.
     *
     * @param name The name.
     */
    public Tag(String name) {
        this.name = name;
    }

    /**
     * Gets the name of this tag.
     *
     * @return The name of this tag.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Gets the value of this tag.
     *
     * @return The value of this tag.
     */
    public abstract Object getValue();

    /**
     * Reads this tag from an input stream.
     *
     * @param in Stream to write to.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public abstract void read(DataInput in) throws IOException;

    /**
     * Writes this tag to an output stream.
     *
     * @param out Stream to write to.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public abstract void write(DataOutput out) throws IOException;

    @Override
    public abstract Tag clone();

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Tag)) {
            return false;
        }

        Tag tag = (Tag) obj;
        if(!this.getName().equals(tag.getName())) {
            return false;
        }

        if(this.getValue() == null) {
            return tag.getValue() == null;
        } else if(tag.getValue() == null) {
            return false;
        }

        if(this.getValue().getClass().isArray() && tag.getValue().getClass().isArray()) {
            int length = Array.getLength(this.getValue());
            if(Array.getLength(tag.getValue()) != length) {
                return false;
            }

            for(int index = 0; index < length; index++) {
                Object o = Array.get(this.getValue(), index);
                Object other = Array.get(tag.getValue(), index);
                if(o == null && other != null || o != null && !o.equals(other)) {
                    return false;
                }
            }

            return true;
        }

        return this.getValue().equals(tag.getValue());
    }

    @Override
    public String toString() {
        String name = this.getName() != null && !this.getName().equals("") ? "(" + this.getName() + ")" : "";
        String value = "";
        if(this.getValue() != null) {
            value = this.getValue().toString();
            if(this.getValue().getClass().isArray()) {
                StringBuilder build = new StringBuilder();
                build.append("[");
                for(int index = 0; index < Array.getLength(this.getValue()); index++) {
                    if(index > 0) {
                        build.append(", ");
                    }

                    build.append(Array.get(this.getValue(), index));
                }

                build.append("]");
                value = build.toString();
            }
        }

        return this.getClass().getSimpleName() + name + " { " + value + " }";
    }
}
