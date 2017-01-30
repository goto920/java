package jp.kmgoto.music;

//import jp.kmgoto.music.*;

public class PanCalculator {

  private FFTChunk fftChunk;
  private int windowSize;

  public PanCalculator(int channels, int windowSize) {
    this.windowSize = windowSize;
    if (channels != 2) System.out.println("Channels != 2");
  }

/*
  public void putFFTChunk(FFTChunk chunk){
     fftChunk = chunk;
     if (fftChunk == null) return;

     float[] inL = chunk.getFFTCoefL();
     float[] inR = chunk.getFFTCoefR();
     calcPan(inL, inR);
  }

  public FFTChunk getFFTChunk(){
     return fftChunk;
  }
*/

public void calcPan(FFTChunk chunk){
//  protected void calcPan(FFTChunk chunk){
      if (chunk == null) return;

      float[] inL = chunk.getFFTCoefL();
      float[] inR = chunk.getFFTCoefR();
      float[] pan = new float[windowSize/2 + 1];
      float[] panAmp = new float[windowSize/2 + 1];

      for(int i=0; i < windowSize/2 + 1; i++){

        int base = 2*i, base1 = base + 1;

         float dotProd   =  inL[base]*inR[base] + inL[base1]*inR[base1];
         float crossProd = -inL[base]*inR[base1] + inL[base1]*inR[base];

         float abs 
          = (float) Math.sqrt(
                 (inL[base] + inR[base])*(inL[base] + inR[base])
               + (inL[base1]+ inR[base1])*(inL[base1]+ inR[base1])
            );
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
              panAmp[i] = (absLR - absL)/abs; 
           } else if (dotProd <= absR*absR){ 
              frac = dotProd/(absR*absR);
              panAmp[i] = Math.max(absL, absLR) - Math.abs(crossProd)/absR;
           } else {
              frac = 1f;
              // panAmp[i] = (absL - absLR)/abs;
              panAmp[i] = absL - absLR;
           }
           pan[i] = (1-frac)/(1+frac);
         } else { // absL >= absR
           if (dotProd < 0) {
             frac = 0f;
              panAmp[i] = (absLR - absR)/abs;
           } else if (dotProd <= absL*absL){
             frac = dotProd/(absL*absL);
              panAmp[i] = Math.max(absR, absLR) - Math.abs(crossProd)/absL;
           } else {
             frac = 1f;
              // panAmp[i] = (absR - absLR)/abs;
              panAmp[i] = absR - absLR;
           }
           pan[i] = (frac-1)/(1+frac);
         }
      } // end for i

      chunk.setPan(pan);
      chunk.setPanAmp(panAmp);
    //  return;

   } // End calcPan

} // End class
