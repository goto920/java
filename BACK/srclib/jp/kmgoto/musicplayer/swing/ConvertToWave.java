package jp.kmgoto.musicplayer.swing;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import javax.swing.*;
import javax.sound.sampled.*;
// import java.util.Arrays;
import javazoom.jl.converter.*;
import javazoom.jl.decoder.JavaLayerException;
//
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;
import net.sourceforge.jaad.util.wav.WaveFileWriter;
import java.io.RandomAccessFile;
import java.util.List;

public class ConvertToWave {

  static public int anyToWaveFile(String in, String out){
  // returns sampling rate (44100 or 48000), -1 for error

    if (in.toUpperCase().endsWith(".WAV")){
      return waveToWaveFile(in,out,false);
    } else if (in.toUpperCase().endsWith(".MP3")){
      return mp3ToWaveFile(in,out);
    } else if (in.toUpperCase().endsWith(".MP4") 
        || in.toUpperCase().endsWith(".M4A")){
      return mp4ToWaveFile(in, out);
    } else if (in.toUpperCase().endsWith(".AAC")){
      return aacToWaveFile(in, out);
    } 

    return -1;

  }

  static public int mp4ToWaveFile(String in, String out){
    try {
       return decodeMP4(in, out);
    } catch (Exception e){ 
       System.err.println("error while decoding: "+e.toString());
    } 

    return -1;
  }

  static public int aacToWaveFile(String in, String out){
    try {
      return decodeAAC(in, out); 
   } catch (Exception e){ 
       System.err.println("error while decoding: "+e.toString());
   } 

    return -1;
  }

  static public int mp3ToWaveFile(String mp3File, String outFile){
   // returns sampling rate, -1 for error

    Converter converter = new Converter();
    int rate = -1;
    AudioInputStream ain = null;

    try {
      converter.convert(mp3File, outFile); 
      ain = AudioSystem.getAudioInputStream(new File(outFile));
      AudioFormat inputFormat = ain.getFormat();
      rate = (int) inputFormat.getSampleRate();
    } catch (Exception e) { 
       e.printStackTrace();
       rate = -1;
    } 

    try { 
      if (ain!=null) ain.close();
    } catch (Exception e){ e.printStackTrace();}

    return rate;
  }

  static public boolean rawToWaveFile(String rawFile, String outFile,
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
       bos.write(riff_size,0,riff_size.length);
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

     if (delete) 
       if(!inputFile.delete()){
        System.err.println("file delete failed");}

   } catch (IOException e) {
     e.printStackTrace();
   } 

