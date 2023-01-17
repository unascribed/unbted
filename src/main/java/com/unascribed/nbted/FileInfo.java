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

import java.io.File;

public class FileInfo {

	public static final File STDIN = new File("/dev/stdin");
	
	public final File sourceFile;
	public final Compression compressionMethod;
	public final boolean compressionAutodetected;
	public final Endianness endianness;
	public final boolean isJson;
	
	public FileInfo(File sourceFile, Compression compressionMethod, boolean compressionAutodetected, Endianness endianness, boolean isJson) {
		this.sourceFile = sourceFile;
		this.compressionMethod = compressionMethod;
		this.compressionAutodetected = compressionAutodetected;
		this.endianness = endianness;
		this.isJson = isJson;
	}
	
}
