pure Javaで音楽ファイル加工

0) ライブラリ(ソースコードなし)

--- 使用ライブラリクラス
mp3変換ライブラリ(class)    : 
      jl1.0.1.jar を展開したもの 
      javazoom/ 
      http://www.javazoom.net/javalayer/sources.html 
FFTライブラリ : 
   JTransforms-3.1-with-dependencies.jar を展開
   org/jtransforms/
   依存pl/edu/icm/
   https://github.com/wendykierp/JTransforms

1) 自作ライブラリ: jp/kmgoto/music/

参考にしたプログラム
 SoundTouch(cpp) http://www.surina.net/soundtouch/
   TimeStretchなど
 TarsosDSP(java)
   https://github.com/JorenSix/TarsosDSP
   TimeStretchあるが、モノラル専用

A) 汎用
 GetPortsInfo.java -- Java VMで認識されるオーディオ入出力ポートの情報をGET
   public Mixer.Info[] getInputPorts()
   public Mixer.Info[] getOutputPorts()  
   BiquadEQ.java -- floatで計算するBiquad Equalizer
     from http://www.musicdsp.org/files/Audio-EQ-Cookbook.txt
    
 Util.java 
   16bit LE とfloatの音声サンプル変換、逆変換
     public static float[] LE16ToFloat(byte[] input, int len)
     public static float[] LE16ToFloat(byte[] input) -- len = input.length 
     input
       left  byte[4*n + 0] LSB byte[4*n + 1] MSB
       right  byte[4*n + 2] LSB byte[4*n + 3] MSB
     returns
       left  float[2*n + 0]
       right float[2*n + 1]
       
     public static byte[] FloatToLE16(float[] input)

   floatの状態でゲイン調整
     public static void adjustFloatGain(float[] input, float gain)

   mp3ファイルをwavファイルに変換して出力
     public static void mp3ToWaveFile(String inputFile, String outputFile)


B) 楽器音分離用

FFTChunk.java -- FFTのデータの入れ物(データ自体は外部で作成)
  private int channels;
  private int windowSize;
  private int windowShift;
  private float[] left, right; // (real, imag) * channels * windowSize
   -- フーリエ係数 
         ステレオの場合 (左) 実虚 (右) 実虚.... の順)
  private float[] pan, panAmp; -- ステレオの場合にしか意味なし
         各サンプルに対し
             pan (-1(L) 0(center) 1(R))
             panAmp (強さ、使わないかも)
  private float[] percL, percR;
    左、右サンプルのpercussive値 (0..1)
    (モノラルの場合は左だけ)

BufferedEffector.java -- イフェクタのバッファ処理のための抽象クラス
   inputBuffer = new ArrayList<Float>();  
   outputBuffer = new ArrayList<Float>();  

   public int putSamples(float[] data, int len) -- バッファに入れる 
   public int putSamples(float[] data) 
      abstract protected void process(); 
       -- putSamplesで実行される (子クラスで実装)

   abstract public float[] getSamples(); -- とりだし
   abstract public void flush(); -- 最後にたまったものを吐き出す指示
    (flush()後にgetSamples())
   これらは、子クラスで実装

  FFTforward.java (extends BufferedEffector) -- 順方向FFT 
    -- window/4ずつシフト、Han windowかけて、FFT 
    入力 putSamples(float[]) 
    出力 public FFTChunk getFFTChunk() (出力なければreturn null)
         (getSamples()は使わない) 

  IFFToverlapAdd.java (extends BufferedEffector) 
        -- FFT逆変換とシフトしたものを再合成
    入力 putFFTChunk(FFTChunk) (putSamples()は使わない)  
    出力 float[] getSamples() 
         flush()
    処理内容 protected void process()

PanCalculator.java -- Panを計算してFFTChunkに代入 
  FFTChunk単位処理でバッファリング不要
  入力 putFFTChunk(FFTChunk chunk)
  出力 FFTChunk getFFTChunk()
  処理内容 protected void calcPan(FFTChunk)

PercCalculator.java -- 左右の音のpercussive値を独立に計算してFFTChunkに入れる
  バファリング処理がややこしいがこれでかなり簡単になった
  入力 putFFTChunk(FFTChunk)
  出力 FFTChunk getFFTChunk(), flush()
   処理 calcPMedian() -- 周波数方向に指定(17が適当)した範囲のmedian
        calcHMedian() -- 時間方向に同上、さらにpercussiveの度合い(0..1)を
          FFTChunkに代入

FFTChunkの値を使ったフィルタリング例
 PanFilter.java -- Panでフィルタ (テスト)
   public void filter(FFTChunk chunk)
    -- FFTChunkの中のフーリエ係数を変更
 PercFilter.java -- Percussiveフィルタ (テスト用)
   public void filter(FFTChunk chunk)
    -- FFTChunkの中のフーリエ係数を変更

