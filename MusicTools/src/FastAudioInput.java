import java.io.*;
import java.util.Scanner;
import javax.sound.sampled.*;
import jp.kmgoto.music.*;

public class FastAudioInput {

  public static void main(String[] args) throws Exception {

    AudioFormat format = new AudioFormat(
           AudioFormat.Encoding.PCM_SIGNED,
           44100f, 16, 1, 2, 44100f,false); // 2,4 Stereo 1,2 mono
    System.out.println("AudioFormat: " + format.toString());

    GetPortsInfo info = new GetPortsInfo();

    Mixer.Info[] inputMixers = info.getInputPorts();  
    Mixer.Info[] outputMixers = info.getOutputPorts();  

    Scanner scan = new Scanner(System.in); 

    for (int i=0; i < inputMixers.length; i++)
       System.out.println("In  " + i + ": " + inputMixers[i].getName());

    System.out.print("Select input: ");
    int imixer = Integer.parseInt(scan.next());
    
    for (int i=0; i < outputMixers.length; i++)
       System.out.println("Out " + i + ": " + outputMixers[i].getName());

    System.out.print("Select output: ");
    int omixer = Integer.parseInt(scan.next());

    System.out.println("TargetDataLine ");
    TargetDataLine iline 
        = AudioSystem.getTargetDataLine(format, inputMixers[imixer]);
    System.out.println("SourceDataLine ");
    SourceDataLine oline 
        = AudioSystem.getSourceDataLine(format, outputMixers[omixer]);

    System.out.print("processUnit(512, 1024 etc.) : ");
    int processUnit = Integer.parseInt(scan.next());
//    int bufsize = processUnit*4; // 16 bit LR

    System.out.println("ByteRingBuffer");
    ByteRingBuffer buffer = new ByteRingBuffer(processUnit*4);

    System.out.println("iline.open()");
    iline.open(format, 16);
    System.out.println("FastAudioCapture");
    FastAudioCapture capture = new FastAudioCapture(iline, buffer); 
    System.out.println("Thread");
    Thread rt = new Thread(capture);
    rt.start();
    iline.start();

    System.out.println("write to audio");
    oline.open(format, processUnit*8);
    oline.start();
    byte[] obuff;
    while(true){
      obuff = buffer.get(processUnit*4);
      if (obuff != null) {
        // System.out.println("got " + obuff.length);
        oline.write(obuff,0,obuff.length);
      } 
    }

  }
}

class FastAudioCapture implements Runnable {

      private TargetDataLine iline;
      private ByteRingBuffer rbuff; 

      FastAudioCapture(TargetDataLine iline, ByteRingBuffer rbuff){
        System.out.println("AudioCapture constructor");
        this.iline = iline; 
        this.rbuff = rbuff; 
      }

     public void run(){
      System.out.println("input thread");
      byte[] buff = new byte[4096];
      while(true){
        int avail = iline.available();
        if(avail > 0) {
          iline.read(buff,0,avail);
          rbuff.put(buff,avail);
//          System.out.println(avail);
        }
        // try {Thread.sleep(1); } catch (Exception e) {}
      }
    }
  }

