import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
// import java.awt.*;
// import java.awt.event.*;
// import javax.swing.*;
// import javax.swing.event.*;
import jp.kmgoto.music.*;

public class FeedbackBoosterNoGUI {
  private int fontSize;
  private float totalSec;
  private float lastSec;
  private Mixer.Info outputPort, inputPort;
  private Mixer.Info[] inputMixers, outputMixers; 
  private FeedbackBoosterPlayer player;
  private boolean windows; 

public FeedbackBoosterNoGUI() throws Exception {
    if (System.getProperty("os.name").indexOf("Windows")>=0)
      windows = true; else windows = false;

   inputPort = outputPort = null; player = null;

// get audio port info
    GetPortsInfo info = new GetPortsInfo();
    inputMixers = info.getInputPorts();
    outputMixers = info.getOutputPorts();

  } // end constructor

// UI methods

  public String[] getInputPorts() throws Exception { 
    String[] items  = new String[inputMixers.length];
    for (int i=0; i < inputMixers.length; i++){ 
      items[i] = inputMixers[i].getName();
      if (windows)
       items[i] = new String(items[i].getBytes(),"UTF-8");
    }
    return items;
  }

  public String setInputPort(int i){
    inputPort = inputMixers[i];
    return inputPort.getName();
  }

  public String[] getOutputPorts() throws Exception{
    String[] items = new String[outputMixers.length];
    for (int i=0; i < outputMixers.length; i++){ 
       items[i] = outputMixers[i].getName();
     if (windows)
       items[i] = new String(items[i].getBytes("Windows-932"),"UTF-8");
    }
    return items;
  }

  public String setOutputPort(int i){
    outputPort = outputMixers[i];
    return outputPort.getName();
  }

  public boolean play(boolean parm){
    if (player != null) {player.stopPlay(); player = null;}
    if (parm == true) {
      player = new FeedbackBoosterPlayer(inputPort,outputPort);
      Thread pt = new Thread(player); 
      pt.start(); return true;
    } else {
      if (player != null) { player.stopPlay(); player = null; }
    }
    return false;
  }

  public boolean bypass(boolean parm){
      if (player == null) return true;
      else { 
       if (parm == true) player.setBypass(true);
       else player.setBypass(false);
       return false;
      }
  }

  public boolean autoEQ(boolean parm){
      if (player == null) return true;
      else { 
       if (parm == true) player.setAutoEQ(true);
         else player.setAutoEQ(false);
       return false; 
     }
  }

  public void setCompGain(float gain){
    if (player == null) return;
    player.setCompGain(gain);
  }

  public float getCompGain(){
    if (player == null) return 0f;
    return player.getCompGain();
  }

  public void setPeakHz(float peakHz){
    if (player == null) return;
    player.setPeakHz(peakHz);
  }

  public void setEQGain(int gain){ // dB
    if (player == null) return;
    player.setEQGain(gain);
  } 

  public void setOctMix(float mix) { // -1 .. 1
    if (player == null) return;
    player.setOctMix(mix);
  } 

  public void setQ(float q) {
    if (player == null) return;
    player.setQ(q);
  } 

  public void setRatio(float ratio){
    if (player == null) return;
    player.setRatio(ratio);
  }

  public void setThresh(float thresh){
    if (player == null) return;
    player.setThresh(thresh);
  }

  public void setVolume(float volume){
    if (player == null) return;
    player.setVolume(volume);
  }

}

class FeedbackBoosterPlayer implements Runnable {

  private TargetDataLine iline;
  private SourceDataLine sline;
  private boolean running;
  private int channels;
  private int frameSize;
  private float frameRate,sampleRate;
  private float eQdBGain,Q, octMix, volume, peakHz, compGain;
//  private JSlider timeSlider;
  private boolean bigEndian;
  private boolean bypass, autoEQ;
  private PitchFinder pitch; 
  private BiquadEQ eqBase, eqOct;
  private Compressor comp;
//  private FeedbackBoosterApp parent;
  private int processSize;

  FeedbackBoosterPlayer(Mixer.Info input, Mixer.Info output){

    AudioFormat format = new AudioFormat(
         AudioFormat.Encoding.PCM_SIGNED,
         44100f, 16, 1, 2, 44100f,false);

    processSize = 512; // 512 or 1024

    try {
      iline = AudioSystem.getTargetDataLine(format, input);
      sline = AudioSystem.getSourceDataLine(format,output);
//      System.out.println("Input buffer: " + iline.getBufferSize());
//      System.out.println("Output buffer: " + sline.getBufferSize());
//      iline.open(format, processSize*8);
      iline.open(format);
      // sline.open(format, processSize*4);
      sline.open(format);
      System.out.println("Input buffer: " + iline.getBufferSize());
      System.out.println("Output buffer: " + sline.getBufferSize());
    } catch (Exception e){ e.printStackTrace(); } 

    setBypass(true); setAutoEQ(false);
    setVolume(0.7f);
    running = false;
    pitch = new PitchFinder(44100f);
    eQdBGain = 20f; compGain = (float) Math.pow(10, (double) -eQdBGain/20.0);
    eqBase = new BiquadEQ(BiquadEQ.Type.PEAK); 
    eqOct  = new BiquadEQ(BiquadEQ.Type.PEAK); 
    setPeakHz(440f); setQ(1f);
    setOctMix(0f);
    setCompGain(0f);
    comp = new Compressor(32767); // 16bit
    setRatio(1f); setThresh(0f); 
//    parent = null;
  }

