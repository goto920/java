package jp.kmgoto.musicplayer;

import java.util.Arrays;
import java.util.ArrayList;
// import java.util.Collections;
// import org.apache.commons.lang.ArrayUtils;

public class StereoPercussionSplitter {

  public StereoPercussionSplitter(int datasize){ 
      // datasize: # of coefficients to be buffered (1 per 4096 samples)
    this.datasize = datasize;
    lowF = 30000f; highF = 0f; mix = 1f;
    inputBufferL = new ArrayList<>();
    inputBufferR = new ArrayList<>();
    position = -1; // not ready
    SAMPLING_RATE = 44100;
    filter = null;

    results = new float[2][2*WINDOW_SIZE]; 
      // real, imag * w * 2(LR)
//    results[R] = new float[2*WINDOW_SIZE]; // real, imag
    coefL = new float[2*(WINDOW_SIZE/2+1)]; // (half + 1)*real,imag
    coefR = new float[2*(WINDOW_SIZE/2+1)]; // (half + 1)*real,imag
    coefArrayL = new float[datasize][2*(WINDOW_SIZE/2+1)];
    coefArrayR = new float[datasize][2*(WINDOW_SIZE/2+1)];

    pMedian = new float[2][WINDOW_SIZE/2+1]; // hafl + 1 (real only)
//    pMedian[L] = new float[WINDOW_SIZE/2+1]; // hafl + 1 (real only)
//    pMedian[R] = new float[WINDOW_SIZE/2+1]; // hafl + 1 (real only)
    powerL = new float[datasize]; 
    powerR = new float[datasize]; 
    hMedian = new float[2][WINDOW_SIZE/2+1];
//    hMedian[L] = new float[WINDOW_SIZE/2+1];
//    hMedian[R] = new float[WINDOW_SIZE/2+1];
    state = STATE_NEEDMORE;
    sampleID = 0;

    pan = new float[WINDOW_SIZE/2+1];
    panAmp = new float[WINDOW_SIZE/2+1];
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

  private PanPercFilter filter;
  private int[][] verdictArray; 

  private boolean recordPeakOnly;
  public void setPanPercFilter(PanPercFilter filter, boolean recordPeakOnly){ 
    this.filter = filter; 
    filter.setFourierIndex(WINDOW_SIZE, SAMPLING_RATE);
//    verdictArray = filter.getVerdictArray();
      this.recordPeakOnly = recordPeakOnly;
  }

  public void setSamplingRate(int value){
    SAMPLING_RATE = value;
  }

  public void setBypass(boolean value){
     bypass = value; 
  } 

  // remix balance: 0(harmonic) ... 1(percussive)
  public void setMix(float mix){ this.mix = mix; }

/** Input Fourier coefficients for 1 window
 *  store Fourier coefficients in inputBuffer up to datasize
 *  returns Processed Fourier coefficients for 1 window
 *  null input denotes no more input available
 */ 
  private int position; 

  private int sampleID; // debug
  private final static int L=0;
  private final static int R=1;

  public int percussionSplit(float[] inOutL, float[] inOutR,
        boolean inputAvailable){ 

    // add data if exist
    if (inputAvailable){
       float[] inputL = new float[2*(WINDOW_SIZE/2+1)];
       float[] inputR = new float[2*(WINDOW_SIZE/2+1)];
       System.arraycopy(inOutL,0,inputL,0,2*(WINDOW_SIZE/2+1));
       System.arraycopy(inOutR,0,inputR,0,2*(WINDOW_SIZE/2+1));
       inputBufferL.add(inputL);
       inputBufferR.add(inputR);
       sampleID++;
    }

    float[] tmp;
    switch(state){
      case STATE_NEEDMORE:
       if (inputBufferL.size() >= datasize/2) {
         position = 0; state = STATE_INC; 
       } else position = 0;
       break;
     case STATE_INC:
       System.arraycopy(getResult(position)[L], 0, inOutL, 0, inOutL.length);
       System.arraycopy(getResult(position)[R], 0, inOutR, 0, inOutR.length);
       if (inputBufferL.size() < datasize) 
          position++;
       else state = STATE_FULL;
      break;
     case STATE_FULL:
       System.arraycopy(getResult(position)[L], 0, inOutL, 0, inOutL.length);
       System.arraycopy(getResult(position)[R], 0, inOutR, 0, inOutR.length);
       inputBufferL.remove(0); inputBufferR.remove(0);
       if (inputBufferL.size() < datasize) { state = STATE_DEC; }
      break;
     case STATE_DEC:
       int isize = inputBufferL.size();
       if (isize > datasize/2){
         System.arraycopy(getResult(position)[L], 0, inOutL, 0, inOutL.length);
         System.arraycopy(getResult(position)[R], 0, inOutR, 0, inOutR.length);
         inputBufferL.remove(0); inputBufferR.remove(0);
       } else if (isize > 0)  {
         position = isize - 1;
         System.arraycopy(getResult(position)[L], 0, inOutL, 0, inOutL.length);
         System.arraycopy(getResult(position)[R], 0, inOutR, 0, inOutR.length);
         inputBufferL.remove(0); inputBufferR.remove(0);
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
  private int SAMPLING_RATE;
  private boolean bypass;
  // data store
  private ArrayList<float[]> inputBufferL; 
  private ArrayList<float[]> inputBufferR; 

  private float[][] results;
  private float[] coefL, coefR;

  private float[][] getResult(int position){

   System.arraycopy(inputBufferL.get(position),0, coefL,0,2*(WINDOW_SIZE/2+1));
   System.arraycopy(inputBufferR.get(position),0, coefR,0,2*(WINDOW_SIZE/2+1));

   if (filter != null) calcPan(coefL, coefR);

   float[][] p = getPMedian(position); // median of power in freq axis
   float[][] h = getHMedian(position); // median of power in time axis

   int max2 = WINDOW_SIZE/2;
   for(int i = 0; i <= max2; i++){ 
      float ratioL=0f, ratioR=0f;
      float pSqL, pSqR, hSqL, hSqR;
        pSqL = p[L][i]*p[L][i]; hSqL = h[L][i]*h[L][i];
        pSqR = p[R][i]*p[R][i]; hSqR = h[R][i]*h[R][i];

     int verdict = PanPercFilter.SPLIT;

     if (recordPeakOnly){
       float percRatio =  
                (1 - pan[i])/2f*pSqL/(pSqL + hSqL) 
              + (1 + pan[i])/2f*pSqR/(pSqR + hSqR);
       filter.recordPeaks(i,pan[i],percRatio);
     } else 
       verdict = filter.getVerdict(i,pan[i]); 

      switch(verdict){
         case PanPercFilter.SPLIT:
           if (mix > 0.5f){ 
             ratioL = (hSqL + 2*(1-mix)*pSqL)/(pSqL + hSqL);
             ratioR = (hSqR + 2*(1-mix)*pSqR)/(pSqR + hSqR);
           } else { 
             ratioL = (pSqL + 2*mix*hSqL)/(pSqL + hSqL); 
             ratioR = (pSqR + 2*mix*hSqR)/(pSqR + hSqR); 
           }
         break;
        case PanPercFilter.THROUGH:
           ratioR = ratioL = 1f;
         break;
        case PanPercFilter.CANCEL:
           ratioR = ratioL = 0f;
          break;
        default:
      }

      results[L][2*i]   = ratioL*coefL[2*i]; // real
      results[L][2*i+1] = ratioL*coefL[2*i + 1]; // imag
      results[R][2*i]   = ratioR*coefR[2*i]; // real
      results[R][2*i+1] = ratioR*coefR[2*i + 1]; // imag

      if (i >= 1 && i < max2){
       int mirror = WINDOW_SIZE - i; 
       results[L][2*mirror] = results[L][2*i];
       results[L][2*mirror+1] = -results[L][2*i+1];
       results[R][2*mirror] = results[R][2*i];
       results[R][2*mirror+1] = -results[R][2*i+1];
      }

    }

    return results;
  }

// used in getHMedian()
  private float[][] coefArrayL, coefArrayR;
  private float[][] hMedian;
  private float[] powerL, powerR;

  private float[][] getHMedian(int position){

    int min  = Math.max(0, position - datasize/2);
    // int size = inputBuffer.size();
    int max  = Math.min(position + datasize/2 + 1, inputBufferL.size());

    for (int time = min; time < max; time++){
       System.arraycopy(inputBufferL.get(time),0, 
           coefArrayL[time], 0, WINDOW_SIZE + 1);
       System.arraycopy(inputBufferR.get(time),0, 
           coefArrayR[time], 0, WINDOW_SIZE + 1);
    }

    int max3 = WINDOW_SIZE/2 + 1;
    for (int freq = 0; freq < max3; freq++){

       int count = 0;
       for (int time = min; time < max; time++){
            powerL[count] = coefArrayL[time][2*freq]*coefArrayL[time][2*freq]
                 + coefArrayL[time][2*freq+1]*coefArrayL[time][2*freq+1];
            powerR[count] = coefArrayR[time][2*freq]*coefArrayR[time][2*freq]
                 + coefArrayR[time][2*freq+1]*coefArrayR[time][2*freq+1];
            count++;
       }
       hMedian[L][freq] = median(powerL,count);
       hMedian[R][freq] = median(powerR,count);
    }

    return hMedian;
  }

// Used in getPMedian
  private float[][] pMedian;

  private float[][] getPMedian(int position){

    System.arraycopy(inputBufferL.get(position),0, coefL,0, WINDOW_SIZE+1);
    System.arraycopy(inputBufferR.get(position),0, coefR,0, WINDOW_SIZE+1);
    
    int max = WINDOW_SIZE/2 + 1 ;
    for (int freq = 0; freq < max; freq++){
       int min2 = Math.max(0, freq - datasize/2);
       int max2 = Math.min(freq + datasize/2 + 1, max);
       int count = 0;
       for (int j = min2; j < max2; j++){
         powerL[count] = coefL[2*j]*coefL[2*j] + coefL[2*j+1]*coefL[2*j+1];
         powerR[count] = coefR[2*j]*coefR[2*j] + coefR[2*j+1]*coefR[2*j+1];
         count++;
       }  

       pMedian[L][freq] =  median(powerL,count); 
       pMedian[R][freq] =  median(powerR,count); 
    }

    return pMedian;
  }

  private static float median(float[] in, int size){
    Arrays.sort(in,0,size); // forgot to set from/toIndex
    if (size % 2 == 1) return in[size/2];
    else return (in[size/2-1] + in[size/2])/2.0f;
  }

  private float[] pan, panAmp;
  private void calcPan(float[] inL, float[] inR){

      // input float[] inL, inR (Fourier coefficient (real,imag) WINDOW_SIZE)
      // output float[] pan (WINDOW_SIZE);

      int max = pan.length - 1;

      for(int i=0; i <= max; i++){

        int base = 2*i, base1 = base + 1;

         float dotProd   =  inL[base]*inR[base] + inL[base1]*inR[base1];
         float crossProd = -inL[base]*inR[base1] + inL[base1]*inR[base];
         float absL
         = (float) Math.abs(
              Math.sqrt(inL[base]*inL[base] + inL[base1]*inL[base1]));
         float absR 
           = (float) Math.abs(
              Math.sqrt(inR[base]*inR[base] + inR[base1]*inR[base1])); 
         float absLR 
           = (float) Math.abs(Math.sqrt(
               (inL[base] - inR[base])*(inL[base] - inR[base])
               + (inL[base1] - inR[base1])*(inL[base1] - inR[base1]))
               );

         float frac;
         if (absL < absR) {
           if (dotProd < 0) {
              frac = 0f;
              panAmp[i] = absLR - absL; 
           } else if (dotProd <= absR*absR){ 
              frac = dotProd/(absR*absR);
              panAmp[i] = Math.max(absL, absLR) - Math.abs(dotProd)/absR;
           } else {
              frac = 1f;
              panAmp[i] = absL - absLR;
           }
           pan[i] = (1-frac)/(1+frac);
         } else { // absL >= absR
           if (dotProd < 0) {
             frac = 0f;
              panAmp[i] = absLR - absR;
           } else if (dotProd <= absL*absL){
             frac = dotProd/(absL*absL);
              panAmp[i] = Math.max(absR, absLR) - Math.abs(crossProd)/absL;
           } else {
             frac = 1f;
              panAmp[i] = absR - absLR;
           }
           pan[i] = (frac-1)/(1+frac);
         }

      } // end for i
    //  return;
   } // End calcPan

}
