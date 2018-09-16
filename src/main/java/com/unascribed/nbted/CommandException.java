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

public class CommandException extends RuntimeException {

	// "Exit codes" for use by scripts, if those ever get implemented.
	// Not exposed in the UI at the time of writing.
	
	/**
	 * The command succeeded. Do not use. This is implied by no
	 * exception being thrown.
	 */
	@Deprecated
	public static final int VALUE_SUCCESS = 0;
	/**
	 * A non-specific error occurred.
	 */
	public static final int VALUE_GENERAL_ERROR = 1;
	/**
	 * The argument syntax was wrong. No possible change in environment
	 * would cause this command to succeed.
	 */
	public static final int VALUE_BAD_USAGE = 2;
	/**
	 * A tag specified in an argument or option was not found.
	 */
	public static final int VALUE_TAG_NOT_FOUND = 3;
	/**
	 * A tag specified in an argument or option already existed, and the
	 * command as invoked is refusing to destructively overwrite this
	 * tag.
	 */
	public static final int VALUE_WONT_OVERWRITE = 4;
	
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_1 = 20;
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_2 = 21;
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_3 = 22;
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_4 = 23;
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_5 = 24;
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_6 = 25;
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_7 = 26;
	/**
	 * Command-specific error.
	 */
	public static final int VALUE_CMDSPECIFIC_8 = 27;
	
	/**
	 * A required feature has not yet been implemented.
	 */
	public static final int VALUE_NYI = 125;
	
	private final int value;
	
	public CommandException(int value, String message, Throwable cause) {
		super(message, cause);
		this.value = value;
	}

	public CommandException(int value, String message) {
		super(message);
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}

}
