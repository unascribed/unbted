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

import java.util.List;
import com.google.common.collect.ImmutableList;

public class Command {

	public interface Exec {
		void exec(List<String> arguments) throws Exception;
	}
	
	public final Exec exec;
	public final String description;
	public final ImmutableList<String> names;

	public Command(Exec exec, String description, String... names) {
		this.exec = exec;
		this.description = description;
		this.names = ImmutableList.copyOf(names);
	}

}
