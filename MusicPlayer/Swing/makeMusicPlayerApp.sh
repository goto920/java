rm *.class
rm ../../srclib/jp/kmgoto/musicplayer/*.class
rm ../../srclib/jp/kmgoto/musicplayer/swing/*.class
#
native2ascii -encoding utf8 HELP.UTF8 HELP_ja_JP.properties
#
javac -source 1.7 -target 1.7 -classpath ../../srclib/:../../jarlib/jl1.0.1.jar:../../jarlib/JTransforms-3.1-with-dependencies.jar MusicPlayerApp.java
