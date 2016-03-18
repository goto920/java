package jp.kmgoto.musicplayer;

// import android.util.Log;

// import org.apache.commons.math3.complex.Complex;
import org.jtransforms.fft.FloatFFT_1D;

import java.util.ArrayList;
// import java.util.Collections;
import java.util.List;

public class FFTEffector {
  //  public static final int OP_TYPE = 0;
    public static final int OP_PAN = 1;
    public static final int OP_WIDTH = 2;
    public static final int OP_LOWF = 3;
    public static final int OP_HIGHF = 4;
    public static final int OP_MIX = 5;
    // public static final int OP_CANCEL = 6;

    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_FFTTEST = 0;
    public static final int TYPE_PANCANCEL = 1;
    public static final int TYPE_PERCSPLIT = 2;
    public static final int TYPE_STEREO_PERCSPLIT = 3;

    private int type;
    private long total_input;
    private long total_output;
//    private float panCenter, width, lowF, highF, mix;
//    private boolean cancel;

    // for FFT
    private FloatFFT_1D fft;
    private float[] HannW; // Hann Window
    private List<Float> inputBuffer;
    private List<Float> outputBuffer;
    private final int WINDOW_SIZE = 4096;
    // private final int SAMPLING_RATE = 44100;
    // private final int SAMPLE_SIZE = 2; // octets sample
    // private final int CHANNELS = 2; // channels
    private float[] inL;
    private float[] inR;
    // private boolean EOD, EOI; // End Of output/input Data
    // for PanCanceler

    // PanCancel instanse
    private PanCanceler panCanceler;
    // PercussionSplitter instance
    private PercussionSplitter leftM;
    private PercussionSplitter rightM;
    private StereoPercussionSplitter stereoPercussionSplitter;

    private int seq;
    public FFTEffector(int type) {
        total_input = 0;
        total_output = 0;
       // seq = 0;
        this.type = type;
        inputBuffer = new ArrayList<>(); // Float
        outputBuffer = new ArrayList<>(); // Float

        switch (type) {
          case TYPE_PANCANCEL:
             panCanceler = new PanCanceler();
             break;
          case TYPE_PERCSPLIT:
             leftM  = new PercussionSplitter(17); 
             rightM = new PercussionSplitter(17);
             leftM.name = "LEFT";
             rightM.name = "RIGHT";
             break;
          case TYPE_STEREO_PERCSPLIT:
             stereoPercussionSplitter 
              = new StereoPercussionSplitter(17); // median size
             break;
          default:
             this.type = TYPE_UNKNOWN;
        }

        inL = new float[2 * WINDOW_SIZE];
        inR = new float[2 * WINDOW_SIZE];

        // Init Hann Window
        HannW = new float[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
           HannW[i] = (float) 
            (0.5 - 0.5 * Math.cos((2 * Math.PI * i) / WINDOW_SIZE));
        }

        fft = new FloatFFT_1D(WINDOW_SIZE);
        state = S_MORE;
        retPerc = PercussionSplitter.STATE_NEEDMORE;
    }

    public void setPanPercFilter(PanPercFilter filter){
       if (type == TYPE_STEREO_PERCSPLIT)
          stereoPercussionSplitter.setPanPercFilter(filter);
         else if (type == TYPE_PANCANCEL)
          panCanceler.setPanPercFilter(filter);
    }

    public void setSamplingRate(int value){
       if (type == TYPE_PERCSPLIT){
          leftM.setSamplingRate(value); 
          rightM.setSamplingRate(value);
       } else if (type == TYPE_STEREO_PERCSPLIT){
          stereoPercussionSplitter.setSamplingRate(value); 
       } else if (type == TYPE_PANCANCEL)
          panCanceler.setSamplingRate(value);
    } 

