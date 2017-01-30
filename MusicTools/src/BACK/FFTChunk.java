package jp.kmgoto.music;

public class FFTChunk {

  private int channels;
  private int windowSize;
  private int windowShift;
  private float[] fft; // (real, imag) * channels * windowSize
  private float[] pan, panAmp;
  private float[] percCoef;

  FFTChunk(int channels, int windowSize, int windowShift){
     this.channels = channels;
     this.windowSize = windowSize;
     this.windowShift = windowShift;
     fft = pan = panAmp = percCoef = null;
  }

  public int getChannels(){return channels;}
  public int getWindowSize(){return windowSize;}
  public int getWindowShift(){return windowShift;}

  public void setFFTCoef(float[] input){fft = input; }
  public void setPan(float[] input){ pan = input;}
  public void setPercCoef(float[] input){ percCoef = input;}

  public float[] getFFTCoef(){return fft;}
  public float[] getPan(){ return pan;}
  public float[] getPercCoef(){ return percCoef;}

  public static void main(String[] args){
  }

}
