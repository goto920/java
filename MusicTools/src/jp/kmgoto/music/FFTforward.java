package jp.kmgoto.music;

import org.jtransforms.fft.FloatFFT_1D;
// import jp.kmgoto.music.BufferedEffector;

public class FFTforward extends BufferedEffector {

  private FloatFFT_1D fft;
  private float[] HannW;
  private int windowSize;
  private float[] inL, inR;
  private FFTChunk fftChunk;
  private boolean chunkReady;

  public FFTforward(int channels, float samplingRate, int windowSize){
     super(channels,samplingRate);
     this.windowSize = windowSize;
     HannW = new float[windowSize];
     for (int i = 0; i < windowSize; i++) {
           HannW[i] = (float) 
            (0.5 - 0.5 * Math.cos((2 * Math.PI * i) / windowSize));
     }

     fft = new FloatFFT_1D(windowSize);
     flushing = false;
     outputBuffer = null;
     fftChunk = null;
     chunkReady = false;
  }

@Override
  public float[] getSamples(){return null;}

@Override
  public void flush(){
    added = channels*windowSize - inputBuffer.size();
    System.out.println("FFTforward: added samples = " + added);
    for (int i = 0; i < added; i++) inputBuffer.add(0f);
    flushing = true;
    process();
  }

@Override
  protected void process(){

     if (inputBuffer.size() < channels*windowSize){
        chunkReady = false;
        return;
     }

     fftChunk = new FFTChunk(channels, windowSize);
     inL = new float[2 * windowSize]; // real and imag
     if(channels == 2) inR = new float[2 * windowSize]; 

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

     if (channels == 1) {
       fft.complexForward(inL);  // Left real, imag, Right real, imag
       fftChunk.setFFTCoefMono(inL);
     } else if (channels == 2) {
       fft.complexForward(inL);
       fft.complexForward(inR);
       fftChunk.setFFTCoefStereo(inL, inR);
     } 
     chunkReady = true;
  } 

  public FFTChunk getFFTChunk() {
     if (chunkReady) return fftChunk;
     else return null;
  }
  public int getAdded(){return added;}

  public static void main(String[] args){
  }

}
