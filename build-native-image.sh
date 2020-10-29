#!/bin/sh
cd build/libs
native-image --libc=musl --static -jar unbted-$1.jar
strip unbted-$1
mv unbted-$1 unbted-$1-$(uname -m)
