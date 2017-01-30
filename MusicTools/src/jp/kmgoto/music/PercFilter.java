package jp.kmgoto.music;

//import jp.kmgoto.music.*;

public class PercFilter {

  private float mix;

  public PercFilter() {
    mix = 0.95f;
  } 

  public void setMix(float mix){
    this.mix = mix;
  }

  public void filter(FFTChunk chunk){

    if (chunk == null) return;

    int windowSize = chunk.getWindowSize();
    float[] left = chunk.getFFTCoefL();
    float[] right = chunk.getFFTCoefR();
    float[] pL = chunk.getPercCoefL();
    float[] pR = chunk.getPercCoefR();

//    float sumP = 0f, sumH = 0f;

    for (int i = 0; i < windowSize/2 + 1; i++){

      float mixL, mixR;
     if (mix > 0.5f){ 
         mixL = (1f - pL[i]) + 2f*(1f-mix)*pL[i];
         mixR = (1f - pR[i]) + 2f*(1f-mix)*pR[i];
     } else {
         mixL = 2f*mix*(1f - pL[i]) + pL[i];
         mixR = 2f*mix*(1f - pR[i]) + pR[i];
     }

      left[2*i]  *= mixL; left[2*i+1] *= mixL; // real, imag
      right[2*i] *= mixR; right[2*i+1] *= mixR;

      if (i > 0){ // i = 0 is DC
        left[2*(windowSize - i)] *= mixL;
        left[2*(windowSize - i)+1] *= mixL; 
        right[2*(windowSize - i)] *= mixR; 
        right[2*(windowSize - i)+1] *= mixR;
      } 
    }// end for i 

//    System.out.println("sumP = " + sumP + ", sumpH = " +sumH);

  } 

}
