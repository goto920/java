package jp.kmgoto.musicplayer;

import java.io.*;

public class PanPercFilter {
    public static final int THROUGH = 0;
    public static final int CANCEL  = 1;
    public static final int SPLIT   = 2;

    private float[] freqRanges;
    private int numPanRanges;
    private String[][] data; 
    private int[] findex; // fourierIndex to data index
    private int windowSize;
    private float[][] percPeaks;
    private boolean recordPeaks;

    public PanPercFilter(){
       windowSize = 4096;
       recordPeaks = false;
    }

    public void enableRecord(){
      recordPeaks = true;
    }

    public void setFreqRanges(float[] ranges){
       freqRanges = ranges; 
    }

    public void setNumPanRanges(int num){
      numPanRanges = num;
      if (recordPeaks){ 
        percPeaks = new float[windowSize/2+1][numPanRanges];
        for (int freqIndex =0; freqIndex <= windowSize/2; freqIndex++)
          for (int panIndex =0; panIndex < numPanRanges; panIndex++)
            percPeaks[freqIndex][panIndex] = 0f;
 
      }
    }
   
    public void setData(String[][] inputData){
       data = inputData;
    }

    public void setFourierIndex(int windowSize, int samplingRate){
      this.windowSize = windowSize;
      findex = new int[windowSize/2+1];

      int index = 0;
      for (int i=0; i < windowSize/2 + 1; i++){
         float freq = (i*samplingRate)/(float) windowSize;
         if (freqRanges[index] > freq) {
           findex[i] = index;
         } else {
           findex[i] = index++;
         }
      }

    }

    public int[][] getVerdictArray(){

       int[][] retval = new int[windowSize/2 + 1][numPanRanges];

       for(int freq=0; freq <= freqRanges.length; freq++){
          int i = findex[freq]; // fourierIndex to data index
          for(int pan = 0; pan < numPanRanges; pan++){
           if (data[i][pan].equals("t")){
              retval[freq][pan] = THROUGH;
           } else if (data[i][pan].equals("c")){
              retval[freq][pan] = CANCEL;
           } else if (data[i][pan].equals("s")){
              retval[freq][pan] = SPLIT;
           }
          } // end for
       } // end for freq

       return retval;
    }

    public int getVerdict(int freqIndex, float pan){

       int i = findex[freqIndex]; // fourierIndex to data index
       int panIndex = ((int) ((pan + 1f)*(numPanRanges-1)) + 1)/2;
       if (data[i][panIndex].equals("t")){
           return THROUGH;
       } else if (data[i][panIndex].equals("c")){
           return CANCEL;
       } else if (data[i][panIndex].equals("s")){
           return SPLIT;
       } 

       return THROUGH;
    }

    public void recordPeaks(int freqIndex, float pan, float percRatio){
//       System.err.println(freqIndex +" " +  pan + " " + percRatio);
       if(!recordPeaks) return;
       int panIndex = ((int) ((pan + 1f)*(numPanRanges-1)) + 1)/2;
       if (percRatio > percPeaks[freqIndex][panIndex])
               percPeaks[freqIndex][panIndex] = percRatio;
    }

    public void printPeaks(){
      if (!recordPeaks) return;

      for (int panIndex =0; panIndex < numPanRanges; panIndex++)
        for (int freqIndex =0; freqIndex <= windowSize/2; freqIndex++)
           System.out.println(
             panIndex + ", " + freqIndex + ", " 
               + percPeaks[freqIndex][panIndex]);
    }

    public void printContent(){
      System.err.println("#BEGIN freqRanges: " + freqRanges.length);

      for (int f = 0; f < freqRanges.length; f++){
        System.err.println(f + " " + freqRanges[f]);
      }

      System.err.println("#END freqRanges");
      System.err.println("#INFO numPanRanges: " + numPanRanges);
      System.err.println("#BEGIN data: freq pan value(str)");

      for (int f = 0; f < freqRanges.length; f++){
        for (int p = 0; p < numPanRanges; p++){
          System.err.println(f + " " + p + " " + data[f][p]);
        }
      }
      System.err.println("#END data");

    }

    public void saveContent(String outputFilePath){

    try {
      FileWriter fw = new FileWriter(outputFilePath); 

      fw.write("#BEGIN freqRanges: " + freqRanges.length + "\n");

      for (int f = 0; f < freqRanges.length; f++){
        fw.write(f + " " + freqRanges[f] + "\n");
      }

      fw.write("#END freqRanges: " + freqRanges.length + "\n");
      fw.write("#INFO numPanRanges: " + numPanRanges + "\n");
      fw.write("#BEGIN data: freq pan value(str)\n");

      for (int f = 0; f < freqRanges.length; f++){
        for (int p = 0; p < numPanRanges; p++){
          fw.write(f + " " + p + " " + data[f][p] + "\n");
        }
      }
      fw.write("#END data\n");
      fw.close();
     } catch(Exception e){e.printStackTrace();}

   }

   public void loadContent(String inputFilePath){
    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader(inputFilePath); 
      br = new BufferedReader(fr);
      String line;
      int state = 0;
      String[] var;
      while ((line = br.readLine()) != null){
     //   System.out.println(line);
        if (line.startsWith("#BEGIN freqRanges:")) { 
            var = line.split("\\s+");
            freqRanges = new float[Integer.parseInt(var[2])];
     //       System.out.println("Set # of freqRanges " + freqRanges.length);
            state = 1; 
            continue; 
        }
        if (line.startsWith("#INFO numPanRanges:")) { 
            var = line.split("\\s+");
            numPanRanges = Integer.parseInt(var[2]);
     //       System.out.println("Set numPanRanges " + numPanRanges);
            state = 1; 
            continue; 
        }

        if (line.startsWith("#BEGIN data:")) { 
          data = new String[freqRanges.length][numPanRanges];
          state = 2; 
          continue;
        }
        if (line.startsWith("#END")) { continue; }

        var = line.split("\\s+");
        switch(state){
          case 1: // read freqRanges
           freqRanges[Integer.parseInt(var[0])] = Float.parseFloat(var[1]);
           break;
          case 2: // read data
           data[Integer.parseInt(var[0])][Integer.parseInt(var[1])] = var[2];
           break;
          default:
        }
      }
    } catch(Exception e) { 
        e.printStackTrace();
    } finally {
       try {
         br.close(); fr.close();
       } catch (Exception e) {e.printStackTrace();}
    }
   }

}
