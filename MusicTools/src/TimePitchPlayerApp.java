import java.io.*;
import java.util.Locale;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jp.kmgoto.music.*;
import com.laszlosystems.libresample4j.*;

public class TimePitchPlayerApp extends JFrame // JFrameを拡張する
  implements ActionListener, ChangeListener { 
     // ActionListener (ボタン等) ChangeListener(スライダー等)
  private int fontSize; 
      // private クラスの中(全メソッド)で使う変数の定義
  private int gain;
  private JButton selectDir, selectFile, playButton, stopButton, 
          saveButton, logButton;
  private File inputDir = null;
  private File inputFile = null;
  private JLabel stretch, pitch, time, volume;
  private JSlider tempoSlider, semitoneSlider, centSlider,
          timeSlider, volumeSlider;
  private  JTextArea logText;
  private JFrame logFrame = null;
  private TimePitchPlayer player = null;
  private float totalSec;
  private float lastSec;
  private JComboBox<String> setPortCombo;
  private Mixer.Info[] mixers; 
  private Mixer.Info outputPort = null;

  TimePitchPlayerApp() throws Exception { 
   // throws Exception ここの中で例外処理はしないで、これを呼んだ方に丸投げ
    UIManager.put("FileChooser.readOnly", Boolean.TRUE);
     // クラス名.put staticメソッド
    setTitle("TimePitch Player");
     //省略した時は,自分のセットタイトル
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     // ×を押したら終了（おまじない）

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((d.width*2)/5, (d.width*2)/5);
      //画面サイズを調べてフォントを変える方法（難しい）

    logText = new JTextArea("");
    logText.setEditable(false);
    fontSize = d.width/60;
    logText.append("FontSize: " + fontSize + "\n");
       // 記録用の別ウィンドウ 気にしなくて良い
    logText.setFont(new Font("Serif", Font.PLAIN,fontSize));

    JPanel panel = new JPanel(); 
    panel.setLayout(new GridLayout(2,2));

// Just a label
    JLabel portLabel = new JLabel("Set output port");
    portLabel.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel.add(portLabel);

// output port selector 難しい
    GetPortsInfo info = new GetPortsInfo();
    mixers = info.getOutputPorts();
    String[] items = new String[mixers.length];
     // 配列名.lengthで配列のサイズがわかる
    for (int i=0; i < mixers.length; i++) items[i] = mixers[i].getName();
     //items[i]に文字列が入っている
    setPortCombo = new JComboBox<String>(items);  
    setPortCombo.setSelectedIndex(0);
    outputPort  = mixers[setPortCombo.getSelectedIndex()];
    setPortCombo.setFont(new Font("Serif", Font.PLAIN,fontSize));
    setPortCombo.addActionListener(this);
    panel.add(setPortCombo);

    JLabel fileLabel = new JLabel("Select Input File");
    fileLabel.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel.add(fileLabel);

    selectFile = new JButton("File");
    selectFile.setFont(new Font("Serif", Font.PLAIN,fontSize));
    selectFile.addActionListener(this);
    panel.add(selectFile);

    JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayout(9,1));  

    stretch = new JLabel("Play Speed: 100%");
    stretch.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(stretch);
    tempoSlider = new JSlider(50,200); // 50-200
    tempoSlider.setValue(100); //初期値
    tempoSlider.setLabelTable(tempoSlider.createStandardLabels(20)); 
     // 20おきに目盛りを入れる
    tempoSlider.setPaintLabels(true);
     // これでメモリを表示
    tempoSlider.addChangeListener(this);
    panel2.add(tempoSlider);

    pitch = new JLabel("PitchShift:  0.00");
    pitch.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(pitch);
    semitoneSlider = new JSlider(-6,6); 
    semitoneSlider.setValue(0); //初期値
    semitoneSlider.setLabelTable(semitoneSlider.createStandardLabels(1)); 
    semitoneSlider.setPaintLabels(true);
    semitoneSlider.addChangeListener(this);
    panel2.add(semitoneSlider);

    centSlider = new JSlider(-100,100); 
    centSlider.setValue(0); //初期値
    centSlider.setLabelTable(centSlider.createStandardLabels(20)); 
    centSlider.setPaintLabels(true);
    centSlider.addChangeListener(this);
    panel2.add(centSlider);



    time = new JLabel("Time: "); // 上と同じ
    time.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(time);
    timeSlider = new JSlider();
    timeSlider.addChangeListener(this);
    panel2.add(timeSlider);

    volume = new JLabel("Volume:  70");
    volume.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(volume);
    volumeSlider = new JSlider(0,100); volumeSlider.setValue(70); 
    volumeSlider.setLabelTable(volumeSlider.createStandardLabels(10));
    volumeSlider.setPaintLabels(true);
    volumeSlider.addChangeListener(this);
    panel2.add(volumeSlider);

    JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayout(2,2));
    playButton = new JButton("Play");
    playButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    playButton.addActionListener(this);
    panel3.add(playButton);
    stopButton = new JButton("Stop");
    stopButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    stopButton.addActionListener(this);
    panel3.add(stopButton);
    saveButton = new JButton("Save to file");
    saveButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    saveButton.addActionListener(this);
    panel3.add(saveButton);
    logButton = new JButton("Open log window");
    logButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    logButton.addActionListener(this);
    panel3.add(logButton);

    add(panel, BorderLayout.NORTH);
    add(panel2, BorderLayout.CENTER);
    add(panel3, BorderLayout.SOUTH);
    
    pack();
    setVisible(true);  
    lastSec = 0.0f; //あまり関係ない
  }

  @Override
  public synchronized void actionPerformed(ActionEvent event) {

    Object obj = event.getSource(); // このeventは何がだしたか

    if (obj == selectFile){
       logText.append("selectFile\n");
       JFileChooser fc = new JFileChooser(inputDir); //ひらけ
       fc.setFileFilter(
         new FileNameExtensionFilter("Audio Files(wav, mp3)", 
            "wav","mp3")
       );
       fc.setFileSelectionMode(JFileChooser.FILES_ONLY); 
       if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
         File retval = fc.getSelectedFile();
         if (retval.isFile()) {
           inputFile = fc.getSelectedFile();
           selectFile.setText(inputFile.getName());
           inputDir = inputFile.getParentFile();
         } else inputFile = null;
       }
      return;
    }

    if (obj == playButton){

       logText.append("playButton: " + playButton.getText() + "\n");
       if (playButton.getText().equals("Play")) {
          startPlay(inputFile, outputPort, lastSec);
            //何秒目から再生か指定できる
          playButton.setText("Pause");
       } else if (playButton.getText().equals("Pause")) {
          if (player !=null) lastSec = player.stopPlay();
          player = null;
          playButton.setText("Resume");
       } else if (playButton.getText().equals("Resume")) {
          startPlay(inputFile, outputPort, lastSec);
          playButton.setText("Pause");
       }
      return;
    }

    if (obj == saveButton){
       startSave(inputFile);
    }

    if (obj == stopButton){
       logText.append("stopButton\n");
       if (stopButton.getText().equals("Stop")) {
          if (player !=null) {player.stopPlay(); lastSec = 0f;}
          player = null;
          playButton.setText("Play");
       }
      return;
    }

   if (obj == logButton){
    if (logFrame != null && 
        logButton.getText().equals("Close log window")){
        logFrame.dispose(); logFrame = null;
        logButton.setText("Open log window");
        return;
    }

    if (logButton.getText().equals("Open log window"))
      logButton.setText("Close log window");

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    logFrame = new JFrame();
    logFrame.setTitle("Message and Log");
    logFrame.setSize((d.width*2)/5, (d.width*2)/5);
    // logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JScrollPane scrollPane = new JScrollPane(logText);
    logFrame.add(scrollPane);
    logFrame.setVisible(true);
    return;
   }

   if (obj == setPortCombo){
     outputPort = mixers[setPortCombo.getSelectedIndex()];
     logText.append("output port: " + outputPort.getName() + "\n");
     return;
   } 

  } 
  @Override 
  public synchronized void stateChanged(ChangeEvent e){ //スライダー
    Object obj = e.getSource();

    if (obj == tempoSlider){
      int value = tempoSlider.getValue(); // 値を読み取るのがgetValue();
      stretch.setText("Play Speed: " + 
      String.format("%3d", value) + "%"); //表示する
      if(player != null) player.setTempo(value); //実際にそのテンポに変更
      return;
    }

    if (obj == semitoneSlider || obj == centSlider){
      float value = semitoneSlider.getValue()
                   + 0.01f*centSlider.getValue(); 
      pitch.setText("PitchShift: " + 
      String.format("%3.2f", value)); 
      if(player != null) player.setPitchShift(value); 
      return;
    }

    if (obj == volumeSlider){
      int value = volumeSlider.getValue(); //スライダーは整数のみ,少数なし
      volume.setText("Volume: " + 
      String.format("%3d", value) );
      if(player != null) player.setGain(value);
      return;
    }

    if (obj == timeSlider){
      int value = timeSlider.getValue();
      time.setText(String.format("Time: %03d / %06.2f", value, totalSec));
      if(!playButton.getText().equals("Pause")) lastSec = (float) value; 
          // set time if player is not running
      return;
    }

  }

  private void startSave(File inputFile) {
    try {
     if (player != null) player = null;
     player = new TimePitchPlayer(inputFile, null); // file output
     totalSec = player.getTotalTime();
     player.setGain(volumeSlider.getValue());
     player.setSkip(0f);
     timeSlider.setMinimum(0);
     timeSlider.setMaximum((int) totalSec);
     timeSlider.setLabelTable(timeSlider.createStandardLabels(30));
     timeSlider.setPaintLabels(true);
     timeSlider.setValue(0); 
     player.setTimeSlider(timeSlider);
     player.setTempo(tempoSlider.getValue());
      System.out.println("tempo " + tempoSlider.getValue());
     float value = semitoneSlider.getValue() + 0.01f*centSlider.getValue(); 
     pitch.setText("PitchShift: " + String.format("%3.2f", value)); 
     player.setPitchShift(value);
      System.out.println("pitchShift " + value);
     Thread pt = new Thread(player);
     pt.start();
   } catch (Exception e){
     e.printStackTrace();
   }
  }

  private void startPlay(File inputFile, Mixer.Info outputPort, float last){

        try {
          logText.append("Input: " + inputFile.getName() + "\n");
          logText.append("Output: " + outputPort.getName() + "\n");
          player = new TimePitchPlayer(inputFile, outputPort); 
        // TimeStrechの計算はまた別のところにある
          totalSec = player.getTotalTime();
          player.setGain(volumeSlider.getValue());
          player.setSkip(last);
          logText.append("Length: " + totalSec + " sec\n");
          timeSlider.setMinimum(0);
          timeSlider.setMaximum((int) totalSec);
          timeSlider.setLabelTable(timeSlider.createStandardLabels(30));
          timeSlider.setPaintLabels(true);
          timeSlider.setValue((int) last); 
          player.setTimeSlider(timeSlider);
          player.setTempo(tempoSlider.getValue());
          System.out.println("tempo " + tempoSlider.getValue());
          float value = semitoneSlider.getValue()
                   + 0.01f*centSlider.getValue(); 
          pitch.setText("PitchShift: " + String.format("%3.2f", value)); 
          player.setPitchShift(value);
          System.out.println("pitchShift " + value);
          Thread pt = new Thread(player);
          pt.start();
          logText.append("Player thread start\n");
        } catch (Exception e){
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          pw.flush();
          logText.append(sw.toString());
          e.printStackTrace();
        }
  }
  
  public static void main(String[] args) throws Exception {
     if (args.length >= 1) {
       System.out.println("CUI");
     } else new TimePitchPlayerApp();
  }

}

