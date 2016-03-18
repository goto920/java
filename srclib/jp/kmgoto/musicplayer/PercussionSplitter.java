package jp.kmgoto.musicplayer;

import java.util.Arrays;
import java.util.ArrayList;
// import java.util.Collections;
// import org.apache.commons.lang.ArrayUtils;

public class PercussionSplitter {

  public PercussionSplitter(int datasize){ 
      // datasize: # of coefficients to be buffered (1 per 4096 samples)
    this.datasize = datasize;
    lowF = 30000f; highF = 0f; mix = 1f;
    inputBuffer = new ArrayList<>();
    position = -1; // not ready
    SAMPLING_RATE = 44100;

    results = new float[2*WINDOW_SIZE]; // real, imag
    //  coef = new float[2*WINDOW_SIZE];
    coef = new float[2*(WINDOW_SIZE/2+1)]; // (half + 1)*real,imag
    coefArray = new float[datasize][2*(WINDOW_SIZE/2+1)];
    pMedian = new float[WINDOW_SIZE/2+1]; // hafl + 1 (real only)
    power = new float[datasize]; 
    hMedian = new float[WINDOW_SIZE/2+1];
    state = STATE_NEEDMORE;
    sampleID = 0;
  }

  public String name;

  // return value for percussionSplit()
  public static final int STATE_NEEDMORE = 0 ; // need more data
  public static final int STATE_INC   = 1 ; // initial increasing samples 
  public static final int STATE_FULL  = 2 ; // all samples available
  public static final int STATE_DEC   = 3 ; // last decreasing samples
// public static final int STATE_FLUSH = 4 ; // there are more output 
  public static final int STATE_FINISHED = -1 ; // finished

  public int state; 

  public void setSamplingRate(int value){
    SAMPLING_RATE = value;
  }

  public void setBypass(boolean value){
     bypass = value; 
  } 

/*
  private PanPercFilter filter;
  public void setPanPercFilter(PanPercFilter filter){
     this.filter = filter;
     filter.setFourierIndex(WINDOW_SIZE,SAMPLING_RATE);
  } 
*/

  // exclusion range in Hz. 
  // (lowF < x < highF) or (x < highF || lowF < x)
  public void setLowF(float lowF){ 
    this.lowF = lowF; 
    lowFIndex = (int) ((lowF*WINDOW_SIZE)/SAMPLING_RATE);
  } 
  public void setHighF(float highF){ 
    this.highF = highF; 
    highFIndex = (int) ((highF*WINDOW_SIZE)/SAMPLING_RATE);
  }

  // remix balance: 0(harmonic) ... 1(percussive)
  public void setMix(float mix){ this.mix = mix; }

/** Input Fourier coefficients for 1 window
 *  store Fourier coefficients in inputBuffer up to datasize
 *  returns Processed Fourier coefficients for 1 window
 *  null input denotes no more input available
 */ 
  private int position; 
  // private ArrayList<Float> input;

  private int sampleID; // debug
  public int percussionSplit(float[] inOut, boolean inputAvailable){ 

  /*
   if (name.equals("LEFT")){ 
    System.out.println("BEGIN: LEFT (sampleID, state, position, bufsize) = " 
         + sampleID + " " + state + " " + position + " " 
         + inputBuffer.size()); // debug 
   }
  */

    // add data if exist
    if (inputAvailable){
       float[] input = new float[2*(WINDOW_SIZE/2+1)];
       // int len = inOut.length;
       System.arraycopy(inOut,0,input,0,2*(WINDOW_SIZE/2+1));
       inputBuffer.add(input);
       sampleID++;
    }

    float[] tmp;
    switch(state){
      case STATE_NEEDMORE:
       if (inputBuffer.size() >= datasize/2) {
         position = 0; state = STATE_INC; 
       } else position = 0;
       break;
     case STATE_INC:
       System.arraycopy(getResult(position),0,inOut,0,inOut.length);
       if (inputBuffer.size() < datasize) 
          position++;
       else state = STATE_FULL;
      break;
     case STATE_FULL:
       System.arraycopy(getResult(position), 0, inOut, 0, inOut.length);
       inputBuffer.remove(0);
       if (inputBuffer.size() < datasize) { state = STATE_DEC; }
      break;
     case STATE_DEC:
       int isize = inputBuffer.size();
       if (isize > datasize/2){
         System.arraycopy(getResult(position), 0, inOut, 0, inOut.length);
         inputBuffer.remove(0);
       } else if (isize > 0)  {
         position = isize - 1;
         System.arraycopy(getResult(position), 0, inOut, 0, inOut.length);
         inputBuffer.remove(0);
       } else state = STATE_FINISHED;
      break;
     case STATE_FINISHED:
   }

   return state;
  }

