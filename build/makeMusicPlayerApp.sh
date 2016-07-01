(cd ../MusicPlayer/Swing/; sh makeMusicPlayer.sh)
(cd  ../srclib/; sh makeJar.sh)

jar xvf ../jarlib/JTransforms-3.1-with-dependencies.jar
jar xvf ../jarlib/jp.kmgoto.musicplayer.jar
#jar xvf ../jarlib/jl1.0.1.jar
#jar xvf ../jarlib/jaad-0.8.4.jar

cp ../MusicPlayer/Swing/MusicPlayerApp.class .
cp ../MusicPlayer/Swing/HELP*.properties .

jar cvfe MusicPlayerAppSwing.jar MusicPlayerApp \
	MusicPlayerApp.class HELP*.properties jp org pl
