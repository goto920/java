//package jp.kmgoto.musicplayer;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import jp.kmgoto.musicplayer.FFTEffector;
import jp.kmgoto.musicplayer.PanCanceler;
import jp.kmgoto.musicplayer.PercussionSplitter;
import jp.kmgoto.musicplayer.StereoPercussionSplitter;
import jp.kmgoto.musicplayer.PanPercFilter;
import jp.kmgoto.musicplayer.swing.ConvertPlayThread;

public class ConvertPlayTest {

  public static void main(String[] args){

    File inputFile  = new File(args[0]);
    File outputFile = new File("tmp.raw");
    int nargs = args.length;
    if (nargs < 2){
     System.err.println(args[0] 
        + " FFTTEST/PAN/PERC/SPERC " + " EXTRACT "); 
     return;
    }

    try {
      FileInputStream fis = new FileInputStream(inputFile);
//      FileOutputStream fos = new FileOutputStream(outputFile);

      BufferedInputStream bis = new BufferedInputStream(fis);
//      BufferedOutputStream bos = new BufferedOutputStream(fos);
//      BufferedOutputStream bos = new BufferedOutputStream(System.out);
     OutputStream bos = System.out;

     byte[] buf = new byte[4096]; // should work for any byte length

     // process loop here
     int ilen;

// Test effectors
   FFTEffector ef = null;
  PanPercFilter filter = null;
   switch(args[1]){
     case "FFTTEST":
       ef = new FFTEffector(FFTEffector.TYPE_FFTTEST); // OK (3/1)
       break;
     case "PAN":
      filter = new PanPercFilter();
      filter.loadContent("extract.flt");

      ef = new FFTEffector(FFTEffector.TYPE_PANCANCEL);
      ef.setSamplingRate(44100);
      ef.setPanPercFilter(filter);

      System.err.println("PAN filter");
/*
      ef.setParam(FFTEffector.OP_PAN, 0f);
      ef.setParam(FFTEffector.OP_WIDTH, 0.3f);
      ef.setCancel(true);
      if (nargs >2 && args[2].equals("EXTRACT")) ef.setCancel(false);
      ef.setParam(FFTEffector.OP_LOWF,  220f);
      ef.setParam(FFTEffector.OP_HIGHF, 3400f);
*/
      break;
    case "PERC":
     ef = new FFTEffector(FFTEffector.TYPE_PERCSPLIT); // OK??
     ef.setBypass(false);
     if (nargs > 2 && args[2].equals("BYPASS")) ef.setBypass(true);
     ef.setParam(FFTEffector.OP_LOWF,  800f);
     ef.setParam(FFTEffector.OP_HIGHF, 3400f);
     ef.setParam(FFTEffector.OP_MIX, 1f);
     break;
    case "SPERC":
     System.err.println("SPERC");
     filter = new PanPercFilter();
     filter.loadContent("split.flt");
     ef = new FFTEffector(FFTEffector.TYPE_STEREO_PERCSPLIT); // OK??
     ef.setPanPercFilter(filter);
     ef.setSamplingRate(44100);
     ef.setParam(FFTEffector.OP_MIX, 1f);
     break;
    default:
      System.err.println("No match for Effect type");
      System.err.println("FFTTEST/PAN/PERC");
      return;
    }

   ConvertPlayThread cpt = new ConvertPlayThread(args[0],null,ef);
   cpt.start();

   } catch (IOException e) { e.printStackTrace(); } 

   System.err.println("main return");
   return;
  } // end main

}
