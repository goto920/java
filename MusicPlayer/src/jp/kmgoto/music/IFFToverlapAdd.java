package jp.kmgoto.music;
import org.jtransforms.fft.FloatFFT_1D;
// import jp.kmgoto.music.*;

public class IFFToverlapAdd extends BufferedEffector {

  private FloatFFT_1D fft;
  private int channels, windowSize, windowShift;
  private FFTChunk input;

  public IFFToverlapAdd(int channels, float samplingRate, int windowSize){
    super(channels,samplingRate);
    this.channels =  channels;
    this.windowSize = windowSize;
    input = null;
    fft = new FloatFFT_1D(windowSize);
    outLimit = 0;
    flushing = false;
  }

  public void putFFTChunk(FFTChunk chunk){ 
    if (chunk == null) return;
    input = chunk; 
    process();
  }

@Override
  protected void process(){

     float[] inL  = input.getFFTCoefL(); 
     fft.complexInverse(inL,true);
     float[] inR = input.getFFTCoefR(); 
     fft.complexInverse(inR,true);

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

@Override
  public float[] getSamples(){

    if(flushing) outLimit = outputBuffer.size();

    Float[] out = outputBuffer.subList(0, outLimit).toArray(new Float[0]);
    float[] retval = new float[outLimit];

    for (int i = 0; i < outLimit; i++) retval[i] = out[i].floatValue();

    if (flushing) outputBuffer.clear();
    else {
      outputBuffer.subList(0, outLimit).clear();
      outLimit = 0;
    }

    return retval;
  }

 @Override
  public void flush(){
    flushing = true;
    process();
  }

  public static void main(String[] args){
  }

}
