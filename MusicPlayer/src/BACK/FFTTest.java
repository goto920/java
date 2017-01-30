package jp.kmgoto.music;

import org.jtransforms.fft.FloatFFT_1D;
// import jp.kmgoto.music.*;

public class FFTTest extends BufferedEffector {

  private FloatFFT_1D fft;
  private float[] HannW;
  private int windowSize;
  private float[] inL, inR;

  public FFTTest(int channels, float samplingRate, int windowSize){
     super(channels,samplingRate);
     this.windowSize = windowSize;
     inL = new float[2 * windowSize]; // real and imag
     if (channels == 2) inR = new float[2 * windowSize]; 

     HannW = new float[windowSize];
     for (int i = 0; i < windowSize; i++) {
           HannW[i] = (float) 
            (0.5 - 0.5 * Math.cos((2 * Math.PI * i) / windowSize));
     }

     fft = new FloatFFT_1D(windowSize);
     outLimit = 0;
     flushing = false;
  }

/*
@Override
  public int putSamples(float[] data){
    return putSamples(data, data.length);
  }

@Override
  public int putSamples(float[] data, int len){
    for (int i = 0; i < len; i++) inputBuffer.add(data[i]);
    process();
    return len;
  }
*/

@Override
  public float[] getSamples(){

    if (flushing) outLimit = outputBuffer.size() - added;

    Float[] out = outputBuffer.subList(0, outLimit).toArray(new Float[0]);
    float[] retval = new float[outLimit];

    for (int i = 0; i < outLimit; i++) retval[i] = (float) out[i];

    if (flushing) outputBuffer.clear();
    else {
      outputBuffer.subList(0, outLimit).clear();
      outLimit = 0;
    }

    return retval;
  }

@Override
  public void flush(){
    added = channels*windowSize - inputBuffer.size();
    for (int i = 0; i < added; i++) inputBuffer.add(0f);
    flushing = true;
    process();
  }

@Override
  protected void process(){

     if (inputBuffer.size() < channels*windowSize) return;

     for (int i = 0; i < windowSize; i++) {
       if (channels == 1){
         inL[2*i] = HannW[i] * inputBuffer.get(i);
         inL[2*i+1] = 0f; // imag
       } else if (channels == 2){
         inL[2*i] = HannW[i] * inputBuffer.get(2*i);
         inL[2*i + 1] = 0f; // imag
         inR[2*i] = HannW[i] * inputBuffer.get(2*i+1);
         inR[2*i + 1] = 0f; // imag 
       }
     }

     inputBuffer.subList(0, channels*(windowSize/4)).clear();

     fft.complexForward(inL);  // Left real, imag, Right real, imag
     if (channels == 2) fft.complexForward(inR);

     fft.complexInverse(inL,true);
     if (channels == 2) fft.complexInverse(inR,true);

// 1/4 window overlap add
     int max = outputBuffer.size();

     for (int i = 0; i < inL.length/2; i++) {
        int pos = outLimit + channels*i;
        if (pos < max){
          outputBuffer.set(pos, outputBuffer.get(pos) + inL[2*i]/2f);
          if (channels == 2)
             outputBuffer.set(pos+1, outputBuffer.get(pos+1) + inR[2*i]/2f);
        } else {
          outputBuffer.add(inL[2*i]/2f);
          if (channels == 2) 
             outputBuffer.add(inR[2*i]/2f); // use real part only
        }
     }
     outLimit += channels * windowSize/4;
  }

  public static void main(String[] args){
     FFTTest ef = new FFTTest(2, 44100f, 4096);
  }

}
