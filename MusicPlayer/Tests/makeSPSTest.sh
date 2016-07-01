#!/bin/sh

if [ $1 = "c" ]; then 
javac StereoPSTest.java -classpath ../../srclib:../../jarlib/JTransforms-3.1-with-dependencies.jar 
fi

if [ $1 = "r" ]; then
java -classpath ../../srclib:../../jarlib/JTransforms-3.1-with-dependencies.jar:. StereoPSTest $2 $3 
fi
