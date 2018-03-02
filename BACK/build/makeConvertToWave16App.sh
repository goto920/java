jar xf ../jarlib/jl1.0.1.jar
jar xf ../jarlib/jaad-0.8.4.jar
(cd  ../srclib/; sh makeJar.sh)
jar xvf ../jarlib/jp.kmgoto.musicplayer.jar

cp ../ConvertToWave16/ConvertToWave16App.class .
cp ../ConvertToWave16/HELP*.properties .

jar cvfe ConvertToWave16App.jar ConvertToWave16App \
	ConvertToWave16App.class HELP*.properties \
	jp/kmgoto/musicplayer/swing/ConvertToWave.class \
	javazoom/jl/ net/sourceforge/jaad/
