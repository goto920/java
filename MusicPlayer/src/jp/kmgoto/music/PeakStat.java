package jp.kmgoto.music;
// import java.io.Serializable;
import java.io.File;
import java.io.FileWriter;
// use Fuzzy C-means Java Implementation?
// http://jwork.org/jminhep/

public class PeakStat {

   private float samplingRate;
   private int windowSize;
   private double[][] totalPeak, percussivePeak, harmonicPeak;
   private int panStep, freqStep;
   private double div;
   private double[] freqFromIndex;
   private int[] freqIndexMin;

   public PeakStat(float samplingRate, 
   int windowSize, int panStep, int freqStep)
   {
      this.samplingRate = samplingRate;
      this.windowSize = windowSize;
      this.panStep = panStep;
      this.freqStep = freqStep;
      totalPeak = new double[panStep][freqStep];
      percussivePeak = new double[panStep][freqStep];
      harmonicPeak   = new double[panStep][freqStep];
      freqFromIndex  = new double[freqStep];
      freqIndexMin   = new int[windowSize/2+1];
      double maxPitch = 12*Math.log(44100.0/2.0)/Math.log(2.0);

      freqFromIndex[0] = 0;
      for (int i = 1; i < freqStep; i++){ 
        freqFromIndex[i] = Math.pow(2, i*maxPitch/(12*freqStep));
      }

/*
      for (int i = 1; i < freqStep; i++) 
        System.out.println("DEBUG i, freqFromIndex[i] = "
             + i + ", " + freqFromIndex[i]); 
*/

      for (int i = 0; i <= windowSize/2; i++){
        double freq = i*samplingRate/windowSize; // Hz
        for (int j = 0; j < freqStep - 1; j++){
           if (freq >= freqFromIndex[j] &&
               freq < freqFromIndex[j+1]) freqIndexMin[i] = j; 
        }
      }

/*
      for (int i = 0; i <= windowSize/2; i++)
          System.out.println("DEBUG i, freqIndexMin[i] = "
                            + i + ", " + freqIndexMin[i]);
*/

      for (int pan =0; pan < panStep; pan++){
        for (int freq=0; freq < freqStep; freq++){
          totalPeak[pan][freq] = 0;
          percussivePeak[pan][freq] = 0;
          harmonicPeak[pan][freq] = 0;
        }
      }
   }

   public void dumpPeak(String fileName) throws Exception {
     /*
       in gnuplot
       plot 'fileName' using 1:2:4 with image // for totalPeak
       or
       splot 'fileName' using 1:2:4 
       splot 'fileName' using 1:3:4 // in Hz 
       plot 'fileName' using 1:2:5 with image // for percussivePeak
        or splot...
       plot 'fileName' using 1:2:6 with image // for harmonicPeak
        or splot...
       3D plot
       A) set pm3d interpolate 10,10
       splot 'TPH-Peak.plot' using 1:3:4 with pm3d (3D)
       B) set pm3d interpolate 10,10
       set pm3d map
       splot 'TPH-Peak.plot' using 1:3:4 with pm3d (color)
          (using 1:2:4 (5 6) , 1:3:4 (5 6))
       set cbrange[min:max]
     */
     FileWriter fw = new FileWriter(new File(fileName));
     double[][] t = getTotalPeak(); 
     double[][] p = getPercussivePeak(); 
     double[][] h = getHarmonicPeak(); 

     fw.write("# pan freq Hz total percussive harmonic \n");
     for (int i = 0 ; i < p.length; i++){
       for (int j = 0 ; j < p[0].length; j++){
/*
        fw.write(i + " " + j + " " 
                +  String.format("%10.2f", freqFromIndex[j]) 
                + " " + Math.log(t[i][j]) 
                + " " + Math.log(p[i][j]) 
                + " " + Math.log(h[i][j]) + "\n");
*/
        fw.write(i + " " + j + " " 
                +  String.format("%10.2f", freqFromIndex[j]) 
                + " " + t[i][j] 
                + " " + p[i][j] 
                + " " + h[i][j] + "\n");
        }
        fw.write("\n");
     }
     fw.close();
   }

   public double[][] getTotalPeak(){
      return totalPeak;
   }

   public double[][] getPercussivePeak(){
      return percussivePeak;
   }

   public double[][] getHarmonicPeak(){
      return harmonicPeak;
   }

   public void process(FFTChunk chunk){
     if (chunk == null) return;

     if (chunk.getChannels() != 2) {
       System.out.println("Monaural NOT supported");
       return;
     }

     float[] panAmp = chunk.getPanAmp(); 
     float[] pL = chunk.getPercCoefL(); 
     float[] pR = chunk.getPercCoefR(); 
     float[] pan = chunk.getPan();

     for (int p = 0; p < panStep; p++){ // panIndex
       double sumT = 0, sumP = 0, sumH = 0;
       int lastFreqIndex = 0;

       for (int i = 0; i < windowSize/2 + 1; i++){ // FFTindex

          int panIndex = (int) (panStep*(pan[i]+1)/2);
          if (panIndex != p) continue;

          int freqIndex = freqIndexMin[i];
          double percussive 
              = (1-pan[i])/2*pL[i] + (1+pan[i])/2*pR[i];

          sumT += panAmp[i];
          sumP += panAmp[i]*percussive;
          sumH += panAmp[i]*(1-percussive);

          if (freqIndex > lastFreqIndex){
            if (totalPeak[panIndex][lastFreqIndex] < sumT) 
                totalPeak[panIndex][lastFreqIndex] = sumT;
            if (percussivePeak[panIndex][lastFreqIndex] < sumP) 
                percussivePeak[panIndex][lastFreqIndex] = sumP;
            if (harmonicPeak[panIndex][lastFreqIndex] < sumH) 
                harmonicPeak[panIndex][lastFreqIndex] = sumH;
            sumT = 0; sumP = 0; sumH = 0; 
            lastFreqIndex = freqIndex;
          } // end if

       } // end inner for

     } // end outer for
 
   } // end process()

  public static void main(String[] args){
   new PeakStat(44100, 4096, 21, 23); 
  } 

} // end class
