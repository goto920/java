// docs.oracle.com -- Java Thread primitive Deprecation
package jp.kmgoto.musicplayer;

import android.util.Log;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ConverterThread extends Thread {

   public synchronized void stopConv(){ running = false; }


   public ConverterThread (String rawInputFile, String wavOutputFile,
               FFTEffector ef, boolean deleteUponComplete, 
               SeekBar progressSlider){
          this.rawInputFile = rawInputFile;
          this.wavOutputFile = wavOutputFile;
          this.ef = ef;
          this.deleteUponComplete = deleteUponComplete;
          this.progressSlider = progressSlider;
          init();
   }

   @Override
   public void run(){

      byte[] ibuf = new byte[4096];
      byte[] out  = new byte[4096];
      ArrayList<Byte> obuf = new ArrayList<>();
       if (progressSlider == null) Log.d("CONV", "progressSlider not set before run()");
            else Log.d("CONV", "progressSlider set before run()");
      running = true;
      int ilen, ret;

      try {
         while (bis.available() > 0 && running) {
            ilen = bis.read(ibuf); if (ilen < 0) break;
            if ((ret = ef.process(ibuf, ilen, obuf)) == 0) continue;

            for (int i = 0; i < ret; i++) out[i] = obuf.get(i);

            obuf.clear();

            bos.write(out, 0, ret);
            progress +=ret;
            if (progressSlider != null) updateProgress(progress);
         } 
      } catch (IOException e) { e.printStackTrace(); }

      try { 
         if (bis != null) bis.close();
         while ((ret = ef.process(null, -1, obuf)) > 0) {
            for (int i = 0; i < ret; i++) out[i] = obuf.get(i);
            obuf.clear();
            bos.write(out, 0, ret);
            progress += ret;
            updateProgress(progress);
         }
         bos.close();
      } catch (IOException e) {e.printStackTrace();}

      Utils.rawToWaveFile(tmpFileName, wavOutputFile, true);

      if (deleteUponComplete) { if (!iFile.delete()) System.out.println("tmpFile delete failed");}
            // Log.d("CONV", "End of run()");
        if (progressSlider != null) updateProgress(100);

      } // End of run()

      private volatile boolean running;
      private volatile ProgressBar progressSlider;

      private String rawInputFile;
      private String tmpFileName;
      private String wavOutputFile;
      private File iFile;
      private FFTEffector ef;
      private long total;
      private long progress;
      private boolean deleteUponComplete;
      private BufferedInputStream bis;
      private BufferedOutputStream bos;
      private int progressPercent;

      private void init(){
        iFile = new File(rawInputFile);
        total = iFile.length();
        if(progressSlider != null)  {
//            progressSlider.setMax(100);
//            progressSlider.setProgress(0);
         //  Log.d("CONV", "progressSlider set");
        } else 
           // Log.d("CONV", "progressSlider NOT set");

      //  Log.d("CONV", "file size: " + total);

        progress = 0;

        tmpFileName = wavOutputFile + "-tmp";
        File tmpFile = new File(tmpFileName);

        try {
           FileInputStream fis = new FileInputStream(iFile);
           bis = new BufferedInputStream(fis);

           FileOutputStream fos = new FileOutputStream(tmpFile);
           bos = new BufferedOutputStream(fos);

        } catch (IOException e) { e.printStackTrace();}

     } // End init()

     private void updateProgress(long outputBytes){
        int tmp = (int) (100*progress/(float) total);

        if (tmp  > progressPercent && tmp < 100){
            progressSlider.setProgress(tmp);
            progressPercent = tmp;
          //  Log.d("CONVERTER","progress update sent as " + tmp);
        }
     }

} // End of class
