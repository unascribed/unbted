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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Command {
	public interface Action {
		void run(OptionSet set, List<String> args) throws Exception;
	}
	public interface Options {
		void setup(OptionParser parser) throws Exception;
	}
	
	private Action action;
	private Completer completer;
	private Options options;
	private String description = "";
	private String name = "";
	private ImmutableList<String> aliases = ImmutableList.of();
	private ImmutableSet<String> allNames = ImmutableSet.of();
	
	private Command() {}
	
	public void execute(Iterable<String> args) throws Exception {
		execute(Iterables.toArray(args, String.class));
	}
	
	public void execute(String... args) throws Exception {
		OptionParser parser = new OptionParser();
		setupOptionParser(parser);
		OptionSet set = parser.parse(args);
		execute(set);
	}
	
	public void execute(OptionSet set) throws Exception {
		action.run(set, (List<String>)set.nonOptionArguments());
	}
	
	public void setupOptionParser(OptionParser parser) throws Exception {
		if (options != null) {
			options.setup(parser);
		}
	}
	
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		if (completer != null) {
			completer.complete(reader, line, candidates);
		}
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getName() {
		return name;
	}
	
	public ImmutableList<String> getAliases() {
		return aliases;
	}
	
	public ImmutableSet<String> getAllNames() {
		return allNames;
	}
	
	
	
	public Command action(Action action) {
		this.action = action;
		return this;
	}
	
	public Command completer(Completer completer) {
		this.completer = completer;
		return this;
	}
	
	public Command options(Options options) {
		this.options = options;
		return this;
	}
	
	public Command description(String description) {
		this.description = description;
		return this;
	}
	
	public Command name(String name) {
		this.name = name;
		this.allNames = ImmutableSet.copyOf(Iterables.concat(Collections.singleton(name), aliases));
		return this;
	}
	
	public Command aliases(String... aliases) {
		this.aliases = ImmutableList.copyOf(aliases);
		this.allNames = ImmutableSet.copyOf(Iterables.concat(Collections.singleton(name), Arrays.asList(aliases)));
		return this;
	}
	
	
	public static Command create() {
		return new Command();
	}

}
