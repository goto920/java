package jp.kmgoto.music;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

public abstract class BufferedEffector {
 
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

  public int putSamples(float[] data, int len){
     for (int i = 0; i < len; i++) inputBuffer.add(data[i]);
     process();
     return len;
  }

  public int putSamples(float[] data){
      return putSamples(data, data.length);
  }

  abstract public float[] getSamples();
  abstract public void flush();

  protected List<Float> inputBuffer;
  protected List<Float> outputBuffer;
  protected int channels;
  protected float samplingRate;
  protected int outLimit, added;
  protected boolean flushing;
  abstract protected void process();

}
