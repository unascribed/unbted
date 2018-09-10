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

import com.google.common.collect.ImmutableList;

public final class AnsiGroup implements Ansi {
	private final AnsiCode[] codes;
	public AnsiGroup(AnsiCode... codes) {
		this.codes = codes;
	}
	@Override
	public String toString() {
		return Ansi.Utils.toString(codes);
	}
	@Override
	public ImmutableList<Integer> getCodes() {
		ImmutableList.Builder<Integer> builder = ImmutableList.builder();
		for (AnsiCode code : codes) {
			builder.addAll(code.getCodes());
		}
		return builder.build();
	}
}