    public void setParam(int var, float value) {
        switch (var) {
            case OP_PAN:
              if (type == TYPE_PANCANCEL) 
                 panCanceler.setPan(value);
              break;
            case OP_WIDTH:
              if (type == TYPE_PANCANCEL)  
                 panCanceler.setWidth(value);
              break;
            case OP_LOWF:
              if (type == TYPE_PERCSPLIT){
                  leftM.setLowF(value); rightM.setLowF(value);
              } else if (type == TYPE_PANCANCEL) 
                 panCanceler.setLowF(value);
              break;
            case OP_HIGHF:
                if (type == TYPE_PERCSPLIT){
                   leftM.setHighF(value); rightM.setHighF(value);
                } else if (type == TYPE_PANCANCEL) 
                 panCanceler.setHighF(value);
                break;
            case OP_MIX:
                if (type == TYPE_PERCSPLIT){
                   leftM.setMix(value); rightM.setMix(value);
                } else if (type == TYPE_STEREO_PERCSPLIT){
                   stereoPercussionSplitter.setMix(value);
                }
                break;
            default:
//                Log.d("FFTEffector", "wrong set op");
        }
    }

    public void setCancel(boolean op) {
       if (type == TYPE_PANCANCEL) panCanceler.setCancel(op);
    }

    public void setBypass(boolean op) {
       if (type == TYPE_PANCANCEL) 
          panCanceler.setBypass(op);
       else if (type == TYPE_PERCSPLIT){
         leftM.setBypass(op); rightM.setBypass(op);
       }
    }

////////////// main part
    private int state;
    private final int S_MORE     =  0; // need more data
    private final int S_READY    =  1; // ready
    private final int S_FLUSH1   =  2; // flush input in Effector
    private final int S_FLUSH2   =  3; // flush output in PercussionSplitter
    private final int S_FLUSH3   =  4; // flush output in Effector
    private final int S_FLUSH4   =  5; // flush last output in Effector
    private final int S_FINISHED = -1; // no more output
    private int retPerc;

