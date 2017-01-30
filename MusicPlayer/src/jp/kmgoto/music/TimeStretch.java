/*
 based on http://www.surina.net/soundtouch
  TDStretch.h, cpp
  
*/

package jp.kmgoto.music;

// import jp.kmgoto.music.*;

public class TimeStretch extends BufferedEffector {
  // private int channels;
  private int sampleReq;
  private boolean flushing;

  private int overlapLength;
  private int seekLength;
  private int seekWindowLength;
  // private int overlapDividerBitsNorm;
  // private int overlapDividerBitsPure;
  // privae  int slopingDivider;
  // private int sampleRate;

  private int sequenceMs;
  private int seekWindowMs;
  private int overlapMs = 8;

  // unsigned long maxnorm;
  private float maxnormf;

  private float tempo;
  private float nominalSkip;
  private float skipFract;

  private boolean bQuickSeek;
  private boolean bAutoSeqSetting;
  private boolean bAutoSeekSetting;

//  float[] pMidBufferUnaligned;
//  float[] pMidBuffer;
  private float[] pMidBuffer;
  private float[] pRefMidBuffer;

 //  FIFOSampleBuffer outputBuffer;
 //  FIFOSampleBuffer inputBuffer;

 // Constants in header files
  private final int DEFAULT_SEQUENCE_MS = 0; // AUTO
  private final int DEFAULT_SEEKWINDOW_MS = 0; // AUTO
  private final int DEFAULT_OVERLAP_MS = 8; // 8 m sec

  private void acceptNewOverlapLength(int newOverlapLength){

    int prevOvl = overlapLength;
    overlapLength = newOverlapLength;
    if (overlapLength > prevOvl){
      pMidBuffer = new float[overlapLength*channels *8];
      pRefMidBuffer = new float[overlapLength*channels *8];
// pMidBuffer = (SAMPLETYPE *)
// SOUNDTOUCH_ALIGN_POINTER_16(pMidBufferUnaligned);
    } 

    clearMidBuffer();
  }

  private void clearCrossCorrState(){
   // Empty
  }

  private void calculateOverlapLength(int overlapMs){ // tarsos version
     int newOverlapLength = (int) (samplingRate * overlapMs/1000);
//     seekWindowLength = (int) (samplingRate * sequenceMs/1000);
     acceptNewOverlapLength(newOverlapLength);
  }

  private float calcCrossCorr(float[] data, int refPos, float[] compare){
    float corr=0f;
    float norm=0f;

    for (int i=0; i < channels * overlapLength; i += 2){
      int index = refPos + i;
      corr += data[index] * compare[i] 
              + data[index + 1] * compare[i + 1];
      norm += data[index] * data[i]  
              + data[index + 1] * data[i + 1];
    }

    return (float) (corr / Math.sqrt(norm));
  }

  // virtual int seekBestOverlapPositionFull(const SAMPLETYPE *refPos);
  //  virtual int seekBestOverlapPositionQuick(const SAMPLETYPE *refPos);
  //  virtual int seekBestOverlapPosition(const SAMPLETYPE *refPos);

  private int seekBestOverlapPositionFull(float[] data){
  // mixed version (no pointer in Java)
    int bestOffset = 0;
    float bestCorrelation = -10f;
    float norm;

    bestCorrelation = calcCrossCorr(data, 0, pMidBuffer);

    for (int tempOffset = 1; tempOffset < seekLength; tempOffset++){
       float corr 
          = calcCrossCorr(data, channels * tempOffset, pMidBuffer);
       if (corr > bestCorrelation) {
         bestCorrelation = corr;
         bestOffset = tempOffset; 
       }
    }

    return bestOffset;
  }

  // virtual void overlapStereo(SAMPLETYPE *output, 
  //   const SAMPLETYPE *input) const;
  // virtual void overlapMono(SAMPLETYPE *output, 
  //   const SAMPLETYPE *input) const;
  //  virtual void overlapMulti(SAMPLETYPE *output, 
  //   const SAMPLETYPE *input) const;

  private void overlapMulti(float[] output, float[] input, int ovlPos){
    float m1 = 0f, m2;

    int i = 0;
    int base = channels*ovlPos;

    for (m2 = (float) overlapLength; m2>0; m2--){
       for (int c = 0; c < channels; c++){
         output[i] = (input[base+i]*m1 + pMidBuffer[i]*m2) / overlapLength;
         i++;
       }
      m1++;
    }
   
  }

  private void clearMidBuffer(){
   // memset(pMidBuffer, 0, channels * sizeof(SAMPLETYPE) * overlapLength);
   for (int i = 0; i < pMidBuffer.length; i++) pMidBuffer[i] = 0f;
  }

/*
  private void overlap(float[] output, float[] input, int ovlPos){
  }
*/

