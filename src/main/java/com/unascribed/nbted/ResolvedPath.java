package com.unascribed.nbted;

import io.github.steveice10.opennbt.NBTRegistry;
import io.github.steveice10.opennbt.tag.NBTParent;
import io.github.steveice10.opennbt.tag.NBTTag;

public class ResolvedPath {

	public final NBTParent immediateParent;
	public final NBTTag leaf;
	public final String parentPath;
	public final String canonicalPath;
	
	public ResolvedPath(NBTParent immediateParent, NBTTag leaf, String parentPath, String canonicalPath) {
		this.immediateParent = immediateParent;
		this.leaf = leaf;
		this.parentPath = parentPath;
		this.canonicalPath = canonicalPath;
	}
	
	@Override
	public String toString() {
		return "ResolvedPath[immediateParent.typeName="+NBTRegistry.typeNameForTag(immediateParent)+",leaf.typeName="+NBTRegistry.typeNameForTag(leaf)+",parentPath="+parentPath+",canonicalPath="+canonicalPath+"]";
	}
	
}
