package jp.kmgoto.music;

import java.io.*;
import javax.sound.sampled.*;

/*
  Usage:
    WaveWrite = new WaveWriter(format, origFile, extension);
    rawWrite(samples); // repeat until end
    waveWrite(); // to origFile-extension.wav

*/

public class WaveWriter {
   private File outFile, rawFile;
   private String extension;
   private AudioFormat format;
   private FileOutputStream fos;

   public WaveWriter(AudioFormat format, File origFile, String extension)
     throws Exception {
      this.format = format;
      rawFile = new File(origFile.getName() + "-" + extension + ".raw");
      outFile = new File(origFile.getName() + "-" + extension + ".wav");
      fos = new FileOutputStream(rawFile);
   }

   public void rawWrite(byte[] samples) throws Exception{
      fos.write(samples);
   } 

   public void waveWrite() {
    try {
     fos.close();
     AudioInputStream ois = new AudioInputStream(
         new FileInputStream(rawFile),
         format,
         rawFile.length()/format.getFrameSize()
     );
     AudioSystem.write(ois, AudioFileFormat.Type.WAVE, outFile);
     rawFile.delete();
    } catch (Exception e){ e.printStackTrace();}
   
   }

}
