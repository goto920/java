Java programming practice of GUI and music sound processing.

Executable jar files for downloaded are in bin/

Execution:

  Just click the file icon on Windows,

  or as a command line on Windows, Linux, Mac OS X

    java -jar xxx.jar 

Note: Programs were compiled with javac 1.7.0_121 (openJDK on Ubuntu 14.04LTS)  

Known bugs:   
      On Windows, sound device names are not correctly shown
      if the system language is not English. It looks like
      a bug in sound API in Java RE.

List of programs

1) ConvertToWave16App.jar

   Audio file converter for the following programs,
   just in case you do not have a converter.
 
   Input: mp4 video, mp4 audio (m4a, mp4, aac), mp3, 24bit wave

   Output: always 16bit wave audio

2) TimePitchPlayerApp.jar

   Variable time and pitch music player.

   Input: 16bit 44.1kHz stereo wave file only. 

   Real time playback or save the converted sound to a file.

   Output: 16bit 44.1kHz stereo wave file

3) FilteredPlayerApp.jar

   Input: 16bit 44.1kHz stereo wave only. 

   Extract or delete some part of stereo music 
   by frequency range, LR position(pan), percussive/harmonic separation.

   Playback or save to file.

   This may be useful to make karaoke, drum suppressed music for
   musical skill training. Extracting one instrument might be possible
   if the freq, pan range of the instrument is in good separation.
   
   In the filter window, choose  

   T(hrough), M(ute), P(ercussive), H(armonic) 

   or use Presets (karaokeMale or drumCover) and modify the filter.

   Customized filter can be saved in a binary file using Java object
   serialization.

 Note: Split is not implemented yet.
   This application is quite CPU intensive.
   Playback delay is intensionaly to have good sound quality.
   Automatic filter and splitter is work in progress.

4) FeedbackBoosterApp2.jar (work in progress)

   Feedback booster for electric guitar.

   Equipments: Dynamic mic in front of a guitar amp

          Audio I/O interface

          Small audio power amp

          Vibration speaker (exciter)

   See docs/FeedbackBoosterApp-howto.html

  Note: Audio input latency is not short enough especially on Windows.
        Workaround is in developement.

+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
Directory structure

MusicTools/src/ -- source code and dependent library classes

              jp/kmgoto/music/ -- my library

BACK/Android/ -- experimental code for drum suppressor for Android

    /ConvertToWave16/ -- source code for ConvertToWave16.jar 

    /MusicPlayer/ -- experimental code (not interested)

    /srclib/ -- source code of my old library

    /TimeStretchPlayer/ -- older version of TimePitchPlayer 
             without variable pitch function

    /jarlib/ -- library jar files 

End of description
++++++++++++++++++++++++++++++
