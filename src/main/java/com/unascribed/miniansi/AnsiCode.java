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

public enum AnsiCode implements Ansi {
	FG_BLACK(30),
	FG_RED(31),
	FG_GREEN(32),
	FG_YELLOW(33),
	FG_BLUE(34),
	FG_MAGENTA(35),
	FG_CYAN(36),
	FG_WHITE(37),
	FG_DEFAULT(39),
	
	FG_BLACK_INTENSE(90),
	FG_RED_INTENSE(91),
	FG_GREEN_INTENSE(92),
	FG_YELLOW_INTENSE(93),
	FG_BLUE_INTENSE(94),
	FG_MAGENTA_INTENSE(95),
	FG_CYAN_INTENSE(96),
	FG_WHITE_INTENSE(97),
	
	BG_BLACK(40),
	BG_RED(41),
	BG_GREEN(42),
	BG_YELLOW(43),
	BG_BLUE(44),
	BG_MAGENTA(45),
	BG_CYAN(46),
	BG_WHITE(47),
	BG_DEFAULT(49),
	
	BG_BLACK_INTENSE(100),
	BG_RED_INTENSE(101),
	BG_GREEN_INTENSE(102),
	BG_YELLOW_INTENSE(103),
	BG_BLUE_INTENSE(104),
	BG_MAGENTA_INTENSE(105),
	BG_CYAN_INTENSE(106),
	BG_WHITE_INTENSE(107),
	
	BOLD(1),
	UNDERLINE(4),
	
	LEFT_SIDE_LINE(62),
	RIGHT_SIDE_LINE(60),
	
	NEGATIVE(7),
	POSITIVE(27),
	
	BLINK(5),
	
	RESET(0),
	;
	private final ImmutableList<Integer> codes;
	private final String alone;
	AnsiCode(Integer... codes) {
		this.codes = ImmutableList.copyOf(codes);
		this.alone = Ansi.Utils.toString(this);
	}
	@Override
	public ImmutableList<Integer> getCodes() {
		return codes;
	}
	@Override
	public String toString() {
		return alone;
	}
}
