#!/bin/bash
echo Invoking Gradle...
output=`gradle fatJar 2>&1`
gradleExit=$?
if [ "$gradleExit" != "0" ]; then
	echo output
	exit $gradleExit
else
	echo
	env -u_JAVA_OPTIONS java -jar build/libs/unbted-*-fat.jar "$@"
fi
