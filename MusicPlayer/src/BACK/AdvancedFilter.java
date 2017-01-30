import jp.kmgoto.music.*;

public class AdvancedFilter {

   private float samplingRate;
   private int windowSize;
   private String[][] filterArray;
   private int panSplits;

   public AdvancedFilter(float samplingRate, int windowSize, int panSplits){
      this.samplingRate = samplingRate;
      this.windowSize = windowSize;
      this.panSplits = panSplits;
      filterArray = new String[panSplits][windowSize/2 +1];
   }

   public void dumpFilterArray(){
     for (int p = 0; p < filterArray.length; p++)
       for (int f = 0; f < filterArray[0].length; f++)
        System.out.println("filterArray(" + p + "," +f + ") = "
           + filterArray[p][f]);
   }

   public void setRangeAndType(
      float panL, float panR, float lowF, float highF, String type){

/*
     System.out.println("debug: panL/panR/lowF/highF/type" 
          + panL + "/" + panR + "/" + lowF + "/" + highF + "/" +type); 
*/

     int panLIndex  = Math.max(0, (int) (panSplits*(panL + 1f)/2f));
     int panRIndex  = Math.min(filterArray.length-1, 
                          (int) (panSplits*(panR + 1f)/2f));
     int lowFIndex  = Math.max(0, (int) (lowF*windowSize/samplingRate));
     int highFIndex  = Math.min(filterArray[0].length-1,
                        (int) (highF*windowSize/samplingRate));

     for (int p = panLIndex; p <= panRIndex; p++)
       for (int f = lowFIndex; f <= highFIndex; f++)
        filterArray[p][f] = type;
   }

   public void apply(FFTChunk chunk){
     if (chunk == null) return;
     if (chunk.getChannels() != 2) {
       System.out.println("Monaural NOT supported");
       return;
     }

     float[] left = chunk.getFFTCoefL(); 
     float[] pL = chunk.getPercCoefL(); 
     float[] right = chunk.getFFTCoefR(); 
     float[] pR = chunk.getPercCoefR(); 
     float[] pan = chunk.getPan();

     for (int i = 0; i < windowSize/2 + 1; i++){ // FFTindex
       int panIndex  = (int) (panSplits*(pan[i] + 1f)/2f);
       panIndex = Math.min(filterArray.length -1, panIndex);
       String type = filterArray[panIndex][i];

       if (type.equals("T")) { // Through
       } else if (type.equals("M")) { // Mute
         left[2*i] = left[2*i+1] = 0f; 
         right[2*i] = right[2*i+1] = 0f; 
       } else if (type.equals("H")) { // Harmonic
         left[2*i] *= (1 - pL[i]); left[2*i+1] *= (1 - pL[i]);
         right[2*i] *= (1 - pR[i]); right[2*i+1] *= (1 - pR[i]);
       } else if (type.equals("P")) { // Percussive
         left[2*i] *= pL[i]; left[2*i+1] *= pL[i];
         right[2*i] *= pR[i]; right[2*i+1] = pR[i];
       } else if (type.equals("S")) { // Split (not implemented)
         System.out.println("type split not implemented yet");
       } else {
         System.out.println("type " + type + "Unknown");
         return;
       }

       if (i > 0){ // i = 0 is DC
          left[2*(windowSize - i)] = left[2*i];
          left[2*(windowSize - i)+1] = -left[2*i + 1];
          right[2*(windowSize - i)] = right[2*i];
          right[2*(windowSize - i)+1] = -right[2*i+1];
       } 
       
     } 
 
   }

}
