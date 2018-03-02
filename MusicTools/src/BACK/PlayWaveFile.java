import java.io.*;
import javax.sound.sampled.*;
import javax.swing.JSlider;
import jp.kmgoto.music.*;

import javazoom.jl.converter.*;
// import javazoom.jl.decoder.JavaLayerException;

public class PlayWaveFile implements Runnable {

  private AudioInputStream ais;
  private SourceDataLine sline;
  private boolean running;
  private int channels;
  private int frameSize;
  private float frameRate,sampleRate;
  private float gain, tempo, currentTime = 0f, lastTime = 0f, totalTime;
  private JSlider timeSlider;
  private boolean bigEndian;
  private long currentFrame = 0, skipTo = 0;
  private File tmpFile = null;
  private TimeStretch timeStretch = null;

  PlayWaveFile(File inputFile, Mixer.Info outputPort) throws Exception {
    if (inputFile.getName().toUpperCase().endsWith(".MP3")){
      String out = inputFile.getName() + ".wav";
      tmpFile = new File(out);
        // if out does not exist 
      mp3ToWaveFile(inputFile.getPath(),out); 

      setIO(tmpFile,outputPort);
    } else setIO(inputFile, outputPort);

    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
    tempo = 1.0f;
  }

  private void setIO(File inputFile, Mixer.Info mixer) throws Exception {
    ais = AudioSystem.getAudioInputStream(inputFile);

    AudioFormat format = ais.getFormat();
    frameSize = format.getFrameSize();
    frameRate = format.getFrameRate();
    sampleRate = format.getSampleRate();
    bigEndian  =  format.isBigEndian();
    channels   = format.getChannels();

    if (mixer == null){
      DataLine.Info info = new DataLine.Info(SourceDataLine.class,format);
      sline = (SourceDataLine) AudioSystem.getLine(info);
    } else
      sline = AudioSystem.getSourceDataLine(format,mixer); 

//    sline.open(format, 4096*10); 
    sline.open(format);
  }

  public synchronized void setTempo(int intTempo){ // 100 scale
    tempo = intTempo/100f;
    if (timeStretch != null) timeStretch.setTempo(tempo);
  }

  public synchronized void setGain(int intGain){ // 100 scale
    gain = intGain/100f;
  }

  public void setSkip(float skipSec){ 
     skipTo = (long) ((skipSec*sampleRate)*frameSize); 
  }

 public synchronized float stopPlay(){
//  public float stopPlay(){
    running = false;
    return currentFrame/sampleRate;
  }

  public synchronized void setTimeSlider(Object obj){
    if (obj instanceof JSlider) timeSlider = (JSlider) obj;
  }

  private synchronized void showTime(){
    currentTime = currentFrame/sampleRate;
//    System.out.println("Time: " + currentTime + " last: " + lastTime);
    if ((currentTime > lastTime + 1f) && (timeSlider != null)) {
//        System.out.println("Mod");
        timeSlider.setValue((int) currentTime);
        lastTime = currentTime;
    }
  }

  public float getTotalTime(){
    long len = ais.getFrameLength();
    AudioFormat format = ais.getFormat();
    totalTime = len/format.getFrameRate();
    // System.out.println("Frame Length: " + len + "Total time: " + totalTime);
    return totalTime;
  }

