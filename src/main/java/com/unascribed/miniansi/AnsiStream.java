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

package com.unascribed.miniansi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class AnsiStream extends PrintStream {

	public AnsiStream(File file, String charsetName) throws FileNotFoundException, UnsupportedEncodingException {
		super(file, charsetName);
	}

	public AnsiStream(File file) throws FileNotFoundException {
		super(file);
	}

	public AnsiStream(OutputStream out, boolean autoFlush, String charsetName) throws UnsupportedEncodingException {
		super(out, autoFlush, charsetName);
	}

	public AnsiStream(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public AnsiStream(OutputStream out) {
		super(out);
	}

	public AnsiStream(String fileName, String charsetName) throws FileNotFoundException, UnsupportedEncodingException {
		super(fileName, charsetName);
	}

	public AnsiStream(String fileName) throws FileNotFoundException {
		super(fileName);
	}

	public void print(char[] chars, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(chars);
	}

	public void print(char c, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(c);
	}

	public void print(double d, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(d);
	}

	public void print(float f, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(f);
	}

	public void print(int i, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(i);
	}

	public void print(long l, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(l);
	}

	public void print(Object o, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(o);
	}

	public synchronized void print(String str, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(str);
	}

	public void print(boolean b, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.print(b);
	}

	public void print(Ansi... codes) {
		if (codes.length == 0) return;
		Ansi.Utils.toAppendableUnchecked(this, codes);
	}

	public void println(Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println();
	}

	public void println(char[] chars, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(chars);
	}

	public void println(char c, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(c);
	}

	public void println(double d, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(d);
	}

	public void println(float f, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(f);
	}

	public void println(int i, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(i);
	}

	public void println(long l, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(l);
	}

	public void println(Object o, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(o);
	}

	public synchronized void println(String str, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(str);
	}

	public void println(boolean b, Ansi... codes) {
		Ansi.Utils.toAppendableUnchecked(this, codes);
		super.println(b);
	}

	public void printPadded(String str, Ansi... codes) {
		print(codes);
		print(" ");
		print(str);
		print(" ");
	}

	public void cursorRight(int i) {
		print("\u001B[");
		print(i);
		print("C");
	}
	
	public void cursorUp(int i) {
		print("\u001B[");
		print(i);
		print("A");
	}
	
	public void cursorDown(int i) {
		print("\u001B[");
		print(i);
		print("B");
	}
	
	public void cursorLeft(int i) {
		print("\u001B[");
		print(i);
		print("D");
	}

	public void reset() {
		print(AnsiCode.RESET);
	}

}
