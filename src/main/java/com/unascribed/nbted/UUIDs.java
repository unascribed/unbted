/*
 * unbted - Una's NBT Editor
 * Copyright (C) 2018 - 2023 Una Thompson (unascribed)
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

import java.util.UUID;

public class UUIDs {

	public static UUID fromIntArray(int[] arr) {
		return new UUID(
				(long)arr[0] << 32 | arr[1] & 0xFFFFFFFFL,
				(long)arr[2] << 32 | arr[3] & 0xFFFFFFFFL
			);
	}
	
	public static int[] toIntArray(UUID id) {
		long msb = id.getMostSignificantBits();
		long lsb = id.getLeastSignificantBits();
		return new int[] {
			(int)((msb >> 32)&0xFFFFFFFFL),
			(int)(msb&0xFFFFFFFFL),
			(int)((lsb >> 32)&0xFFFFFFFFL),
			(int)(lsb&0xFFFFFFFFL)
		};
	}
	
}
