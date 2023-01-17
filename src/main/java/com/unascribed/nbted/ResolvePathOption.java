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

public enum ResolvePathOption {
	/**
	 * Return null instead of throwing errors.
	 */
	NO_ERROR,
	/**
	 * If the resolved tag is not an NBTParent, throw an error.
	 */
	PARENTS_ONLY,
	/**
	 * Automatically create any required intermediate NBTCompounds.
	 */
	CREATE_PARENTS,
	/**
	 * Return a null leaf on an out-of-bounds list index instead of
	 * erroring.
	 */
	SOFT_IOOBE,
}
