#!/bin/sh
cd build/libs
native-image --libc=musl --static -jar unbted-$1.jar
strip unbted-$1
fin=unbted-$1-$(uname -m)
mv unbted-$1 $fin
gzip -k9 $fin
xz -k9 $fin
