package jp.kmgoto.music;

public class PeakFinder {
   private int minPos, maxPos;

   private float calcMassCenter(float[] data, int firstPos, int lastPos){
     float sum, wsum;

     sum = wsum = 0f;

     for (int i = firstPos; i <= lastPos; i++){
        sum += (float) i * data[i];
        wsum += data[i];
     }
     return sum/wsum;
   }

   private int findCrossingLevel(float[] data, 
     float level, int peakpos, int direction){

     float peakLevel;
     int pos;

     peakLevel = data[peakpos];
     pos = peakpos;
   
     while(pos >= minPos && pos < maxPos){
      if (data[pos+direction] < level) return pos;
      pos += direction;
     } 
     return -1;
   }  

  private int findTop(float[] data, int peakpos){
     
     int start = Math.max(minPos, peakpos - 10);
     int end = Math.min(maxPos, peakpos + 10);
     float refvalue = data[peakpos];

     for (int i = start; i <= end; i++)
        if (data[i] > refvalue) { peakpos = i; refvalue = data[i];}

     if (peakpos == start || peakpos == end) return 0;

     return peakpos;
  }

  private int findGround(float[] data, int peakpos, int direction){
     int lowpos, pos, climb_count;
     float refvalue, delta; 

     climb_count = 0; refvalue = data[peakpos];
     lowpos = peakpos; pos = peakpos;

     while(pos > minPos+1 && pos < maxPos-1){
       int prevpos = pos;
       pos += direction;
       delta = data[pos] - data[prevpos];
       if (delta <= 0){

         if (climb_count > 0) climb_count--;
         if (data[pos] < refvalue) {
           lowpos = pos;
           refvalue = data[pos];
         }
       } else { 
          climb_count++;
          if (climb_count > 5) break; 
       }
      
     }

     return lowpos;
  }

  private float getPeakCenter(float[] data, int peakpos){
     float peakLevel;
     int crosspos1, crosspos2;
     float cutLevel;
     float groundLevel;
     int gp1, gp2;
   
     gp1 = findGround(data, peakpos, -1);
     gp2 = findGround(data, peakpos, 1);
     peakLevel = data[peakpos];

     if (gp1 == gp2) cutLevel = groundLevel = peakLevel;
     else {
       groundLevel = 0.5f*(data[gp1] + data[gp2]);
       cutLevel = 0.7f * peakLevel + 0.3f * groundLevel;
     }

     crosspos1 = findCrossingLevel(data, cutLevel, peakpos, -1);
     crosspos2 = findCrossingLevel(data, cutLevel, peakpos, 1);
    
     if (crosspos1 < 0 || crosspos2 < 0) return 0;

     return calcMassCenter(data, crosspos1, crosspos2); 
  }


  public PeakFinder(){
    minPos = maxPos = 0;
  }

  public float detectPeak(float[] data, int minPos, int maxPos){
    int peakpos;
    float highPeak, peak;

    this.minPos = minPos; this.maxPos = maxPos;
    peakpos = minPos; peak = data[minPos];

// absolute peak
    for (int i = minPos + 1; i < maxPos; i++){
       if (data[i] > peak) {
         peak = data[i]; peakpos = i;
       }
    }
    highPeak = getPeakCenter(data, peakpos);
    peak = highPeak;

    for (int i = 3; i < 10; i++){
      float peaktmp, harmonic;
      int i1, i2;

      harmonic = (float) i * 0.5f;
      peakpos = (int)(highPeak/ harmonic + 0.5f);
      if (peakpos < minPos) break;
      peakpos = findTop(data, peakpos);
      if (peakpos == 0) continue;
      peaktmp = getPeakCenter(data, peakpos); 
      float diff = harmonic * peaktmp / highPeak;
      if (diff < 0.96f || diff > 1.04f) continue;

      i1 = (int) (highPeak + 0.5f);
      i2 = (int) (peaktmp + 0.5f);
      if (data[i2] >= 0.4f*data[i1]) peak = peaktmp;

    }
    return peak;
  }

  public static void main(String[] args){
  }
}
