package com.unascribed.nbted;

import java.io.File;

public class FileInfo {

	public static final File STDIN = new File("/dev/stdin");
	
	public final File sourceFile;
	public final Compression compressionMethod;
	public final boolean compressionAutodetected;
	public final Endianness endianness;
	
	public FileInfo(File sourceFile, Compression compressionMethod, boolean compressionAutodetected, Endianness endianness) {
		this.sourceFile = sourceFile;
		this.compressionMethod = compressionMethod;
		this.compressionAutodetected = compressionAutodetected;
		this.endianness = endianness;
	}
	
}
