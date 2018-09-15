#!/bin/bash
echo -ne 'Invoking Gradle...\r'
tmp=`mktemp`
gradle fatJar </dev/null > $tmp 2>&1
gradleExit=$?
if [ "$gradleExit" != "0" ]; then
	cat $tmp
	exit $gradleExit
else
	echo -en 'Running program...\r'
	env -u_JAVA_OPTIONS java -jar build/libs/unbted-*-fat.jar "$@"
fi