  private int datasize;
  private float lowF, highF;
  private float mix;
  private int lowFIndex, highFIndex;
  private final int WINDOW_SIZE = 4096;
//  private final int SAMPLING_RATE = 44100;
  private int SAMPLING_RATE;
  private boolean bypass;
  // data store
  private ArrayList<float[]> inputBuffer; 

  private float[] results;
  private float[] coef;

  private float[] getResult(int position){

   System.arraycopy(inputBuffer.get(position),0,
                    coef,0,2*(WINDOW_SIZE/2+1));

   int max2 = WINDOW_SIZE/2;
   if (bypass){
     // i=0 DC (real,imag), i=w/2 Center, i=w-i conjecture
     for(int i = 0; i <= max2; i++){ 
        results[2*i] = coef[2*i];  
        results[2*i+1] = coef[2*i+1]; 
        if (i >= 1 && i < max2){ // mirror conjecture
          int mirror = WINDOW_SIZE - i;
          results[2*mirror] = coef[2*i]; 
          results[2*mirror+1] = -coef[2*i+1];
        }
     }
     return results;
   }

   float[] p = getPMedian(position); // median of power in freq axis
   float[] h = getHMedian(position); // median of power in time axis

   for(int i = 0; i <= max2; i++){ 
      float ratio;
      float pSq = p[i]*p[i], hSq = h[i]*h[i];

      if (mix > 0.5f){ 
         //ratio = (hSq + 2*(1f - mix)*pSq)/(pSq + hSq);
         ratio = hSq/(pSq + hSq);
      } else { 
         ratio = (pSq + 2*mix*hSq)/(pSq + hSq); 
      }

      if (lowFIndex < highFIndex){
          if (i > lowFIndex && i < highFIndex) ratio = 1f;
      } else {
          if (i <= lowFIndex || i >= highFIndex) ratio = 1f;
      }

      results[2*i]   = ratio*coef[2*i]; // real
      results[2*i+1] = ratio*coef[2*i + 1]; // imag

      if (i >= 1 && i < max2){
       int mirror = WINDOW_SIZE - i; 
       results[2*mirror] = results[2*i];
       results[2*mirror+1] = -results[2*i+1];
      }

    }

    return results;
  }

// used in getHMedian()
  private float[][] coefArray;
  private float[] hMedian;
  private float[] power;

  private float[] getHMedian(int position){

    int min  = Math.max(0, position - datasize/2);
    // int size = inputBuffer.size();
    int max  = Math.min(position + datasize/2 + 1, inputBuffer.size());

    for (int time = min; time < max; time++)
       System.arraycopy(inputBuffer.get(time),0, 
           coefArray[time], 0, WINDOW_SIZE + 1);

    int max3 = WINDOW_SIZE/2 + 1;
    for (int freq = 0; freq < max3; freq++){

       int count = 0;
       for (int time = min; time < max; time++){
            power[count] = coefArray[time][2*freq]*coefArray[time][2*freq]
                 + coefArray[time][2*freq+1]*coefArray[time][2*freq+1];
            count++;
       }
       hMedian[freq] = median(power,count);
    }

    return hMedian;
  }

// Used in getPMedian
  private float[] pMedian;

  private float[] getPMedian(int position){

    System.arraycopy(inputBuffer.get(position),0, coef,0, WINDOW_SIZE+1);
    
    int max = WINDOW_SIZE/2 + 1 ;
    for (int freq = 0; freq < max; freq++){
       int min2 = Math.max(0, freq - datasize/2);
       int max2 = Math.min(freq + datasize/2 + 1, max);
       int count = 0;
       for (int j = min2; j < max2; j++){
         power[count] = coef[2*j]*coef[2*j] + coef[2*j+1]*coef[2*j+1];
         count++;
       }  

       pMedian[freq] =  median(power,count); 
    }

    return pMedian;
  }

  private static float median(float[] in, int size){
    Arrays.sort(in,0,size); // forgot to set from/toIndex
    if (size % 2 == 1) return in[size/2];
    else return (in[size/2-1] + in[size/2])/2.0f;
  }

}