  public void run() {
    byte[] buf = new byte[1024];
    currentFrame = skipTo/frameSize;
    running = true;
    // FFTTest fftTest = new FFTTest(channels,sampleRate,4096);
    // BufferedEffector effect = new BPMDetect(channels,sampleRate);
    // timeStretch = new TimeStretch(channels,sampleRate);
    // BufferedEffector effector = (BufferedEffector) timeStretch;
    FFTforward fftForward = new FFTforward(channels,sampleRate,4096);
    IFFToverlapAdd iFFToverlapAdd 
       = new IFFToverlapAdd(channels,sampleRate,4096);
    PanCalculator panCalculator = new PanCalculator(channels,4096);
    PanFilter panFilter = new PanFilter();
    PercCalculator percCalculator = new PercCalculator(channels,4096,17);
    PercFilter percFilter = new PercFilter();
    Spectrogram spectrogram = new Spectrogram(640,44100f,4096);

    try {
       ais.skip(skipTo);
       sline.start();
       int nread;
       float[] floatSamples, out = null;
       byte[] byteSamples;

       while((nread = ais.read(buf)) > 0 && running) {
          currentFrame += nread/frameSize; 
       // insert effect here
          floatSamples = LE16ToFloat(buf, nread); 
          showTime();

// FFTTest example
/*
          fftTest.putSamples(floatSamples);
          out = fftTest.getSamples();
//        out = floatSamples; // bypass
*/

// Time Stretch Example
/*
          timeStretch.putSamples(floatSamples); 
          out = timeStretch.getSamples();
//        out = floatSamples; // bypass
*/

// FFT example
 // Forward
          fftForward.putSamples(floatSamples);
          FFTChunk chunk = fftForward.getFFTChunk();
          panCalculator.calcPan(chunk); // only one line
//          panFilter.filter(chunk);

// PercCalculator
          percCalculator.putFFTChunk(chunk);
          chunk = percCalculator.getFFTChunk();
         // if (chunk != null) chunk.dump();
//         percFilter.filter(chunk);

//  if (chunk != null) System.out.println("chunk: " + chunk.getCount());

         spectrogram.drawFFTChunk(chunk);

// Backward(inverse and overlap add)
          iFFToverlapAdd.putFFTChunk(chunk);
          out = iFFToverlapAdd.getSamples(); 
//        out = floatSamples; // bypass

          if (out.length > 0){
            adjustGain(out);
            byteSamples = FloatToLE16(out);
            sline.write(byteSamples,0,byteSamples.length);
          }

       } // end while
       fftForward.flush();
       int added = fftForward.getAdded(); // added samples in float


/*
       percCalculator.putFFTChunk(fftForward.getFFTChunk());
       percCalculator.flush();
       FFTChunk chunk = null; 
       while ((chunk = percCalculator.getFFTChunk()) != null){
         percFilter.filter(chunk);
         iFFToverlapAdd.putFFTChunk(chunk);
       }
*/

       FFTChunk chunk = fftForward.getFFTChunk(); 
       panCalculator.calcPan(chunk);
       panFilter.filter(chunk);
       iFFToverlapAdd.flush();
       out = iFFToverlapAdd.getSamples();

       System.out.println("Out len " + out.length 
            + ", Added samples: " + added);
       if (out.length > 0){
           byteSamples = FloatToLE16(out);
           sline.write(byteSamples,0,byteSamples.length - 2*added);
       }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      sline.drain();
      sline.stop();
      sline.close();
//    if (tmpFile != null) tmpFile.delete();
    }
    System.out.println("Player thread end");
  }

  public static float[] LE16ToFloat(byte[] input, int len) {
     float[] retval = new float[len/2];
     for (int i=0; i < len/2; i++){
       retval[i] = (float) (input[2*i+1] << 8 | (input[2*i] & 0xff));
     }
     return retval;
  }

   public static byte[] FloatToLE16(float[] input) {
     byte[] retval = new byte[input.length*2];
     for (int i=0; i < input.length; i++){
       if (Math.abs(input[i]) >= 32767) 
           input[i] = Math.signum(input[i])*32767f;
       short tmp = (short) input[i];
       retval[2*i]     = (byte) (tmp & 0xff);
       retval[2*i + 1] = (byte) (tmp >> 8 & 0xff);
     }
     return retval;
   }

   public void adjustGain(float[] input){
       for (int i=0; i < input.length; i++) input[i] *= gain;
   } 

   public void adjustGain(byte[] input,int len){
      float[] samples = LE16ToFloat(input,len); 
      for (int i=0; i < samples.length; i++) samples[i] *= gain;
      byte[] result = FloatToLE16(samples);
      for (int i=0; i < len; i++) input[i] = result[i];
   } 

  public static void mp3ToWaveFile(String inputFile, String outputFile)
    throws Exception {
    Converter converter = new Converter();
    converter.convert(inputFile,outputFile);
  }

  public static void main(String[] args) throws Exception {
    File inputFile = new File(args[0]);
    PlayWaveFile player = new PlayWaveFile(inputFile,null);
    Thread pt = new Thread(player);
    System.out.println("total time: " + player.getTotalTime());
    pt.start();
    pt.join();
    System.out.println("main end");
  }
  
}