     return true;
  }

  static public boolean waveToRawFile(String rawFile, String outFile,
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
       return false;
    } 

    return true;

  }

   private static int decodeMP4(String in, String out) throws Exception {
   // return sampling rate
     WaveFileWriter wav = null;
     int samplingRate = -1;
     try {
          final MP4Container cont = new MP4Container(
                   new RandomAccessFile(in, "r"));
          final Movie movie = cont.getMovie();
	  final List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);

          if(tracks.isEmpty()) 
            throw new Exception("movie does not contain any AAC track");

	 final AudioTrack track = (AudioTrack) tracks.get(0);
         samplingRate = track.getSampleRate();
	 wav = new WaveFileWriter(
               new File(out), track.getSampleRate(), 
               track.getChannelCount(), track.getSampleSize());
         // write in BE

         final Decoder dec = new Decoder(track.getDecoderSpecificInfo());

	  Frame frame;
	  final SampleBuffer buf = new SampleBuffer();
	  while(track.hasMoreFrames()) {
	        frame = track.readNextFrame();
	        dec.decodeFrame(frame.getData(), buf);
		wav.write(buf.getData());
	  }

          // if(dec!=null) dec.release();

      } finally { if(wav!=null) wav.close(); }

        return samplingRate;
    }

    private static int decodeAAC(String in, String out) throws IOException {

    WaveFileWriter wav = null;
    int sampleRate = -1;

    try {
      final ADTSDemultiplexer adts 
         = new ADTSDemultiplexer(new FileInputStream(in));
      final Decoder dec = new Decoder(adts.getDecoderSpecificInfo());

      final SampleBuffer buf = new SampleBuffer();
      byte[] b;

       while(true) {
          b = adts.readNextFrame();
	  dec.decodeFrame(b, buf);

        if(wav==null){ 
         wav = new WaveFileWriter(new File(out), buf.getSampleRate(), 
         buf.getChannels(), buf.getBitsPerSample());
         sampleRate = buf.getSampleRate();
        }

         wav.write(buf.getData());
       }
     } catch (Exception e) {
       e.printStackTrace();
     } finally { if(wav!=null) wav.close();}

     return sampleRate;
   }

  static public int waveToWaveFile(String in, String out, boolean delete) { 
  // any stereo 24bit wav to 16 bit stereo 

   File inputFile  = new File(in);
   File outputFile = new File(out);
   int rate = -1;
   FileInputStream fis = null;
   BufferedInputStream bis = null;

   FileOutputStream fos = null;
   BufferedOutputStream bos = null;

   try {
     fis = new FileInputStream(inputFile);
     bis = new BufferedInputStream(fis);

/*
     fos = new FileOutputStream(outputFile);
     bos = new BufferedOutputStream(fos);
*/

     // read headers
/*
  http://alvinalexander.com/java/jwarehouse/android/
  core/java/android/speech/srec/WaveHeader.java.shtml
*/
     byte b1=0, b2=0, b3=0, b4=0;
     byte[] riff_id = {'R','I','F','F'}; // 4
       bis.read(riff_id,0,riff_id.length);
 //      System.out.println("riff-id: " + new String(riff_id));
     byte[] riff_size = {b4,b3,b2,b1}; // LE 4 (32bit)
       bis.read(riff_size,0,riff_size.length);
//       System.out.println("riff-size: " + LEbytesToInt(riff_size));
     byte[] riff_type = {'W','A','V','E'}; // 4
       bis.read(riff_type,0,riff_type.length);
//       System.out.println("type: " + new String(riff_type));

     // Wave header (20+ bytes)
     byte[] wave_id = {'f','m','t',' '}; // 4
       bis.read(wave_id,0,wave_id.length);
//       System.out.println("riff-fmt: " + new String(wave_id));
     byte[] wave_size = {b4,b3,b2,b1}; // LE 4 (32bit) header size(16)
       bis.read(wave_size,0,wave_size.length);
//       System.out.println("wave-size: " + LEbytesToInt(wave_size));
     byte[] wave_compression = {1,0}; // LE 2 (16bit)
       bis.read(wave_compression,0,wave_compression.length);
//       System.out.println("compression: " + LEbytesToInt(wave_compression));
     byte[] wave_channels = {2,0}; // LE 2 (16bit)
       bis.read(wave_channels,0,wave_channels.length);
//       System.out.println("channels: " + LEbytesToInt(wave_channels));
     byte[] wave_rate = {b4,b3,b2,b1}; // LE 4 (32bit)
       bis.read(wave_rate,0,wave_rate.length);
//       System.out.println("rate: " + LEbytesToInt(wave_rate));
     byte[] wave_bytesPerSec = {b4,b3,b2,b1}; // LE (32bit)
       bis.read(wave_bytesPerSec,0,wave_bytesPerSec.length);
 //      System.out.println("Bps: " + LEbytesToInt(wave_bytesPerSec));
      // bytes/sample * channels
     byte[] wave_alignment = {4,0}; // LE 2 (16bit) 
       bis.read(wave_alignment,0,wave_alignment.length);
//       System.out.println("alignment: " + LEbytesToInt(wave_alignment));
     byte[] wave_bitsPerSample = {16,0}; // LE 2 (16bit) 
       bis.read(wave_bitsPerSample,0,wave_bitsPerSample.length);
//       System.out.println("bitsPerSample: " + LEbytesToInt(wave_bitsPerSample));

     int extension = 0;
     if (LEbytesToInt(wave_size) > 16){
         byte[] extended_size = new byte[2]; 
         bis.read(extended_size,0,extended_size.length);
         extension = LEbytesToInt(extended_size);
//         System.out.println("extended " + extension);
         if (extension > 0){
            byte[] rest = new byte[extension];
            bis.read(rest,0,rest.length);
         }
     }

     // data header
    int nread = 0;
    boolean found = false;
    byte[] data_id = {'d','a','t','a'}; // 4
    byte[] data_size = {b4,b3,b2,b1}; // LE 4

    while(nread >= 0 && !found){
       nread = bis.read(data_id,0,data_id.length);
       String chunk_id = new String(data_id);
//       System.out.println("data-id: " + chunk_id);
       nread = bis.read(data_size,0,data_size.length);
 //      System.out.println("size: " + LEbytesToInt(data_size));

      if (chunk_id.equals("data")) {
        found = true;
      } else {
        byte[] chunk = new byte[LEbytesToInt(data_size)];
        nread = bis.read(chunk,0, chunk.length); 
      }
    } // end while

   if (!found) {
//     System.out.println("data chunk not found");
     return -1;
   }

   int inputBitsPerSample = LEbytesToInt(wave_bitsPerSample);

   if (inputBitsPerSample == 16){ 
       bis.close();fis.close();
       return 0; // no conversion necessary
   }

   int datalen = LEbytesToInt(data_size);
   if (inputBitsPerSample == 24) datalen = (datalen*2)/3; 
//   System.out.println("Output datalen = " + datalen);


   fos = new FileOutputStream(outputFile);
   bos = new BufferedOutputStream(fos);

   // write headers
   bos.write(riff_id,0,riff_id.length);
      riff_size = intToLEbytes(datalen + 36, 4); 
       // basic header 44bytes - 8(id and size) 
   bos.write(riff_size,0,riff_size.length); 
   bos.write(riff_type,0,riff_type.length);
   bos.write(wave_id,0,wave_id.length);
      wave_size = intToLEbytes(16,4); // no extension
   bos.write(wave_size,0,wave_size.length);
      wave_compression = intToLEbytes(1,2);
   bos.write(wave_compression,0,wave_compression.length);
   bos.write(wave_channels,0,wave_channels.length);
   bos.write(wave_rate,0,wave_rate.length);
       rate = LEbytesToInt(wave_rate); 
       wave_bytesPerSec = intToLEbytes((rate*2*16)/8,4);
   bos.write(wave_bytesPerSec,0,wave_bytesPerSec.length);
       wave_alignment = intToLEbytes(4,2); // LE 2 (16bit) 
   bos.write(wave_alignment,0,wave_alignment.length);
       wave_bitsPerSample = intToLEbytes(16,2); // LE 2 (16bit) 
   bos.write(wave_bitsPerSample,0,wave_bitsPerSample.length);
   bos.write(data_id,0,data_id.length);
       data_size = intToLEbytes(datalen,4);
   bos.write(data_size,0,data_size.length);

     int ilen;
     byte[] buff = new byte[3]; 

     while ((ilen = bis.read(buff)) >= 0){ bos.write(buff,1,2); }
     
   } catch (Exception e){
     e.printStackTrace();
   } 

   try {
     if (bis !=null) bis.close();
     if (bos !=null) bos.close();
     if (fis !=null) fis.close();
     if (fos !=null) fos.close();
   } catch (Exception e){
     e.printStackTrace();
   } 

   return rate;
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

}