    public int process(byte[] input, int ilen, ArrayList<Byte> output) {

     boolean inputAvailable;
     if (ilen >= 0) total_input += ilen;

     if (input != null && ilen >= 0){ 
       for (int i = 0; i < ilen; i += 2) 
         inputBuffer.add((float) (input[i] + input[i+1]*256)); 
     } else {
//        System.out.println("No input ");
     }

     int isize = inputBuffer.size(); 
     int osize = outputBuffer.size();

     switch(state){
       case S_MORE:
          if (input == null || ilen < 0){
             state = S_FLUSH1;
          } else if (isize >= 2*WINDOW_SIZE) state = S_READY;

         break;
       case S_READY:
         if (input == null || ilen < 0){ 
           if (total_input - total_output < WINDOW_SIZE)
             state = S_FLUSH4; // 4 bytes/sample * win/4
           else state = S_FLUSH1;
         } else if (isize < 2*WINDOW_SIZE) state = S_MORE;
         
         break;
       case S_FLUSH1:
         if (isize == 0){ 
             if (type == TYPE_PERCSPLIT) state = S_FLUSH2; 
             else state = S_FLUSH3;
         } else if (total_input - total_output < WINDOW_SIZE) // rest bytes/4 < WINDOW/4
           state = S_FLUSH4; // 4 bytes/sample * win/4
         break; 
       case S_FLUSH2:
          if (retPerc == PercussionSplitter.STATE_FINISHED){ 
            state = S_FLUSH3;
          } else if (total_input - total_output < WINDOW_SIZE)
            state = S_FLUSH4; // 4 bytes/sample * win/4
          break;
       case S_FLUSH3:
          if (total_input <= total_output) state = S_FINISHED;
          else if (total_input - total_output < WINDOW_SIZE)
            state = S_FLUSH4; // 4 bytes/sample * win/4
         break;
       case S_FLUSH4:
          if (osize == 0) 
            state = S_FINISHED;
         break;
       case S_FINISHED:
           return -1;
     } // End switch

   // debug
/*
     System.out.print(
      "seq, state, ilen, isize, osize = " 
       + seq + " " + state + " " + ilen + " " + isize + " " + osize + "| ");
*/
     seq++;

     if (state == S_MORE)     return 0; // need more data
     if (state == S_FINISHED) return -1; // no more

     // Input
     if (isize > 0){
        // fill float bytes(LE) for Fourier transform coefficient
        for (int i = 0; i < WINDOW_SIZE; i++) {
          if (2*i < isize){
            inL[2*i] = HannW[i] * inputBuffer.get(2*i);
            inR[2*i] = HannW[i] * inputBuffer.get(2*i+1);
          } else inL[2*i] = inR[2*i] = 0f; // fill 0 for
          inL[2*i + 1] = inR[2*i + 1] = 0f; // imag
        } // end for 

     // shift WINDOW_SIZE/4
     // i.e. remove WINDOW_SIZE/4*channels samples from the head 
     //    if (isize < 2*WINDOW_SIZE/4) inputBuffer.clear();
     //    else 
       if (isize >= 2*WINDOW_SIZE/4)
       inputBuffer.subList(0,2*WINDOW_SIZE/4).clear();

       // forward transform. results are returned in inL, inR
       fft.complexForward(inL); 
       fft.complexForward(inR);
       inputAvailable = true;
    }  else inputAvailable = false;

    // Conversion
     switch(type){
        case TYPE_FFTTEST: 
           break;
        case TYPE_PANCANCEL: 
           panCanceler.processOneWindow(inL,inR); 
           break;
        case TYPE_PERCSPLIT:
//           System.out.println("Perc inputAvail " + inputAvailable);
           retPerc = leftM.percussionSplit(inL,inputAvailable); 
                     rightM.percussionSplit(inR,inputAvailable); 

//           System.out.println("retPerc " + retPerc);
           switch(retPerc){
             case PercussionSplitter.STATE_NEEDMORE: //  0
             case PercussionSplitter.STATE_FINISHED: // -1
//               System.out.println("retPerc(switch) " + retPerc);
               return retPerc; // no output
             default:
           }
           break;
        case TYPE_STEREO_PERCSPLIT:
           retPerc = stereoPercussionSplitter.percussionSplit(
              inL,inR,inputAvailable); 

           switch(retPerc){
             case StereoPercussionSplitter.STATE_NEEDMORE: //  0
             case StereoPercussionSplitter.STATE_FINISHED: // -1
               return retPerc; // no output
             default:
           }
           break;

        default:
      } // end switch


    if (state == S_READY || state == S_FLUSH1 || state == S_FLUSH2) { 

      fft.complexInverse(inL,true);
      fft.complexInverse(inR,true);

      // Output
      // store inversed sample in 2 bytes(LE) as shifted window
      int pos;
      int max = outputBuffer.size();
      if (max/2 < WINDOW_SIZE) pos = 0;
      else pos = max/2 - 3*WINDOW_SIZE/4; 

      // fill outputBuffer
      for (int i = 0; i < WINDOW_SIZE; i ++) {
       int basePos = 2*(pos+i); int basePos1 = basePos+1;

       if (basePos < max){
         float sample = outputBuffer.get(basePos);
         outputBuffer.set(basePos, sample  + inL[2*i]/2f); 
           // Left real at 2*i, imag at 2*i + 1
         sample = outputBuffer.get(basePos1);
         outputBuffer.set(basePos1, sample + inR[2*i]/2f);
           // Right real at 2*i, imag at 2*i + 1
        } else {
         outputBuffer.add(inL[2*i]/2f);
         outputBuffer.add(inR[2*i]/2f);
        }
       } // end for
     } // End (READY or FLUSH1)

     int len = outputBuffer.size();
     if (len >= WINDOW_SIZE || state == S_FLUSH4) { // Half window (LR) 

      // Fill in data byte converted from float
      // # of  samples WINDOW_SIZE * 4 (bytes/sample) - added samples
      // output buffer: 2 floats/sample
      int max;
      if (state == S_FLUSH4){ 
         max = (int) ((total_input - total_output)/4);
         inputBuffer.clear(); // flush input data with added data
         System.err.println("FLUSH4 max " + max);
      }  else max = WINDOW_SIZE/4;

      output.clear();
      for (int i = 0; i < max; i++){
         int base = 4*i;
         int sample = (int) ((float) outputBuffer.get(2*i)); // Left
         output.add(base,(byte) (sample & 0xff)); // LE
         output.add(base+1, (byte) (0xff & sample/256));

         sample = (int) ((float) outputBuffer.get(2*i+1)); // Right
         output.add(base+2,(byte) (sample & 0xff));
         output.add(base+3,(byte) (0xff & sample/256));
       } // end for

       if (max > 0) outputBuffer.subList(0, 2*max).clear(); // delete window/4

       total_output += output.size();

//   System.out.println("total in/out: " + total_input + " " + total_output);

       return output.size(); // number of output bytes
    } else { 
//   System.out.println("out 0 byte");
       return 0; // no output
    }

  } // End of func

} // End of class
