package jp.kmgoto.musicplayer.swing;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import javax.swing.*;
import javax.sound.sampled.*;

public class Utils {

  public static AudioFormat checkFormat(String inputFile){

    AudioFormat format = null;

    if (!inputFile.toUpperCase().endsWith(".WAV")) return null;

    File in = new File(inputFile);


    try { 
       AudioInputStream ain = AudioSystem.getAudioInputStream(in);
         format = ain.getFormat();
         // System.out.println(format.toString());
       ain.close();
    } catch (Exception e) {
        // e.printStackTrace(); 
         return null;
    }

    System.err.println(format.toString());

    if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED){
       System.err.println("Sorry, PCM_SIGNED only");
       return null;
    }

    if (format.getFrameSize() != 4){
         System.err.println("Sorry, 4 bytes frame(16bit stereo) only");
       return null;
    }

    if (format.isBigEndian() == true){
        System.err.println("Sorry, LE only");
       return null;
    }

    if (format.getChannels() != 2){
       System.err.println("Sorry, stereo only");
       return null;
    }

    return format;
  }

  static int LEbytesToInt(byte[] in){
     int retval = 0;
     if (in.length == 2) {
//   System.out.println("in0,1 = " + (in[0] & 0xff) + " " + (in[1] & 0xff));
       retval = (in[0] & 0xff) | (in[1] & 0xff) << 8; // 0xff required
     } else if (in.length == 4){ 
       retval =  (in[0] & 0xff) | (in[1] & 0xff) << 8 
               | (in[2] & 0xff) << 16 | (in[3] & 0xff) << 24;
     }

     return retval;
  }

  static byte[] intToLEbytes(int in, int len){
     byte[] retval = new byte[len];

     if (len == 2){ 
        retval[1] = (byte) (in >> 8 & 0xff);
        retval[0] = (byte) (in & 0xff);
     } else if (len == 4) { 
        retval[3] = (byte) (in >> 24 & 0xff); 
        retval[2] = (byte) (in >> 16 & 0xff);
        retval[1] = (byte) (in >> 8 & 0xff);
        retval[0] = (byte) (in & 0xff);
     }

     return retval;
  }

  static int writeWaveHeader (
    BufferedOutputStream bos, int samplingRate, int datalen){

    int total = 0;

  try {
    byte[] riff_id = {'R','I','F','F'}; // 4
    bos.write(riff_id,0,riff_id.length);
      total += riff_id.length; 
    byte[] riff_size = Utils.intToLEbytes(datalen + 36, 4); 
    bos.write(riff_size,0,riff_size.length);
      total += riff_size.length; 
    byte[] riff_type = {'W','A','V','E'}; // 4
    bos.write(riff_type,0,riff_type.length);
      total += riff_type.length; 
    byte[] wave_id = {'f','m','t',' '}; // 4
    bos.write(wave_id,0,wave_id.length);
      total += wave_id.length; 

    byte[] wave_size = Utils.intToLEbytes(16,4);
    bos.write(wave_size,0,wave_size.length);
      total += wave_size.length; 

    byte[] wave_compression = Utils.intToLEbytes(1,2);
    bos.write(wave_compression,0,wave_compression.length);
      total += wave_compression.length;

    byte[] wave_channels = Utils.intToLEbytes(2,2);
    bos.write(wave_channels,0,wave_channels.length);
      total += wave_channels.length;

    byte[] wave_rate = Utils.intToLEbytes(samplingRate,4);
    bos.write(wave_rate,0,wave_rate.length);
       total += wave_rate.length;

    byte[] wave_bytesPerSec = Utils.intToLEbytes(
          (samplingRate*2*16)/8, 4); 
    bos.write(wave_bytesPerSec,0,wave_bytesPerSec.length);
       total += wave_bytesPerSec.length;
    byte[] wave_alignment = Utils.intToLEbytes(4,2);
    bos.write(wave_alignment,0,wave_alignment.length);
       total += wave_alignment.length;

    byte[] wave_bitsPerSample = Utils.intToLEbytes(16,2);
    bos.write(wave_bitsPerSample,0,wave_bitsPerSample.length);
       total += wave_bitsPerSample.length;

    byte[] data_id = {'d','a','t','a'}; // 4
    bos.write(data_id,0,data_id.length);
       total += data_id.length;

    byte[] data_size = Utils.intToLEbytes(datalen, 4); 
    bos.write(data_size,0,data_size.length); 
       total += data_size.length;
    } catch (Exception e) { e.printStackTrace();}
   
    return total; 
  }

}
