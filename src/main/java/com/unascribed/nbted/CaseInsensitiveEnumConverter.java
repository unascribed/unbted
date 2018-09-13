package com.unascribed.nbted;

import java.util.Locale;

import com.google.common.base.Enums;

import joptsimple.ValueConverter;

public class CaseInsensitiveEnumConverter<E extends Enum<E>> implements ValueConverter<E> {

	private final Class<E> clazz;
	
	public CaseInsensitiveEnumConverter(Class<E> clazz) {
		this.clazz = clazz;
	}
	
	@Override
	public E convert(String value) {
		return Enums.getIfPresent(clazz, value.toUpperCase(Locale.ROOT)).get();
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
