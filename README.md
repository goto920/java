# java
Programming practice of GUI and stereo music processing.
Executable jar files are in executables/ (java -jar jarfile)

ConvertToWaveApp.jar : Convert mp3, mp4, m4a, aac, 24bit wav to 16bit wav (audio only)

MusicPlayerAppSwing.jar : Play, Cancel vocal by freq and pan, 
     or Split percussive and harmonic sound (16bit stereo wav input file only)

Currently executable jar files are only for Java 8 Runtime Environment (latest) 
 (Also worked with Ubuntu openjdk-7)

Build (on Linux or Mac OS X):
  cd build/
  sh makeMusicPlayer.app

Copy build/MusicPlayer.jar to desired directly.

Execution:
  As command line
    java -jar MusicPlayer.jar 

  or just click the file on Windows

Directory structure
 executables/ -- pre-compiled applications (may not work for older java RE)
 build/ -- build sh script etc. MusicPlayerAppSwing.jar
 jarlib/ -- dependent java libraries (jar files)
 MusicPlayer/ -- source code of the main GUI
    Android/ -- Must be compiled with Android SDK 
    Swing/   -- plain java source
    Tests/   -- some experimental programs

 ConverToWav16/ -- audio file converter source code for the main GUI

 srclib/jp/kmgoto/musicplayer/ -- support package library source codes
  *.java -- common sources
  android/ -- for android only
  swing/   -- for Swing

End of description
