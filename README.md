<a href="https://asciinema.org/a/syF7agJRfqjAqhPrXkPhUHBiq"><img width="320px" align="right" src="https://asciinema.org/a/syF7agJRfqjAqhPrXkPhUHBiq.png"/></a>
# Una's NBT Editor

unbted (**U**na's **NBT** **Ed**itor) is a command-line interactive NBT editor. That's about it, really.
Since it's command-line, it can be easily used over SSH connections on remote servers, instead of having to download the file and edit it locally with one of the many GUI editors.

## Downloads
Releases are [here on GitHub](https://github.com/unascribed/unbted/releases). Development builds can be found [on the Elytra Jenkins](https://ci.elytradev.com/view/all/job/unascribed/job/unbted/job/master/).

## Running
Just run the jar with `java -jar unbted-1.0.jar`. I recommend adding a shell script to your path to invoke it, like this one:
```shell
#!/bin/bash
java -jar ~/unbted-1.0.jar "$@"
```
And then you can just invoke it as `unbted <args>`, assuming you have an `unbted-1.0.jar` in your home directory.

## Features

* ANSI-colorized output for more distinctive and easier to skim output
* Can convert NBT files to well-formed JSON for processing by anything that can parse JSON
  * (however, it cannot convert back, as type information is lost in the conversion)
* Support for "inferred" types when printing NBT trees in its default format. This includes:
  * **JSON** - JSON objects such as `generatorOptions` in level.dat will be colorized, indented, and split into multiple lines for easier reading
  * **UUID** - Pairs of long NBT tags with names ending in `Most` and `Least` will be printed as a UUID
    * The `set` command also supports UUIDs, which makes working with them easy
  * **Forge registries** - Lists containing compounds with only two children, `K` and `V`, will be printed in a condensed format for easier skimming, and sorted by their value
* Support for all NBT tags, including int and long arrays
  * (note: the 1.0 release does not have support for modifying arrays, only reading them; this will be added later)
* Full help included
  * Since the help is just plain text files, you can view it online:
    * Usage: https://github.com/unascribed/unbted/blob/master/src/main/resources/switches-help.txt
    * Commands: https://github.com/unascribed/unbted/blob/master/src/main/resources/commands-help.txt
* Full line editing and tab completion
* Persistent history
* Compression autodetection
* Written in Java, which you almost certainly already have installed (no messing with Mono)
* Support for little-endian legacy Pocket Edition NBT files

## Planned features

* Anvil file support
* Array editing support
* Mojangson (i.e. command block format) support for `set --compound`
* Scripting

## Building
`gradle fatJar` for a fast build that emits a large artifact, `gradle build` for an optimized artifact.

Both emit jars into `build/libs`.

## License
```
unbted - Una's NBT Editor
Copyright (C) 2018 Una Thompson (unascribed)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
```
