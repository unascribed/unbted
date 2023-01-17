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

import java.util.Locale;

import com.google.common.base.Enums;
import com.google.common.base.Optional;

import joptsimple.ValueConverter;

public class CaseInsensitiveEnumConverter<E extends Enum<E>> implements ValueConverter<E> {

	private final Class<E> clazz;
	
	public CaseInsensitiveEnumConverter(Class<E> clazz) {
		this.clazz = clazz;
	}
	
	@Override
	public E convert(String value) {
		Optional<E> opt = Enums.getIfPresent(clazz, value.toUpperCase(Locale.ROOT));
		if (!opt.isPresent()) {
			throw new IllegalArgumentException(value+" is not an acceptable value for "+valuePattern());
		}
		return opt.get();
	}

	@Override
	public Class<? extends E> valueType() {
		return clazz;
	}

	@Override
	public String valuePattern() {
		return clazz.getSimpleName().toLowerCase(Locale.ROOT);
	}

}
