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
}
