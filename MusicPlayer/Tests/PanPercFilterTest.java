//package jp.kmgoto.musicplayer;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import jp.kmgoto.musicplayer.PanPercFilter;

public class PanPercFilterTest {

/*
  public PanPercFilterTest() {
  }
*/

  public static void main(String[] args){

    PanPercFilter filter = new PanPercFilter();

// Load test 
    filter.loadContent("test.flt"); // file created by GridButtons
    filter.printContent();

/* 
// Data input test
    int y = 23;
    float[] freqRanges = new float[y];

    for (int f =0; f < y; f++)
      freqRanges[f] = (float) (13.75*Math.pow(2.0,6*f/12f));

    int x = 21;
    filter.setFreqRanges(freqRanges);
    filter.setFourierIndex(4096,44100);
    filter.setNumPanRanges(x);
*/

  } // end main

}
