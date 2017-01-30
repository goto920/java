JTRANSFORM= pl org/apache \
	org/jtransforms/utils \
	org/jtransforms/fft/FloatFFT_1D*.class

all: TimePitchPlayerApp.jar FilteredPlayerApp.jar FeedbackBoosterApp2.jar

TimePitchPlayerApp.jar: TimePitchPlayerApp 
	jar cvef TimePitchPlayerApp ../../bin/TimePitchPlayerApp.jar \
	TimePitchPlayerApp.class TimePitchPlayer.class \
	javazoom com jp/kmgoto/music/*.class
	chmod +x ../../bin/TimePitchPlayerApp.jar

TimePitchPlayerApp: clean TimePitchPlayerApp.java
	javac TimePitchPlayerApp.java

TimeStretchPlayerApp.jar: TimeStretchPlayerApp 
	jar cvef TimeStretchPlayerApp ../../bin/TimeStretchPlayerApp.jar \
	TimeStretchPlayerApp.class TimeStretchPlayer.class \
	javazoom jp/kmgoto/music/*.class
	chmod +x ../../bin/TimeStretchPlayerApp.jar

TimeStretchPlayerApp: clean TimeStretchPlayerApp.java
	javac TimeStretchPlayerApp.java

FilteredPlayerApp.jar: FilteredPlayerApp
	jar cvef FilteredPlayerApp ../../bin/FilteredPlayerApp.jar \
	FilteredPlayerApp.class FilteredPlayer.class FilterGUI.class \
	javazoom $(JTRANSFORM) jp/kmgoto/music/*.class
	chmod +x ../../bin/FilteredPlayerApp.jar

FilteredPlayerApp: clean FilteredPlayerApp.java FilterGUI.java
	javac FilteredPlayerApp.java

FeedbackBoosterApp.jar: FeedbackBoosterApp
	jar cvef FeedbackBoosterApp FeedbackBoosterApp.jar \
	FeedbackBoosterApp.class FeedbackBoosterPlayer.class \
	jp/kmgoto/music/*.class
	chmod +x FeedbackBoosterApp.jar

FeedbackBoosterApp2.jar: FeedbackBoosterApp2
	jar cvef FeedbackBoosterApp2 ../../bin/FeedbackBoosterApp2.jar \
	FeedbackBoosterApp2.class FeedbackBoosterPlayer2.class \
	jp/kmgoto/music/*.class
	chmod +x ../../bin/FeedbackBoosterApp2.jar

FeedbackBoosterApp: clean FeedbackBoosterApp.java
	javac FeedbackBoosterApp.java

FeedbackBoosterApp2: clean FeedbackBoosterApp2.java
	javac FeedbackBoosterApp2.java

clean:
	rm -f *.class jp/kmgoto/music/*.class

