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

import java.io.IOException;
import java.io.UncheckedIOException;

import com.google.common.collect.ImmutableList;

public interface Ansi {
	ImmutableList<Integer> getCodes();

	public static final class Utils {
		public static String toString(Ansi... codes) {
			StringBuilder sb = new StringBuilder();
			toAppendableUnchecked(sb, codes);
			return sb.toString();
		}
	
		public static void toAppendableUnchecked(Appendable a, Ansi... codes) {
			try {
				toAppendable(a, codes);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	
		public static void toAppendable(Appendable a, Ansi... codes) throws IOException {
			a.append("\u001B[");
			boolean first = true;
			for (Ansi code : codes) {
				for (Integer i : code.getCodes()) {
					if (!first) {
						a.append(';');
					}
					a.append(i.toString());
					first = false;
				}
			}
			a.append('m');
		}
	}
}
