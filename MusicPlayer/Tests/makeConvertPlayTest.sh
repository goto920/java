#!/bin/sh

if [ $1 = "c" ]; then 
javac -classpath ../../srclib:../../jarlib/JTransforms-3.1-with-dependencies.jar ConvertPlayTest.java
fi

if [ $1 = "r" ]; then
java -classpath ../../srclib:../../jarlib/JTransforms-3.1-with-dependencies.jar:. ConvertPlayTest $2 $3 
fi