その他
  TimeStretch.java -- 再生速度変更 (一応機能する)
  PitchFinder.java -- ピッチ検出(点検中)、ギターフィードバックイフェクタ用
  BPMDetect.java -- BPM検出 (未実験)
   PeakFinder.java -- データのピーク検出 (BPMDetect用だが他でも使えるかなあ)
  Spectrogram.java 
     -- 一応機能するが、時間変化が速すぎて、表示を見ても役に立たない
---------------------------------------------
Player中の処理順 (例外処理省略)

 public void  run() {
   FFT, IFFTインスタンス生成
   while(入力ストリームが終わるまで){
     LE16サンプルを入力ストリームから読み込む
     floatに変換
     FFTforward(インスタンス)にputSamples(float[])
        getFFTChunk()

     PanCalculator(インスタンス)にputFFTChunk(FFTChunk)
        getFFTChunk()

     PercCalculator(インスタンス)にputFFTChunk(FFTChunk)
        getFFTChunk()

     FFTChunkを使うフィルタ

     (ゲイン調整)

     floatからLE16サンプルに変換
     LE16サンプルを出力先に書き込む 
   }

   使用順にflush()
   データがあれば出力先に書き込む

 }
+++++++++++++++++++
Mixer字化け問題

ByteArrayInputStream　で、このバイト列を入力ストリームとして、

ByteArrayInputStream is = new ByteArrayInputStream(binary);
String charsetName = "JISAutoDetect";
InputStreamReader inputreader = new InputStreamReader(is, charsetName);
BufferedReader br = new BufferedReader(inputreader);

みたいな形で文字を読みこめば、文字化けを治すことができます。
ByteBufferと、CharsetDecoderでも出来るかも。
++++++++++++++++
ピッチ調整

https://github.com/JorenSix/TarsosDSP/blob/master/src/examples/be/tarsos/dsp/example/PitchShiftingExample.java
Originally from https://github.com/dnault/libresample4j/

import be.tarsos.dsp.resample.RateTransposer;
  using resample http://www-ccrma.stanford.edu/~jos/resample/
  https://ccrma.stanford.edu/~jos/resample/

private double currentFactor;// pitch shift factor
private double sampleRate;

public static double centToFactor(double cents){
return 1 / Math.pow(Math.E,cents*Math.log(2)/1200/Math.log(Math.E)); 
}

wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.musicDefaults(currentFactor, sampleRate));
rateTransposer = new RateTransposer(currentFactor);

dispatcher.addAudioProcessor(wsola);
dispatcher.addAudioProcessor(rateTransposer);

processing size (at least 4096)

public RateTransposer(double factor){
  this.factor = factor;
  r= new Resampler(false,0.1,4.0); // advance, min, max
  this.Nmult = highQuality ? 35 : 11;

process()
  float[] out = new float[(int) (src.length * factor)];
  r.process(factor, src, 0, src.length, false, out, 0, out.length);

Low pass filter
    FilterKit.lrsLpFilter(Imp64, this.Nwing, 0.5 * Rolloff, Beta, Npc);
    this.Imp = new float[this.Nwing]; 
    this.ImpD = new float[this.Nwing];

int Nout;
if (factor >= 1) { // SrcUp() is faster if we can use it */
  Nout = lrsSrcUp(this.X, this.Y, factor, /* &this.Time, */
         Nx, Nwing, LpScl, Imp, ImpD, interpFilt);
} else {
 Nout = lrsSrcUD(this.X, this.Y, factor, /* &this.Time, */
        Nx, Nwing, LpScl, Imp, ImpD, interpFilt);

    /*
     * Sampling rate up-conversion only subroutine; Slightly faster than
     * down-conversion;
     */
   private int lrsSrcUp(float X[], float Y[], double factor, 
       int Nx, int Nwing, float LpScl, float Imp[], 
       float ImpD[], boolean Interp) {
}
++++++++++++++++++++++++++++++++++++++++++++++++++
Low latency audio I/O

https://java.net/downloads/gervill/fosdem/fosdem2011/slides.pdf
(2011)

Linux ALSA -- 5msec 
      PulseAudio 25msec
Windows -- 100msec

Linux -- JJack
Windows JasioHost
Mac -- Mandoline M3D Mixer

Suggestion to Java Sound -- pull-based API

JAudioLibs’ AudioServer API (2013)

https://github.com/jaudiolibs/examples/blob/master/src/main/java/org/jaudiolibs/examples/PassThroughAudioClient.java

http://quod.lib.umich.edu/cgi/p/pod/dod-idx/real-time-low-latency-audio-processing-in-java.pdf?c=icmc;idno=bbp2372.2007.131 (2007)

Decklight 4 using RtAudio (JNI)
https://www.music.mcgill.ca/~gary/rtaudio/ (C++)
Java wrapper

https://code.google.com/archive/p/jrtaudio/
http://www.beadsproject.net/

PortAudio (Java Binding)
https://app.assembla.com/spaces/portaudio/git/source
 -- Java binding included but no call back support

Beads http://www.beadsproject.net/

Java callback
http://qiita.com/__zck__/items/86c0a7ab9de4f79eacdb
 (ちょっと回りくどい気がするが)

https://gist.github.com/rjeschke/1210000 -- 効果あるのかわからん

以上
