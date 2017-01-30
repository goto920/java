package jp.kmgoto.music;

import java.util.*;

public class PercCalculator {
  
  List<FFTChunk> inputBuffer;
  List<FFTChunk> outputBuffer;
  boolean flushing;
  int channels, windowSize, dataSize;

  public PercCalculator(int channels, int windowSize, int dataSize) {
     this.channels = channels;
     this.dataSize = dataSize;
     this.windowSize = windowSize;
     inputBuffer = new ArrayList<FFTChunk>();
     outputBuffer = new ArrayList<FFTChunk>();
     flushing = false;
  }

  public void putFFTChunk(FFTChunk chunk){
     if (chunk == null) return;

     inputBuffer.add(chunk);
     calcPMedian();
     calcHMedian();
  }

  public void flush(){
    flushing = true;
    while(inputBuffer.size() > dataSize/2+1) calcHMedian();
  }

  public FFTChunk getFFTChunk(){
     if (outputBuffer.size() == 0) return null;

     FFTChunk retval = outputBuffer.get(0);
     outputBuffer.remove(0);
     return retval;
  }

  private int count = 0;
  private void calcHMedian(){ // harmonic factor in time domain

     int from, to, index;

     if (flushing){
       index = dataSize/2;
       if (index >= inputBuffer.size()) return;
       from = 0;
       to = inputBuffer.size();
     } else { 
       index = inputBuffer.size() - 1 - dataSize/2;
       if (index < 0) return;
       from = Math.max(0, index - dataSize/2);
       to = Math.min(inputBuffer.size(), index + dataSize/2 + 1);
     }

     FFTChunk chunk = inputBuffer.get(index);

     List<Float> powerL, powerR=null;

     for (int freq = 0; freq < windowSize/2 + 1; freq++){  

       powerL = new ArrayList<Float>();
       if (channels == 2) powerR = new ArrayList<Float>();

      for (int i = from; i < to; i++){
        FFTChunk tmp = inputBuffer.get(i);
        float[] coef = tmp.getFFTCoefL();
        powerL.add(coef[2*freq]*coef[2*freq] + coef[2*freq+1]*coef[2*freq+1]);
        if (channels == 2){
          coef = tmp.getFFTCoefR();
          powerR.add(coef[2*freq]*coef[2*freq] + coef[2*freq+1]*coef[2*freq+1]);
        }
      } // end for i

      Float[] power = powerL.toArray(new Float[0]);
      float hL = median(power,power.length);
      float[] pL  = chunk.getPercCoefL();
      pL[freq] = (pL[freq]*pL[freq])/(pL[freq]*pL[freq] + hL*hL);
      powerL.clear(); 

      if (channels == 2) {
        power = powerR.toArray(new Float[0]);
        float hR = median(power, power.length);
        float[] pR = chunk.getPercCoefR();
        pR[freq] = (pR[freq]*pR[freq])/(pR[freq]*pR[freq] + hR*hR);
        powerR.clear();
      }
     } // end for freq

    outputBuffer.add(chunk); 

    if(inputBuffer.size() >= dataSize || flushing) inputBuffer.remove(0);

  }

  private void calcPMedian(){ // percussive factor in freq domein

    if (inputBuffer.size() == 0) return;

    FFTChunk chunk = inputBuffer.get(inputBuffer.size()-1); // last one

//    System.out.println("calcPMedian " + chunk.getCount()); // debug

    float[] coefL = chunk.getFFTCoefL();
    float[] pMedianL = new float[windowSize/2 + 1]; 
    List<Float> powerL = new ArrayList<Float>();

    float[] coefR = null;
    float[] pMedianR = null;
    List<Float> powerR = null;

    if (channels == 2) {
      coefR = chunk.getFFTCoefR();
      pMedianR = new float[windowSize/2 + 1]; 
      powerR = new ArrayList<Float>();
    }

    for (int i = 0; i < windowSize/2 + 1; i++){ // i = FFT index
      powerL.add(coefL[2*i]*coefL[2*i] + coefL[2*i+1]*coefL[2*i+1]);
        // real 2*i, imag 2*i + 1
      if (channels == 2)
        powerR.add(coefR[2*i]*coefR[2*i] + coefR[2*i+1]*coefR[2*i+1]);
    } // end for i

    for (int i = 0; i < windowSize/2 + 1; i++){
      Float[] powArray;
      int from = Math.max(0,i - dataSize/2);
      int to   = Math.min(windowSize/2 + 1, i + dataSize/2 + 1);

      powArray = powerL.subList(from,to).toArray(new Float[0]);
      pMedianL[i] = median(powArray, powArray.length);

      if(channels == 2){ 
        powArray = powerR.subList(from,to).toArray(new Float[0]);
        pMedianR[i] = median(powArray, powArray.length);
      }

    } // end for i

    if (channels == 1) {
      chunk.setPercCoefMono(pMedianL);
      powerL.clear();
    } else if (channels == 2) {
      chunk.setPercCoefStereo(pMedianL, pMedianR);
      powerL.clear(); powerR.clear();
    }

  }
  
  private static float median(Float[] in, int size){
    Arrays.sort(in,0,size);
    if (size % 2 == 1) return in[size/2];
    else return (in[size/2-1] + in[size/2])/2.0f;
  }

}
