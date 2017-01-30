import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

abstract class BufferedEffector {
 
  protected BufferedEffector(int channels, float samplingRate){
    this.channels = channels;
    this.samplingRate = samplingRate;
/*
     inputBuffer = new LinkedList<Float>();  
     outputBuffer = new LinkedList<Float>();  
*/
     inputBuffer = new ArrayList<Float>();  
     outputBuffer = new ArrayList<Float>();  
  }

  abstract public int putSamples(float[] data, int len);
  abstract public int putSamples(float[] data);
  abstract public float[] getSamples();
  abstract public void flush();

  protected List<Float> inputBuffer;
  protected List<Float> outputBuffer;
  protected int channels;
  protected float samplingRate;
  protected int outLimit;
  protected boolean flushing;
  abstract protected void process();

}
