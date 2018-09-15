/*
 * unbted - Una's NBT Editor
 * Copyright (C) 2018 Una Thompson (unascribed)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.unascribed.nbted;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.google.common.base.Charsets;

import io.github.steveice10.opennbt.NBTIO.LittleEndianDataInputStream;
import io.github.steveice10.opennbt.NBTIO.LittleEndianDataOutputStream;

public enum Endianness {
	BIG,
	LITTLE,
	ZZAZZ;
	
	public DataInput wrap(InputStream in) {
		switch (this) {
			case BIG: return new DataInputStream(in);
			case LITTLE: return new LittleEndianDataInputStream(in);
			case ZZAZZ: return new DataInputStream(new XORInputStream(in, EGG_NOISE));
			default: throw new AssertionError("missing case for "+this);
		}
	}
	
	public DataOutput wrap(OutputStream out) {
		switch (this) {
			case BIG: return new DataOutputStream(out);
			case LITTLE: return new LittleEndianDataOutputStream(out);
			case ZZAZZ: return new DataOutputStream(new XOROutputStream(out, EGG_NOISE));
			default: throw new AssertionError("missing case for "+this);
		}
	}
	
	@Override
	public String toString() {
		switch (this) {
			case BIG: return "Big (Java Edition)";
			case LITTLE: return "Little (Legacy Pocket Edition)";
			case ZZAZZ: return "ZZAZZAAZZ (ZZAAZZAAZZAAZZAAZZAAZZAAZZAAZZ)";
			default: throw new AssertionError("missing case for "+this);
		}
	}

	private static final byte[] EGG_NOISE = "ZZAZZAAZZAAZZZAAZZZAZAZZAZAZAZZAAZZAAZAZAZAZZAZAZAZAZAZAZZAAZZAAZZAAZZAAAZAZAZAAZZAAZZAAZAZZAAZZAAZZAZAZAZZAZZAZZAZZAZZZZZAAZAZAZAZAZAZAZ".getBytes(Charsets.UTF_8);
	
	private static class XORInputStream extends FilterInputStream {
		private int counter = 0;
		private final byte[] key;
		
		protected XORInputStream(InputStream in, byte[] key) {
			super(in);
			this.key = key;
		}
		
		@Override
		public int read() throws IOException {
			int b = in.read();
			if (b == -1) return -1;
			b = (byte)(b^key[counter])&0xFF;
			counter = (counter+1)%key.length;
			return b;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			len = in.read(b, off, len);
			for (int i = 0; i < len; i++) {
				b[off+i] = (byte) (b[off+i]^key[counter]);
				counter = (counter+1)%key.length;
			}
			return len;
		}
		
	}
	
	private static class XOROutputStream extends FilterOutputStream {
		private int counter = 0;
		private final byte[] key;
		
		protected XOROutputStream(OutputStream out, byte[] key) {
			super(out);
			this.key = key;
		}
		
		@Override
		public void write(int b) throws IOException {
			out.write((byte)(b^key[counter])&0xFF);
			counter = (counter+1)%key.length;
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			byte[] copy = Arrays.copyOfRange(b, off, off+len);
			for (int i = 0; i < copy.length; i++) {
				copy[i] = (byte) (copy[i]^key[counter]);
				counter = (counter+1)%key.length;
			}
			out.write(copy);
		}
		
	}
	
}