class TimePitchPlayer implements Runnable {

  private AudioInputStream ais;
  private SourceDataLine sline;
  private boolean running;
  private int channels;
  private int frameSize;
  private float frameRate,sampleRate;
  private float gain, tempo, pitchShift,
          currentTime = 0f, lastTime = 0f, totalTime;
  private JSlider timeSlider;
  private boolean bigEndian;
  private long currentFrame = 0, skipTo = 0;
  private File tmpFile = null, inputFile;
  private TimeStretch timeStretch = null;
  private WaveWriter waveWriter = null;
  private AudioFormat format; 
  private Resampler resamplerL, resamplerR; 

  TimePitchPlayer(File inputFile, Mixer.Info outputPort) throws Exception {

    if (inputFile.getName().toUpperCase().endsWith(".MP3")){
      String out = inputFile.getName() + ".wav";
      tmpFile = new File(out);
        // if out does not exist 
      Util.mp3ToWaveFile(inputFile.getPath(),out); 
      setOutput(tmpFile,outputPort);
    } else setOutput(inputFile, outputPort);

    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
    tempo = 1.0f;
  }

  private void setOutput(File inputFile, Mixer.Info mixer) throws Exception {
    this.inputFile = inputFile;
    ais = AudioSystem.getAudioInputStream(inputFile);

    format = ais.getFormat();
    frameSize = format.getFrameSize();
    frameRate = format.getFrameRate();
    sampleRate = format.getSampleRate();
    bigEndian  =  format.isBigEndian();
    channels   = format.getChannels();


    if (mixer != null) {
      waveWriter = null;
      sline = AudioSystem.getSourceDataLine(format,mixer); 
      sline.open(format);
    }
  }

