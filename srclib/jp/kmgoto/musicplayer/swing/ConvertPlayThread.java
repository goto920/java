package jp.kmgoto.musicplayer.swing;

import jp.kmgoto.musicplayer.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
// import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.swing.*;
import javax.sound.sampled.*;

public class ConvertPlayThread extends Thread {

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
    private long lastOutput;
    private FFTEffector ef;

    public synchronized void stopPlay(){ 
      running = false; 
    }

    public ConvertPlayThread(String inputFile, JSlider playTimeSlider, 
       FFTEffector ef){
        this.inputFile = inputFile;
        this.playTimeSlider = playTimeSlider;
        this.ef = ef;
        ready = init();
        gain = 100/150f; 
        lastOutput = 0;
    }

    public synchronized void setVolume(float vol){
        gain = vol; 
    }

    private void updateTime(){
        if (totalOutput > lastOutput + 1024*10){
           if (playTimeSlider != null)
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

       if (playTimeSlider != null) {
         playTimeSlider.setMinimum(0);
         playTimeSlider.setMaximum((int) (inputLength/1024));
       }
       lastOutput = 0;

       return true;
    }

    @Override
    public void run(){
      if (!ready) {
        System.err.println("Sorry, can't play this file");
        return;
      }

      byte[] ibuf = new byte[4096];
      ArrayList<Byte> obuf = new ArrayList<>();
      byte[] out = new byte[4096];
      int readlen, writelen;
      boolean EOD = false;

      long skipTo = 0;
       if (playTimeSlider != null)
           skipTo = 1024*playTimeSlider.getValue();

      long totalInput = 0;
           totalOutput = skipTo; 
      int efRet = 0;

      try {
        line.open(outputFormat, 100*out.length);
         // volCtrl = (FloatControl) line.getControl(
          // FloatControl.Type.VOLUME);
        line.start();
        running = true;

        while(running & !EOD){
           readlen = ain.read(ibuf); 
           if (readlen < 0) {EOD = true; continue;}
           totalInput += readlen;

           if (ef != null &&
              (efRet = ef.process(ibuf,readlen,obuf))== 0)
            continue;

           for (int i=0; i < efRet; i++) out[i] = obuf.get(i);
           obuf.clear();

           if (totalInput >= skipTo){
           writelen = line.write(out,0,efRet);
           totalOutput += writelen;
           updateTime();
           }
         }

       } catch (Exception e) {e.printStackTrace(); }

       try { 
         if (ain != null) ain.close();
         if (ef != null){
           while ((efRet = ef.process(null, -1, obuf)) > 0) {
            for (int i = 0; i < efRet; i++) out[i] = obuf.get(i);
            obuf.clear();
            writelen = line.write(out, 0, efRet);
            totalOutput += writelen;
            updateTime();
           }
         }
         ain.close();
      } catch (IOException e) {e.printStackTrace();}

      line.drain();
      line.close();

      System.err.println("ConvertPlayThread exiting...");
    } 

}
