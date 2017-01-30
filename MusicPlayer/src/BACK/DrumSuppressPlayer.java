import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import jp.kmgoto.music.*;

public class DrumSuppressPlayer implements Runnable {

  private AudioInputStream ais;
  private File outputFile = null;
  private SourceDataLine sline;
  private boolean running;
  private int channels;
  private int frameSize;
  private float frameRate,sampleRate;
  private float gain, tempo, currentTime = 0f, lastTime = 0f, totalTime;
  private JSlider timeSlider;
  private boolean bigEndian;
  private long currentFrame = 0, skipTo = 0;
  private AudioFormat format;

  DrumSuppressPlayer(File inputFile, File outputFile) throws Exception {
    List<Byte> buffer = new ArrayList<Byte>();
    setOutput(checkMP3(inputFile), outputFile);
    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
  }

  DrumSuppressPlayer(File inputFile, Mixer.Info outputPort) throws Exception {
    setOutput(checkMP3(inputFile), outputPort);
    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
  }

  private File checkMP3 (File inputFile) throws Exception {
    File tmpFile = inputFile;
    if (inputFile.getName().toUpperCase().endsWith(".MP3")){
      String out = inputFile.getName() + ".wav";
      tmpFile = new File(out);
      Util.mp3ToWaveFile(inputFile.getPath(),out); 
    } 
    return tmpFile;
  }

  private void setOutput(File inputFile, File outputFile) throws Exception {
    this.outputFile = outputFile;
    ais = AudioSystem.getAudioInputStream(inputFile);

    AudioFormat format = ais.getFormat();
    frameSize = format.getFrameSize();
    frameRate = format.getFrameRate();
    sampleRate = format.getSampleRate();
    bigEndian  =  format.isBigEndian();
    channels   = format.getChannels();
  }

  private void setOutput(File inputFile, Mixer.Info mixer) throws Exception {
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

    sline.open(format);
  }

/*
  public synchronized void setTempo(int intTempo){ // 100 scale
    tempo = intTempo/100f;
    if (timeStretch != null) timeStretch.setTempo(tempo);
  }
*/

  public synchronized void setGain(int intGain){ // 100 scale
    gain = intGain/100f;
  }

  public void setSkip(float skipSec){ 
     skipTo = (long) ((skipSec*sampleRate)*frameSize); 
  }

  public synchronized float stopPlay(){
    running = false;
    return currentFrame/sampleRate;
  }

  public synchronized void setTimeSlider(Object obj){
    if (obj instanceof JSlider) timeSlider = (JSlider) obj;
  }

  private synchronized void showTime(){
    currentTime = currentFrame/sampleRate;
    if ((currentTime > lastTime + 1f) && (timeSlider != null)) {
        timeSlider.setValue((int) currentTime);
        lastTime = currentTime;
    }
  }

  public float getTotalTime(){
    long len = ais.getFrameLength();
    AudioFormat format = ais.getFormat();
    totalTime = len/format.getFrameRate();
    return totalTime;
  }

  public void run() {
    byte[] buf = new byte[1024];
    currentFrame = skipTo/frameSize;
    running = true;
    List<Byte> buffer = new ArrayList<Byte>();

    try {
       ais.skip(skipTo);
/*
       if (outputFile == null) 
         sline.start();
       else 
         List<Byte> buffer = new ArrayList<Byte>();
*/

       int nread;
       float[] floatSamples, out = null;
       byte[] byteSamples;

       while((nread = ais.read(buf)) > 0 && running) {
          currentFrame += nread/frameSize; 
          floatSamples = Util.LE16ToFloat(buf, nread); 
          showTime();

          if (out.length > 0){
            Util.adjustFloatGain(out,gain);
            byteSamples = Util.FloatToLE16(out);
            if (outputFile == null){
               for (int i = 0; i < byteSamples.length; i++)
                  buffer.add(byteSamples[i]);
            } else { 
              sline.write(byteSamples,0,byteSamples.length);
            }

          }

       } // end while

       byte[] outbytes =  new byte[buffer.size()];
       for (int i=0; i < buffer.size(); i++) 
           outbytes[i] = buffer.get(i).byteValue();

       AudioInputStream ois = 
           new AudioInputStream(
             new ByteArrayInputStream(outbytes), format, buffer.size()
           );
       AudioSystem.write(ois, AudioFileFormat.Type.WAVE, outputFile);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (outputFile == null){
       sline.drain(); sline.stop(); sline.close();
      } 
    }
    System.out.println("Player thread end");
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) return;

    File inputFile = new File(args[0]);
    File outputFile = null;
      outputFile = new File(args[0] + "-nodrums.wav");
      DrumSuppressPlayer player = new DrumSuppressPlayer(inputFile,outputFile); 
      System.out.println("total time: " + player.getTotalTime());
      player.run();
      System.out.println("main end");
  }
  
}
