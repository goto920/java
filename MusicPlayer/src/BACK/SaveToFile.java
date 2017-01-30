import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import jp.kmgoto.music.*;

public class SaveToFile {

  private AudioInputStream ais;
  private boolean running;
  private int channels;
  private int frameSize;
  private float frameRate,sampleRate;
  private float gain, tempo, currentTime = 0f, lastTime = 0f, totalTime;
  private boolean bigEndian;
  private long currentFrame = 0, skipTo = 0;
  private File tmpFile = null;
  private TimeStretch timeStretch = null;
  private File outputFile = null;
  private AudioFormat format; 

  SaveToFile(File inputFile, File outputFile) throws Exception {
    if (inputFile.getName().toUpperCase().endsWith(".MP3")){
      String out = inputFile.getName() + ".wav";
      tmpFile = new File(out);
        // if out does not exist 
      Util.mp3ToWaveFile(inputFile.getPath(),out); 
      setOutput(tmpFile,outputFile);
    } else setOutput(inputFile, outputFile);

    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
    tempo = 1.0f;
  }

  private void setOutput(File inputFile, File outputFile) throws Exception {
    ais = AudioSystem.getAudioInputStream(inputFile);

    format = ais.getFormat();
    frameSize = format.getFrameSize();
    frameRate = format.getFrameRate();
    sampleRate = format.getSampleRate();
    bigEndian  =  format.isBigEndian();
    channels   = format.getChannels();

    this.outputFile = outputFile;
  }

  public synchronized void setTempo(int intTempo){ // 100 scale
    tempo = intTempo/100f;
    if (timeStretch != null) timeStretch.setTempo(tempo);
  }

  public synchronized void setGain(int intGain){ // 100 scale
    gain = intGain/100f;
  }

  public float getTotalTime(){
    long len = ais.getFrameLength();
    AudioFormat format = ais.getFormat();
    totalTime = len/format.getFrameRate();
    return totalTime;
  }

  public void run() {
    byte[] buf = new byte[1024];
    running = true;
    timeStretch = new TimeStretch(channels,sampleRate);
   // BufferedEffector effector = (BufferedEffector) timeStretch;
    setTempo(50); 
    List<Byte> buffer = new ArrayList<Byte>();

    try {
       int nread;
       float[] floatSamples, out = null;
       byte[] byteSamples = null;
       long total_read = 0;

       while((nread = ais.read(buf)) > 0) {
          total_read += nread;
          currentFrame += nread/frameSize; 
          floatSamples = Util.LE16ToFloat(buf, nread); 

          timeStretch.putSamples(floatSamples); 
          out = timeStretch.getSamples();

//        System.out.println("nread/out " + total_read + "/" + out.length);

          if (out.length > 0){
            Util.adjustFloatGain(out,gain);
            byteSamples = Util.FloatToLE16(out);
            for (int i = 0; i < byteSamples.length; i++)
                           buffer.add(byteSamples[i]);
          }

       } // end while

/*
      timeStretch.flush();
      while (true){
        out = timeStretch.getSamples();
        System.out.println("flush out " + out.length);
        if (out.length > 0){
            Util.adjustFloatGain(out,gain);
            byteSamples = Util.FloatToLE16(out);
            for (int i = 0; i < byteSamples.length; i++)
                   buffer.add(byteSamples[i]);
        } else break;
      }
*/

      System.out.println("Buffer size " + buffer.size() 
          + " total input " + total_read);

      byte[] outbytes =  new byte[buffer.size()];
      for (int i=0; i < buffer.size(); i++) 
           outbytes[i] = buffer.get(i).byteValue();

      AudioInputStream ois = 
       new AudioInputStream(
         new ByteArrayInputStream(outbytes), format, buffer.size());
       // in the same format
     
      AudioSystem.write(ois, AudioFileFormat.Type.WAVE, outputFile);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
    }
    System.out.println("File writer thread end");
  }

  public static void main(String[] args) throws Exception {
    File inputFile = new File(args[0]);
    File outputFile = new File(args[0] + "-out.wav");
    SaveToFile player = new SaveToFile(inputFile,outputFile);
    System.out.println("total time: " + player.getTotalTime());
    player.run();
    System.out.println("main end");
  }
  
}
