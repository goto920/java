Working directory

Compilation: 
javac -cp ../jarlib/JTransforms-3.1-with-dependencies.jar:../jarlib/jl1.0.1.jar:. *.java
Execution:
java -cp ../jarlib/JTransforms-3.1-with-dependencies.jar:../jarlib/jl1.0.1.jar:. TimeStretchPlayer



+++++++++++
目標: TimeStretchPlayer

スロー、ファースト再生可能なオーディオプレイヤ(趣味)

再設計、入出力データのバッファ等

1) TimeStretchPlayer.java GUIメインプログラム
   出力先選択
    GetPortsInfo info = new GetPortsInfo();
    mixers = info.getOutputPorts();
    String[] items = new String[mixers.length];
    で、JComboBox<String>を生成

   入力ファイル選択 -- JFileChooser() read only
     JFileChooser fc = new JFileChooser(inputDir);
     ディレクトリを保存  inputDir = inputFile.getParentFile();

   再生速度調整スライダ

   時間スライダ

   音量スライダ

   Play/Pause/resume 
    Play) startPlay(inputFile, outputPort, lastSec); // lastSecは0
          player = new PlayWaveFile(inputFile, outputPort);
          totalSec = player.getTotalTime();
          player.setGain(volumeSlider.getValue());
          player.setSkip(last);
          player.setTimeSlider(timeSlider);
          Thread pt = new Thread(player);
          pt.start();
          logText.append("Player thread start\n");
          ****** GUI log windowで表示するJTextArea

    Pause) lastSec = player.stopPlay();
    Resume) Playと同じ (lastSecを使う)

   Stop)
     生yer.stopPlay(); lastSec = 0f;

   Save  
   Open log window -- JFrame, JScrollPane, JTextArea

2) GetPortsInfo.java 入出力ポートの情報取得
     public Mixer.Info[] getInputPorts()
     public Mixer.Info[] getOutputPorts()
     
3) PlayWaveFile.java プレイヤの処理(Runnable)

   PlayWaveFile(File inputFile, Mixer.Info outputPort)
    MP3ファイルは、INPUT.mp3をINPUT.mp3.wav に変換して使う
    変換は、import javazoom.jl.converter.*を使い、

      mp3ToWaveFile(infile, outfile)
      setIO(infile, outport) -- ややこしい処理をまとめた
      public void setSkip(float skipSec) // 飛ばす秒セット
      public synchronized void setTimeSlider(Object obj) 
      private synchronized void showTime() -- タイムスライダの移動(1秒ごと)
      public float getTotalTime() -- 再生ファイルの時間長さ

   public static float[] LE16ToFloat(byte[] input, int len)
   public static byte[] FloatToLE16(float[] input)
   public void adjustGain(float[] input) // gainはインスタンス変数

4) BufferedEffector.java イフェクタの抽象クラス  

  protected List<Float> inputBuffer;
  protected List<Float> outputBuffer;
    コンストラクタで
     inputBuffer = new ArrayList<Float>();  
     outputBuffer = new ArrayList<Float>();  
     inputBuffer = new ArrayList<Float>();  
     outputBuffer = new ArrayList<Float>();  
      // LinkedListは遅かった

    abstract public int putSamples(float[] data, int len);
    abstract public int putSamples(float[] data); // なくてもよいかな?
    abstract public float[] getSamples();

    abstract protected void process(); // 処理内容
    abstract public void flush(); // 最後にたまっているものを出力

5) FFTTest.java FFT変換、逆変換           

使い方
   Playerの中で
      FFTTest effect = new FFTTest(channels,sampleRate,4096);

    run()内ループで
     float[] floatSamples = LE16ToFloat(buf, nread);
     effect.putSamples(floatSamples);
     float[] out = effect.getSamples();
     byte[] byteSamples = FloatToLE16(out);
     sline.write(byteSamples,0,byteSamples.length);

    putSamples()
     Hann window適用後
     inputBufferに書き込み
     process()
    
    process()
     window/4ずらしで、
     FFT
     逆FFT     
     outputBufferにwindow/4ずらしでオーバーラップ合計

    getSamples()
     処理が終わったところまで出力
     (flushのときは、最後まで)
++++++++++++++++++++++++++++++++++++++
