// docs.oracle.com -- Java Thread primitive Deprecation
package jp.kmgoto.musicplayer.swing;

// import android.util.Log;
// import android.widget.ProgressBar;
// import android.widget.SeekBar;

import jp.kmgoto.musicplayer.*;
// import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.*;
import javax.sound.sampled.*;

public class ConverterThread extends Thread {

   public synchronized void stopConv(){ running = false; }

   public ConverterThread (String wavInputFile, String wavOutputFile,
               FFTEffector ef, JProgressBar progressSlider){
          this.wavInputFile = wavInputFile;
          this.wavOutputFile = wavOutputFile;
          this.ef = ef;
          this.progressSlider = progressSlider;
          init();
   }

   @Override
   public void run(){

      byte[] ibuf = new byte[4096];
      byte[] out  = new byte[4096];
      ArrayList<Byte> obuf = new ArrayList<>();

      running = true;
      int ilen, ret;

      try {
         while (ais.available() > 0 && running) {
            ilen = ais.read(ibuf); if (ilen < 0) break;
            if ((ret = ef.process(ibuf, ilen, obuf)) == 0) continue;

            for (int i = 0; i < ret; i++) out[i] = obuf.get(i);

            obuf.clear();

            bos.write(out, 0, ret);
            progress +=ret;
            if (progressSlider != null) updateProgress(progress);
         } 
      } catch (IOException e) { e.printStackTrace(); }

      try { 
         if (ais != null) ais.close();
         while ((ret = ef.process(null, -1, obuf)) > 0) {
            for (int i = 0; i < ret; i++) out[i] = obuf.get(i);
            obuf.clear();
            bos.write(out, 0, ret);
            progress += ret;
            updateProgress(progress);
         }
         ais.close();
      } catch (IOException e) {e.printStackTrace();}
        progressSlider.setValue(100); // really finished

    } // End of run()

      private volatile boolean running;
      private volatile JProgressBar progressSlider;

      private String wavInputFile;
      private String wavOutputFile;
      private File iFile;
      private FFTEffector ef;
      private int total;
      private long progress;
      private AudioInputStream ais;
      private BufferedOutputStream bos;
      private int progressPercent;

      private void init(){

        iFile = new File(wavInputFile);
        int length = 0; 
        int samplingRate = 0;
        try {
          AudioFileFormat fformat 
            = AudioSystem.getAudioFileFormat(new File(wavInputFile));
          length = fformat.getFrameLength();
      
          AudioFormat format = Utils.checkFormat(wavInputFile);
          samplingRate = (int) format.getSampleRate();
          ef.setSamplingRate(samplingRate);

          total = length*2*format.getChannels();
          // iFile.length() - 44; // 44 is wave header
 
        } catch (Exception e) {e.printStackTrace();}


        if(progressSlider != null)  {
            progressSlider.setMaximum(100);
            progressSlider.setValue(0);
        } else { 
           System.err.println("progressSlider NOT set");
        } 

        progress = 0;

        try {
           ais = AudioSystem.getAudioInputStream(iFile);
           bos = new BufferedOutputStream(
             new FileOutputStream(new File(wavOutputFile))
           );
           // write wave headers
           Utils.writeWaveHeader(bos, samplingRate, total);
        } catch (Exception e) { e.printStackTrace();}

     } // End init()

     private void updateProgress(long outputBytes){
        int tmp = (int) (100*progress/(float) total);

        if (tmp  > progressPercent && tmp < 100){ // upto 99 parcent
            progressSlider.setValue(tmp);
            progressPercent = tmp;
        }
     }

} // End of class
