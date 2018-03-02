package jp.kmgoto.music;

//import jp.kmgoto.music.*;

public class PanFilter {

  private float position, width;

  public PanFilter() {
    position = -0.9f;
    width = 0.3f;
  } 

  public void filter(FFTChunk chunk){
    if (chunk == null) return;

    float[] pan = chunk.getPan(); 
    int windowSize = chunk.getWindowSize();
    float[] left = chunk.getFFTCoefL();
    float[] right = chunk.getFFTCoefR();

    for (int i = 0; i < windowSize/2; i++){
       if (pan[i] < position - width/2 || pan[i] > position + width/2){
          left[2*i] = left[2*i+1] = 0f;
          right[2*i] = right[2*i+1] = 0f;
        if (i > 0){
          left[2*(windowSize -i)] = left[2*(windowSize -i) +1] = 0f; 
          right[2*(windowSize -i)] = right[2*(windowSize -i) +1] = 0f; 
        }
       }
    }

  } 

}