  private void calcSeqParameters(){


    final float AUTOSEQ_TEMPO_LOW = 0.5f;
    final float AUTOSEQ_TEMPO_TOP = 2.0f;
    final float AUTOSEQ_AT_MIN = 125.0f;
    final float AUTOSEQ_AT_MAX = 50.0f;
    final float AUTOSEQ_K =  (AUTOSEQ_AT_MAX - AUTOSEQ_AT_MIN)
      / (AUTOSEQ_TEMPO_TOP - AUTOSEQ_TEMPO_LOW);
    final float AUTOSEQ_C = (AUTOSEQ_AT_MIN - AUTOSEQ_K * AUTOSEQ_TEMPO_LOW); 

    final float AUTOSEEK_AT_MIN = 25.0f;
    final float AUTOSEEK_AT_MAX = 15.0f;
    final float AUTOSEEK_K = (AUTOSEEK_AT_MAX - AUTOSEEK_AT_MIN) 
      / (AUTOSEQ_TEMPO_TOP - AUTOSEQ_TEMPO_LOW);
    final float AUTOSEEK_C = 
         AUTOSEEK_AT_MIN - AUTOSEEK_K * AUTOSEQ_TEMPO_LOW;

    float seq = AUTOSEQ_C + AUTOSEQ_K * tempo;
    if (seq > AUTOSEQ_AT_MAX) seq = AUTOSEQ_AT_MAX; 
    if (seq < AUTOSEQ_AT_MIN) seq = AUTOSEQ_AT_MIN; 
    sequenceMs = (int)(seq + 0.5);

    float seek = AUTOSEEK_C + AUTOSEEK_K * tempo;
    if (seek > AUTOSEEK_AT_MAX) seq = AUTOSEEK_AT_MAX; 
    if (seq < AUTOSEEK_AT_MIN) seq = AUTOSEEK_AT_MIN; 
    seekWindowMs = (int)(seek + 0.5);


    seekWindowLength = (int) ((samplingRate * sequenceMs) / 1000);

    if (seekWindowLength < 2 * overlapLength) 
        seekWindowLength = 2 * overlapLength;

    seekLength = (int) ((samplingRate * seekWindowMs) / 1000);
/*
    System.out.println("In calcSeqParameters -- ");
    System.out.println("samplingRate: " + samplingRate);
    System.out.println("seekWindowMs: " + seekWindowMs);
    System.out.println("overlapLength: " + overlapLength);
    System.out.println("seekLength: " + seekLength);
    System.out.println("seekWindowLength: " + seekWindowLength);
*/

  }

  private void adaptNormalizer(){
  }

  private void processSamples(){
     int ovlSkip, offset;

/*
     if (flushing) {
       for (int i=0; i < inputBuffer.size() ; i++) 
           outputBuffer.add(inputBuffer.get(i));
       return;
     }
*/

     while(inputBuffer.size() >= channels*sampleReq){

        Float[] input = inputBuffer
            .subList(0,channels*sampleReq).toArray(new Float[0]); 
        float[] data = new float[input.length];
        for (int i=0; i < input.length; i++) data[i] = input[i].floatValue();

        // offset = seekBestOverlapPosition(inputBuffer.ptrBegin());
        offset = seekBestOverlapPositionFull(data);
//        System.out.println("processSamples() offset " + offset);

        // overlap(
        //   outputBuffer.ptrEnd((uint)overlapLength), 
        //   inputBuffer.ptrBegin(), (uint)offset);
        float[] output = new float[channels*overlapLength];
        overlapMulti(output,data,offset);
        //   outputBuffer.putSamples((uint)overlapLength); // capacity 

        for (int i=0; i < output.length ; i++) outputBuffer.add(output[i]);

        int tmp = (seekWindowLength - 2 * overlapLength);

       // outputBuffer.putSamples(inputBuffer.ptrBegin() 
       // + channels * (offset + overlapLength), (uint)temp);

       int base = channels*(offset + overlapLength);
       for (int i=0; i < channels*tmp ; i++) outputBuffer.add(data[base+i]);

       // for 
       // outputBuffer.add();

       // memcpy(pMidBuffer, inputBuffer.ptrBegin() 
       // + channels * (offset + temp + overlapLength), 
       //     channels * sizeof(SAMPLETYPE) * overlapLength);

       base = channels*(offset + tmp + overlapLength);
       for (int i = 0; i < channels * overlapLength; i++)
         pMidBuffer[i] = inputBuffer.get(base+i).floatValue();

       skipFract += nominalSkip; 
       ovlSkip = (int) skipFract;  
       skipFract -= ovlSkip;       

//      inputBuffer.receiveSamples(ovlSkip);
       inputBuffer.subList(0,channels*ovlSkip).clear();
     } // end while

  }


@Override
  protected void process(){
    processSamples();
  }

// public section

  public TimeStretch(int channels, float samplingRate){

    super(channels, samplingRate);
    this.channels = channels;
    pMidBuffer = null;
    pRefMidBuffer = null;
    overlapLength = 0;

    maxnormf = 1e8f;
    skipFract = 0;

    tempo = 1.0f;
    setParameters(samplingRate, 
        DEFAULT_SEQUENCE_MS, DEFAULT_SEEKWINDOW_MS, DEFAULT_OVERLAP_MS);

    setTempo(tempo);
//    clear();
    flushing = false;
  }

@Override
  public float[] getSamples(){
    Float[] out = outputBuffer.toArray(new Float[0]);
    float[] retval= new float[outputBuffer.size()];
    for (int i=0; i< out.length; i++) retval[i] = out[i].floatValue(); 
    outputBuffer.clear();
    return retval;
  }

@Override  
  public void flush(){
    flushing = true;
  }

// putSamples() inherited

  public void setTempo(float newTempo){
    tempo = newTempo;
    calcSeqParameters();
    nominalSkip = tempo * (seekWindowLength - overlapLength);
    int intskip = (int)(nominalSkip + 0.5);
    sampleReq = Math.max(
        intskip + overlapLength, seekWindowLength) + seekLength;
//    System.out.println("setTempo() sampleReq: " + sampleReq);
  }

  public void clear(){}

 // void clearInput();
 // void setChannels(int numChannels);
 // void enableQuickSeek(bool enable);
 // bool isQuickSeekEnabled() const;

 public void setParameters(float samplingRate, 
   int sequenceMs, int seekwindowMS, int overlapMS){

   calcSeqParameters();

   calculateOverlapLength(overlapMs);
   setTempo(tempo);
 } 

 // void getParameters
 // virtual void putSamples( data, len);

/*
 public int getInputSampleReq(){
   return 0;
 } 
*/

  public static void main(String[] args){
     TimeStretch ts = new TimeStretch(2, 44100f);
  }

}
