package jp.kmgoto.musicplayer;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
// import java.util.Arrays;

public class Utils {

  static public void rawToWaveFile(String rawFile, String outFile,
       boolean delete){

     File inputFile  = new File(rawFile);
     File outputFile = new File(outFile);

   try {
     FileInputStream fis = new FileInputStream(inputFile);
     FileOutputStream fos = new FileOutputStream(outputFile);

     BufferedInputStream bis = new BufferedInputStream(fis);
     BufferedOutputStream bos = new BufferedOutputStream(fos);

     byte[] buf = new byte[4096];

     byte b1, b2, b3, b4;
     // riff header (12 bytes)
     byte[] riff_id = {'R','I','F','F'}; // 4
       bos.write(riff_id,0,riff_id.length);

     long tmp = inputFile.length() + 36;
       b1 = (byte) (tmp >> 24 & 0xff); b2 = (byte) (tmp >> 16 & 0xff);
       b3 = (byte) (tmp >> 8  & 0xff); b4 = (byte)   (tmp &     0xff); 
     byte[] riff_size = {b4,b3,b2,b1}; // LE 4 (32bit)
              //  datasize + 4 + wave header(24) + data header(8)
              //  = data_size + 36;
       bos.write(riff_size,0,riff_id.length);
     byte[] riff_type = {'W','A','V','E'}; // 4
       bos.write(riff_type,0,riff_type.length);

     // Wave header (20 bytes)
     byte[] wave_id = {'f','m','t',' '}; // 4
       bos.write(wave_id,0,wave_id.length);

      tmp = 16; // size of this header from here
       b1 = (byte) (tmp >> 24 & 0xff); b2 = (byte) (tmp >> 16 & 0xff);
       b3 = (byte) (tmp >> 8  & 0xff); b4 = (byte)   (tmp &     0xff); 
     byte[] wave_size = {b4,b3,b2,b1}; // LE 4 (32bit)
       bos.write(wave_size,0,wave_size.length);

     byte[] wave_compression = {1,0}; // LE 2 (16bit)
       bos.write(wave_compression,0,wave_compression.length);

     byte[] wave_channels = {2,0}; // LE 2 (16bit)
       bos.write(wave_channels,0,wave_channels.length);
   
       tmp = 44100;
       b1 = (byte) (tmp >> 24 & 0xff); b2 = (byte) (tmp >> 16 & 0xff);
       b3 = (byte) (tmp >> 8  & 0xff); b4 = (byte)   (tmp &     0xff); 
     byte[] wave_rate = {b4,b3,b2,b1}; // LE 4 (32bit)
       bos.write(wave_rate,0,wave_rate.length);
   
     //  rate*channels* bitsPerSample/8
       // tmp = 44100*2*16/2;
       tmp = 44100*2*16/8;
       b1 = (byte) (tmp >> 24 & 0xff); b2 = (byte) (tmp >> 16 & 0xff);
       b3 = (byte) (tmp >> 8  & 0xff); b4 = (byte)   (tmp &     0xff); 
     byte[] wave_bytesPerSec = {b4,b3,b2,b1}; // LE (32bit)
       bos.write(wave_bytesPerSec,0,wave_bytesPerSec.length);

     // bytes/sample * channels
     byte[] wave_alignment = {4,0}; // LE 2 (16bit) 
       bos.write(wave_alignment,0,wave_alignment.length);
     byte[] wave_bitsPerSample = {16,0}; // LE 2 (16bit) 
       bos.write(wave_bitsPerSample,0,wave_bitsPerSample.length);

     // data header (8)
     byte[] data_id = {'d','a','t','a'}; // 4
       bos.write(data_id,0,data_id.length);

      tmp = inputFile.length();
       b1 = (byte) (tmp >> 24 & 0xff); b2 = (byte) (tmp >> 16 & 0xff);
       b3 = (byte) (tmp >> 8  & 0xff); b4 = (byte)   (tmp &     0xff); 
     byte[] data_size = {b4,b3,b2,b1}; // LE 4
       bos.write(data_size,0,data_size.length);

     int ilen;
     while ((ilen = bis.read(buf)) >= 0){
         bos.write(buf,0,ilen);
     }

     bis.close(); 
     bos.flush(); bos.close();

     if (delete) if(!inputFile.delete()){
       System.err.println("file delete failed");}

   } catch (IOException e) {
     e.printStackTrace();
   } 

  }

  static public void waveToRawFile(String rawFile, String outFile,
      boolean delete){

     File inputFile  = new File(rawFile);
     File outputFile = new File(outFile);

   try {
     FileInputStream fis = new FileInputStream(inputFile);
     FileOutputStream fos = new FileOutputStream(outputFile);

     BufferedInputStream bis = new BufferedInputStream(fis);
     BufferedOutputStream bos = new BufferedOutputStream(fos);

    // read wave header info
/*
 * http://alvinalexander.com/java/jwarehouse/android/core/java/android/speech/srec/WaveHeader.java.shtml
 */     

     int ilen;
     byte[] buf = new byte[4096];
     ilen = bis.read(buf,0,44); // throw 44 bytes
     //  int read(byte[] b, int off, int len)

     while ((ilen = bis.read(buf)) >= 0){
         bos.write(buf,0,ilen);
     }

     bis.close(); 
     bos.flush(); bos.close();

    if (delete) 
       if(!inputFile.delete()){System.err.println("file delete failed");}
       } catch (IOException e) {
       e.printStackTrace();
       } 

  }

}
