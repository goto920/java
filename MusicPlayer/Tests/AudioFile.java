//package jp.kmgoto.musicplayer;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import jp.kmgoto.musicplayer.FFTEffector;
import jp.kmgoto.musicplayer.PanCanceler;
import jp.kmgoto.musicplayer.PercussionSplitter;
import jp.kmgoto.musicplayer.Utils;

public class AudioFile {

  public AudioFile() {
  }

  public static void main(String[] args){

    File inputFile  = new File(args[0]);
    File outputFile = new File("tmp.raw");
    int nargs = args.length;
    if (nargs < 2){
     System.out.println(args[0] 
        + " FFTTEST/PAN/PERC " + " EXTRACT/BYPASS "); 
     return;
    }

    try {
      FileInputStream fis = new FileInputStream(inputFile);
      FileOutputStream fos = new FileOutputStream(outputFile);

      BufferedInputStream bis = new BufferedInputStream(fis);
      BufferedOutputStream bos = new BufferedOutputStream(fos);

     byte[] buf = new byte[4096]; // should work for any byte length

     // process loop here
     int ilen;

// Test effectors
   FFTEffector ef;
   switch(args[1]){
     case "FFTTEST":
       ef = new FFTEffector(FFTEffector.TYPE_FFTTEST); // OK (3/1)
       break;
     case "PAN":
      ef = new FFTEffector(FFTEffector.TYPE_PANCANCEL);
      ef.setParam(FFTEffector.OP_PAN, 0f);
      ef.setParam(FFTEffector.OP_WIDTH, 0.3f);
      ef.setCancel(true);
      if (nargs >2 && args[2].equals("EXTRACT")) ef.setCancel(false);
      ef.setParam(FFTEffector.OP_LOWF,  220f);
      ef.setParam(FFTEffector.OP_HIGHF, 3400f);
      break;
    case "PERC":
     ef = new FFTEffector(FFTEffector.TYPE_PERCSPLIT); // OK??
     ef.setBypass(false);
     if (nargs > 2 && args[2].equals("BYPASS")) ef.setBypass(true);
     ef.setParam(FFTEffector.OP_LOWF,  800f);
     ef.setParam(FFTEffector.OP_HIGHF, 3400f);
     ef.setParam(FFTEffector.OP_MIX, 1f);
     break;
    default:
      System.out.println("No match for Effect type");
      System.out.println("FFTTEST/PAN/PERC");
      return;
   }

     int ret;
     ArrayList<Byte> obuf = new ArrayList<Byte>();
     byte[] out = new byte[4096]; // any value should work

     long nwritten = 0;
     while ((ilen = bis.read(buf)) >= 0){

       if (ilen == 0) {
         System.out.println("No input");
         continue;
       }

       ret = ef.process(buf,ilen,obuf);
//       System.out.println("AudioFile ret(ef) = " + ret);

       if (ret > 0){
         for (int i=0; i < ret; i++) out[i] = (byte) obuf.get(i);
         bos.write(out,0,ret); 
         obuf.clear();
       }

     }
     // End of input and get the last samples
     bis.close(); bis = null;

     System.out.println("Input end: ");
     while ((ret = ef.process(null,-1,obuf)) > 0){
       System.out.println("got(ret) " + ret);
       for (int i=0; i < ret; i++) out[i] = obuf.get(i);
       bos.write(out,0,ret); 
       obuf.clear();
     }

     ef = null; bos.close();

     // System.out.println("effect processing complete");
     System.out.println("Write to Wave file");
     Utils.rawToWaveFile("tmp.raw","out.wav", true); 
        // true for delete input file
     System.out.println("Done");

   } catch (IOException e) { e.printStackTrace(); } 

   System.out.println("main return");
   return;
  } // end main

}
