<a href="https://asciinema.org/a/201725" target="_blank"><img width="320" align="right" src="https://asciinema.org/a/201725.svg" /></a>
# Una's NBT Editor

unbted (**U**na's **NBT** **Ed**itor) is a command-line interactive NBT editor. That's about it, really.
Since it's command-line, it can be easily used over SSH connections on remote servers, instead of having to download the file and edit it locally with one of the many GUI editors.

## Downloads
Releases are [here on GitHub](https://github.com/unascribed/unbted/releases).

## Running
If you're on an x86_64 Linux system, you can download the native-image build
from the releases (called something like unbted-1.1-x86_64) and stick it in
your PATH as "unbted". It acts just like a native executable because, well, it
is.

You can also use the JAR build (e.g. unbted-1.1.jar) with any JRE. You can then
put a shell script like the following in your PATH:
```sh
#!/bin/sh
java -jar /opt/unbted/unbted-1.1.jar "$@"
```

Note: The JAR build starts up a good bit slower and requires a JRE be available.

## Features

* ANSI-colorized output for more distinctive and easier to skim output
* Can convert NBT files to well-formed JSON for processing by anything that can parse JSON
  * As of 1.1, a special NBT JSON format is supported that can be roundtripped. Want to edit an NBT file with jq? Now you can.
* Support for "inferred" types when printing NBT trees in its default format. This includes:
  * **JSON** - JSON objects such as `generatorOptions` in level.dat will be colorized, indented, and split into multiple lines for easier reading
  * **UUID** - Pairs of long NBT tags with names ending in `Most` and `Least` will be printed as a UUID
    * The `set` command also supports UUIDs, which makes working with them easy
  * **Forge registries** - Lists containing compounds with only two children, `K` and `V`, will be printed in a condensed format for easier skimming, and sorted by their value
  * **Booleans** - unbted will try to guess whether or not an NBT byte is a boolean, and if it thinks it is, will print 0 as false and 1 as true.
* Support for all NBT tags, including int and long arrays
* Full help included
  * Since the help is just plain text files, you can view it online:
    * Usage: https://github.com/unascribed/unbted/blob/master/src/main/resources/switches-help.txt
    * Commands: https://github.com/unascribed/unbted/blob/master/src/main/resources/commands-help.txt
* Full line editing and tab completion
* Persistent history
* Compression autodetection
* Written in Java and compiled to a native statically linked executable
* Support for little-endian legacy Pocket Edition NBT files

## Planned features

* Anvil file support
* Mojangson (i.e. command block format) support for `set --compound`
* Scripting

## Building
`gradle fatJar` for a fast build that emits a large artifact, `gradle build` for 
an optimized artifact, `gradle nativeImage` for the native executable. 
(nativeImage requires Graal native-image on your PATH as well as musl-gcc and a 
static build of zlib for musl.)

All emit into `build/libs`.

## License
```
unbted - Una's NBT Editor
Copyright (C) 2018 - 2020 Una Thompson (unascribed)

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