  public synchronized void setTempo(int intTempo){ // 100 scale
    tempo = intTempo/100f;
     if (resamplerL == null) pitchShift = 1;
    if (timeStretch != null) timeStretch.setTempo(tempo/pitchShift);
  }

  public synchronized void setPitchShift(float pitch){ 
    pitchShift = (float) Math.pow(2.0, (double) (pitch/12));
    if (timeStretch != null) timeStretch.setTempo(tempo/pitchShift);
  }

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
    int audioInputSize = 4096*4; // 16bit LR
    byte[] buf = new byte[audioInputSize];
    currentFrame = skipTo/frameSize;
    running = true;
    resamplerL = new Resampler(true,0.1,4.0);
    resamplerR = new Resampler(true,0.1,4.0);
    timeStretch = new TimeStretch(channels,sampleRate);
    timeStretch.setTempo(tempo/pitchShift);

    try {
       if (sline == null){
          waveWriter = new WaveWriter(format, inputFile, 
          String.valueOf((int) (100*tempo)));
       }

       ais.skip(skipTo);
       if (sline != null) sline.start();
       int nread;
       float[] floatSamples, out = null;
       byte[] byteSamples;

       while((nread = ais.read(buf)) > 0 && running) {
          currentFrame += nread/frameSize; 
          floatSamples = Util.LE16ToFloat(buf, nread); 
          showTime();

          timeStretch.putSamples(floatSamples); 
          out = timeStretch.getSamples();

          if (out.length > 0) {
            float[] left  = Util.getLeft(out);
            float[] right = Util.getRight(out);
            float[] outL  = new float[(int) (left.length/pitchShift)];
            float[] outR  = new float[(int) (right.length/pitchShift)];
            resamplerL.process(1/pitchShift, left,0,left.length, false,
                        outL,0,outL.length);
            resamplerR.process(1/pitchShift, right,0,right.length, false, 
                        outR,0,outR.length);

            out = Util.merge(outL, outR);  
            Util.adjustFloatGain(out,gain);
            byteSamples = Util.FloatToLE16(out);
            if (sline != null)
             sline.write(byteSamples,0,byteSamples.length);
            else waveWriter.rawWrite(byteSamples);
          }

       } // end while

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (sline != null) { sline.drain(); sline.stop(); sline.close();}
      else waveWriter.waveWrite(); 
    }
    System.out.println("Player thread end");
  }

  public static void main(String[] args) throws Exception {
    Locale.setDefault(Locale.US);
    File inputFile = new File(args[0]);
    TimePitchPlayer player = new TimePitchPlayer(inputFile,null);
    player.setTempo(Integer.parseInt(args[1]));
    Thread pt = new Thread(player);
    System.out.println("total time: " + player.getTotalTime());
    pt.start();
    pt.join();
    System.out.println("main end");
  }

}
