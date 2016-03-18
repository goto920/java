package jp.kmgoto.musicplayer;

// import android.util.Log;

// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.List;

public class PanCanceler {

    private float panCenter, width;
    private int lowFIndex, highFIndex; 
    private boolean cancel, bypass; // cancel/extract bypass effect
    private float[] pan, panAmp;

    private final int WINDOW_SIZE = 4096;
//    private final int SAMPLING_RATE = 44100;
    private int SAMPLING_RATE;
    // private final int SAMPLE_SIZE = 2; // octets sample
    // private final int CHANNELS = 2; // channels

    // return value for STATE
    static final int S_READY = 0;
    // static final int S_FINISHED = 1;

    public PanCanceler() {
      pan = new float[WINDOW_SIZE/2+1];
      panAmp = new float[WINDOW_SIZE/2+1];
      setPan(0f); setWidth(0.2f); setLowF(220f); setHighF(4000f);
      cancel = true; bypass = false;
      SAMPLING_RATE = 44100;
      filter = null;
    }

    PanPercFilter filter;
    public void setPanPercFilter(PanPercFilter filter) { 
      this.filter = filter;
      filter.setFourierIndex(WINDOW_SIZE, SAMPLING_RATE);
      System.err.println("PanCanceler: Advanced Filter set");
    }

    public void setSamplingRate(int rate) {
      SAMPLING_RATE = rate;
    }

    public void setLowF(float lowF) {
       lowFIndex = (int) ((lowF*WINDOW_SIZE)/SAMPLING_RATE);
    }

    public void setHighF(float highF) {
       highFIndex = (int) ((highF*WINDOW_SIZE)/SAMPLING_RATE);
    }

    public void setWidth(float width) {
       this.width = width;
    }

    public void setPan(float pan) {
       panCenter = pan;
    }

    public void setCancel(boolean op) { cancel = op;}

    public void setBypass(boolean op) { bypass = op; }

    public int processOneWindow(float[] inL, float[] inR) {

       // System.out.println(bypass);
       if (!bypass) {
         calcPan(inL, inR);

         if (filter == null) {
           // System.out.println("panFilter()");
           panFilter(inL, inR);
         } else {
           // System.out.println("advancedFilter()");
           advancedFilter(inL,inR);
         }

         return S_READY; 
       }

       return S_READY;
    }

    private void panFilter(float[] inL, float[] inR){

      // boolean range;
      // if(lowFIndex <= highFIndex) range = true; else range = false;
       boolean range = lowFIndex <= highFIndex;

      int len   = pan.length - 1;
      float min = panCenter - width/2; 
      float max = panCenter + width/2; 

      if (cancel) {

       for (int i = 0; i <= len; i++){ // DC for i =0

         if (pan[i]  < min || pan[i]  > max) continue; 

         int base = 2*i, base1 = base + 1;
         int mirror =2*(WINDOW_SIZE - i), mirror1=mirror+1;

         if (range) {

          if (i >= lowFIndex && i <= highFIndex) { 
              inL[base] = inL[base1] = inR[base] = inR[base1] = 0f;
            if (i >= 1 && i < len)
              inL[mirror] = inL[mirror1] = inR[mirror] = inR[mirror1] = 0f;
          }

         } else { // not range

          if (i < lowFIndex || i > highFIndex) { 
             inL[base] = inL[base1] = inR[base] = inR[base1] = 0f;
             if (i >= 1 && i < len)
             inL[mirror] = inL[mirror1] = inR[mirror] = inR[mirror1] = 0f;
          }

        } // end if range 

      } // end for

     } else { // extract

       for (int i = 0; i <= len; i++){
          int base  = 2*i, base1 = base + 1;
          int mirror =2*(WINDOW_SIZE - i), mirror1=mirror+1;

         if (pan[i]  < min || pan[i] > max) 
            inL[base] = inL[base1] = inR[base] = inR[base1] = 0f;

         if(range){

           if (i < lowFIndex || i > highFIndex) { 
              inL[base] = inL[base1] = inR[base] = inR[base1] = 0f;
             if (i >= 1 && i < len)
               inL[mirror] = inL[mirror1] = inR[mirror] = inR[mirror1] = 0f;
           }

         } else {

           if (i >= lowFIndex && i <= highFIndex) { 
              inL[base] = inL[base1] = inR[base] = inR[base1] = 0f;
            if (i >= 1 && i < len) 
              inL[mirror] = inL[mirror1] = inR[mirror] = inR[mirror1] = 0f;
           }

         } // end if range

       } // end for

      } // end if cancel

     // return;

    } // End panFilter

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

   private void advancedFilter(float[] inL, float[] inR){

     // System.out.println("advancedFilter()");

     for (int freq = 0; freq <= pan.length - 1; freq++){
        int base = 2*freq, base1 = base + 1;
        int mirror = 2*(WINDOW_SIZE - freq), mirror1=mirror+1;

  //    System.out.println("advancedFilter.filter.getVerdict()");
        if (filter.getVerdict(freq,pan[freq]) == PanPercFilter.CANCEL) { 
          inL[base] = inL[base1] = inR[base] = inR[base1] = 0f;
          if (freq >= 1 && freq < pan.length)
             inL[mirror] = inL[mirror1]  
               = inR[mirror] = inR[mirror1] = 0f;
        }
     } // end for

   }

} // End class
