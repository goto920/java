package jp.kmgoto.music;

public class FFTChunk {

  private int channels;
  private int windowSize;
  private int windowShift;
  private float[] left, right; // (real, imag) * channels * windowSize
  private float[] pan, panAmp;
//  private float[] percL, percR, pMedianL, pMedianR;
  private float[] percL, percR;
  private static long countGlobal = 0;
  private long countLocal = 0;

  public FFTChunk(int channels, int windowSize){
     this.channels = channels;
     this.windowSize = windowSize;
     this.windowShift = windowSize/4;
     left = right = pan = panAmp = percL = percR = null;
     countLocal = FFTChunk.countGlobal;
     FFTChunk.countGlobal++;
  }

  public long getCount(){return countLocal;}
  public int getChannels(){return channels;}
  public int getWindowSize(){return windowSize;}
  public int getWindowShift(){return windowShift;}

  public void setFFTCoefMono(float[] input){left = input;}
  public void setFFTCoefStereo(float[] inputL, float[] inputR){
           left = inputL; right = inputR; 
  }
  public void setPan(float[] input){ pan = input;}
  public void setPanAmp(float[] input){ panAmp = input;}
  public void setPercCoefMono(float[] input){ percL = input;}
  public void setPercCoefStereo(float[] inputL, float[] inputR){ 
      percL = inputL; percR = inputR;
  }

/*
  public void setPMedianMono(float[] input){ pMedianL = input;} 
  public void setPMedianStereo(float[] inputL, float[] inputR){ 
      pMedianL = inputL; pMedianR = inputR;
  }
*/

  public float[] getFFTCoefMono(){return left;}
  public float[] getFFTCoefL(){return left;}
  public float[] getFFTCoefR(){return right;}
  public float[] getPan(){return pan;}
  public float[] getPanAmp(){return panAmp;}
  public float[] getPercCoefL(){return percL;}
  public float[] getPercCoefR(){return percR;}
/*
  public float[] getPMedianL(){return pMedianL;}
  public float[] getPMedianR(){return pMedianR;}
*/

  public void dump(){
 
    System.out.println("index (left) (right) pan panAmp percL percR");
    for (int i=0; i < windowSize/2 + 1; i++){
     System.out.print(
       i + " (" + left[2*i] + "," + left[2*i+1] + ")" 
       + " (" + right[2*i] + "," + right[2*i+1] + ")");
     if (pan !=null)
       System.out.print(" " + pan[i] + " " + panAmp[i]);
     else
       System.out.print(" " + "None"  + " " + "None");

     if (percL != null)
       System.out.println(" "  + percL[i] + " " + percR[i]);
     else
       System.out.println(" "  + "None" + " " + "None");

    } // end for  

  }

  public static void main(String[] args){
      FFTChunk chunk;
     for (int i=0; i < 100; i++){
       chunk = new FFTChunk(2, 4096);
       System.out.println("Count " + chunk.getCount());
     }
      
  }

}