  public void stopPlay(){ running = false;}

  public void setCompGain(float gain){ 
    compGain = (float) Math.pow(10,(double) gain/20.0); 
  }

  public float getCompGain(){ // dB 
    return (float) (20*Math.log10((double)compGain));
  }

  public void setBypass(boolean flag){ bypass = flag;}
  public void setAutoEQ(boolean flag){ autoEQ = flag;}

  public void setPeakHz(float peakHz){ 
    this.peakHz = peakHz;
    eqBase.setParams(peakHz/44100f, eQdBGain, Q);
    eqOct.setParams((2*peakHz)/44100f, eQdBGain, Q);  
/*
    if (parent != null){
       parent.peakHz.setText("peakHz: " + String.format("%10.1f",peakHz));
    }
*/
  }
  public void setEQGain(int gain){
    eQdBGain = (float) Math.pow(10,(double) gain/20.0); 
    eqBase.setParams(peakHz/44100f, eQdBGain, Q);
    eqOct.setParams((2*peakHz)/44100f, eQdBGain, Q);  
  }

  public void setOctMix(float mix){octMix = mix;}
      // -1 (root 100%) .. 1(oct up 100%)
  public void setQ(float Q){
    this.Q = Q;
    eqBase.setParams(peakHz/44100f, eQdBGain, Q);
    eqOct.setParams((2*peakHz)/44100f, eQdBGain, Q);  
  }
//  public void setParent(FeedbackBoosterApp obj){parent = obj;}
  public void setRatio(float ratio){comp.setRatio(ratio); }
  public void setThresh(float thresh){comp.setThresh(thresh);}
  public void setVolume(float vol){volume = vol;}

  public void run() {
    // byte[] buf = new byte[processSize*4]; 
        // 256, 512, 1024, etc. x 4 (16bit*2ch)
    byte[] buf = new byte[1024*4];
    running = true;

    try {
       iline.start();
       sline.start();

       float[] floatSamples;
       byte[] byteSamples;
       int nread, nwritten;
       float alpha = 0.2f;

       while(running) {
          nread = iline.read(buf,0,iline.available());
          floatSamples = Util.StereoToMono(Util.LE16ToFloat(buf, nread)); 

          if (!bypass){
          // process effect here
            if (autoEQ){
              float tmp = pitch.findPitch(floatSamples);
              if (tmp > 0){
                peakHz = (1-alpha)*peakHz + alpha*tmp; // slow adjustment
                setPeakHz(peakHz);
              }
            }

            float[] base = eqBase.processArray(floatSamples);
            float[] oct  = eqOct.processArray(floatSamples);

            // add base and oct
            for (int i = 0; i < base.length; i++){
               floatSamples[i] = ((1-octMix)*base[i] + (1+octMix)*oct[i])/2f;
            }

            Util.adjustFloatGain(floatSamples,compGain);

            double peakdB = -100;
            for (int i = 0; i < floatSamples.length; i++){
               double current = 20*Math.log10(Math.abs(floatSamples[i])/32767);
               if (current > peakdB) peakdB = current;
            }


/*
            if (parent != null){
               if (peakdB > -3){
                 parent.clip.setForeground(Color.RED);
                 parent.comp.setForeground(Color.GREEN);
               } else if (peakdB > (double) parent.threshSlider.getValue()){
                 parent.clip.setForeground(Color.BLACK);
                 parent.comp.setForeground(Color.GREEN);
               } else {
                 parent.clip.setForeground(Color.BLACK);
                 parent.comp.setForeground(Color.BLACK);
               }
            }
*/

            comp.processArray(floatSamples);

          }

          Util.adjustFloatGain(floatSamples,volume); // output volume
          floatSamples = Util.MonoToStereo(floatSamples);
          byteSamples = Util.FloatToLE16(floatSamples);
          nwritten = sline.write(byteSamples,0,byteSamples.length);
       } // end while

   } catch (Exception e){
     e.printStackTrace();
   } 
   iline.stop(); iline.flush(); iline.close();
   sline.stop(); sline.flush(); sline.close();

   System.out.println("Player thread end");
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) return;
    File inputFile = new File(args[0]);
  }
  
}
