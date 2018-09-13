#!/bin/bash
echo -ne 'Invoking Gradle...\r'
output=`gradle fatJar 2>&1 </dev/null`
gradleExit=$?
if [ "$gradleExit" != "0" ]; then
	echo output
	exit $gradleExit
else
	echo -en 'Running program...\r'
	env -u_JAVA_OPTIONS java -jar build/libs/unbted-*-fat.jar "$@"
fi
