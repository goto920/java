// docs.oracle.com -- Java Thread primitive Deprecation
// http://ai-argument.hatenadiary.jp/entry/2013/02/14/211024
package jp.kmgoto.musicplayer.swing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
// import java.nio.ByteBuffer;
// import java.util.ArrayList;
import javax.swing.*;
import javax.sound.sampled.*;

public class PlayerThread extends Thread {

    private String inputFile;
//    private BufferedInputStream  bis;
    private AudioInputStream ain;
    private SourceDataLine line;

    private volatile boolean running;
    private boolean ready;
    private volatile JSlider playTimeSlider;
    private AudioFormat outputFormat;
    private long inputLength;
    private long totalOutput;
    // private FloatControl volCtrl;
    private float gain;
    private long lastOutput = 0;

    public synchronized void stopPlay(){ 
      running = false; 
    }

    public PlayerThread(String inputFile, JSlider playTimeSlider){
        this.inputFile = inputFile;
        this.playTimeSlider = playTimeSlider;
        ready = init();
        gain = 100/150f; 
    }

    public synchronized void setVolume(float vol){
        gain = vol; 
    }

    private void updateTime(){
        if (totalOutput > lastOutput + 1024*10){
           playTimeSlider.setValue((int) (totalOutput/1024));
           lastOutput = totalOutput;
        }
    }

    private boolean init(){

       File in = new File(inputFile);
       inputLength = 0;

       AudioFormat format;
       try { 
         ain = AudioSystem.getAudioInputStream(in);
         format = ain.getFormat();
         // System.out.println(format.toString());
         AudioFileFormat fformat 
            = AudioSystem.getAudioFileFormat(in);
         inputLength 
            = 2 * format.getChannels() * fformat.getFrameLength();

       } catch (Exception e) {
         e.printStackTrace(); 
         return false;
       }

       if (format != null) System.err.println(format.toString());

       if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED){
         System.err.println("Sorry, PCM_SIGNED only");
         return false;
       }

       if (format.getFrameSize() != 4){
         System.err.println("Sorry, 4 bytes frame(16bit stereo) only");
         return false;
       }

       if (format.isBigEndian() == true){
         System.err.println("Sorry, LE only");
         return false;
       }

       System.err.println("sampling rate  " + format.getSampleRate());
/*
       if (format.getSampleRate() != 44100f){
         return false;
       }
*/

       if (format.getChannels() != 2){
         System.err.println("Sorry, stereo only");
         return false;
       }

          outputFormat = new AudioFormat(
          AudioFormat.Encoding.PCM_SIGNED,
          format.getSampleRate(), // frame rate
          16, // bits per sample
          format.getChannels(),
          format.getFrameSize(),  // should be 2*2 for 16bit st.
          format.getSampleRate(), // frame rate
          false // Little Endian
          );

       DataLine.Info info 
          = new DataLine.Info(SourceDataLine.class, outputFormat);

       try {
         line = (SourceDataLine) AudioSystem.getLine(info);
       } catch (Exception e) {e.printStackTrace();}

       playTimeSlider.setMinimum(0);
       playTimeSlider.setMaximum((int) (inputLength/1024));
       lastOutput = 0;

       return true;
    }

    @Override
    public void run(){
      if (!ready) {
        System.err.println("Sorry, can't play this file");
        return;
      }

      byte[] buff = new byte[4096];
      int readlen, writelen;
      boolean EOD = false;

      long skipTo = 1024*playTimeSlider.getValue();

      long totalInput = 0;
           totalOutput = skipTo; 

      try {
        line.open(outputFormat, buff.length);
         // volCtrl = (FloatControl) line.getControl(
          // FloatControl.Type.VOLUME);
      } catch (Exception e) { e.printStackTrace();}

      line.start();
      running = true;
      while(running & !EOD){
        try {
         readlen = ain.read(buff); 
//         System.out.println("readlen " + readlen);
         if (readlen < 0) {EOD = true; continue;}

         totalInput += readlen;
         if (totalInput >= skipTo){
           writelen = line.write(buff,0,readlen);
           totalOutput += writelen;
           // playTimeSlider.setValue((int) (totalOutput/1024));
           updateTime();
         }

        } catch (Exception e) {e.printStackTrace(); }
      }

      line.drain();
      line.close();
      System.err.println("PlayerThread exiting...");
    } 

}
