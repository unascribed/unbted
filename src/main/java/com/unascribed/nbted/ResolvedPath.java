/*
 * unbted - Una's NBT Editor
 * Copyright (C) 2018 - 2020 Una Thompson (unascribed)
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
		return "ResolvedPath[immediateParent.typeName="+NBTRegistry.typeNameForTag((NBTTag)immediateParent)+",leaf.typeName="+NBTRegistry.typeNameForTag(leaf)+",parentPath="+parentPath+",canonicalPath="+canonicalPath+"]";
	}
	
}
