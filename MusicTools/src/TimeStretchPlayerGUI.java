import java.io.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jp.kmgoto.music.*;

public class TimeStretchPlayerGUI extends JFrame //JFrameを拡張する
  implements ActionListener, ChangeListener {//ChangeListenerはスライダーを使うとき
  private int fontSize; //private 基本的にクラスの中でだけ使える変数の定義
  private int gain;　//他のメソッドの境界を超えて他のメソッドでも同じ変数として使える
  private JButton selectDir, selectFile, playButton, stopButton, 
          saveButton, logButton;
  private File inputDir = null;
  private File inputFile = null;
  private JLabel stretch, time, volume;
  private JSlider tempoSlider, timeSlider, volumeSlider;
  private  JTextArea logText;
  private JFrame logFrame = null;
  private TimeStretchPlayer player = null;
  private float totalSec;
  private float lastSec;
  private JComboBox<String> setPortCombo;
  private Mixer.Info[] mixers; 
  private Mixer.Info outputPort = null;

  TimeStretchPlayerGUI() throws Exception { //throws Exception ここの中で例外処理はしないといういう意味　コンストラクター
    UIManager.put("FileChooser.readOnly", Boolean.TRUE);　// クラス名.putというメソッド　　staticメソッド　
    setTitle("TimeStretch Player");//省略した時は,自分のセットタイトル
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);　//ぺけを押したら終了（おまじない）

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();　
    setSize((d.width*2)/5, (d.width*2)/5);//画面サイズを調べてフォントを変える方法（難しい）

    logText = new JTextArea("");
    logText.setEditable(false);
    fontSize = d.width/60;
    logText.append("FontSize: " + fontSize + "\n");　//記録用の別ウィンドウ　気にしなくて良い
    logText.setFont(new Font("Serif", Font.PLAIN,fontSize));

    JPanel panel = new JPanel(); 
    panel.setLayout(new GridLayout(2,2));

// Just a label
    JLabel portLabel = new JLabel("Set output port");
    portLabel.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel.add(portLabel);

// output port selector　難しい
    GetPortsInfo info = new GetPortsInfo();　
    mixers = info.getOutputPorts();
    String[] items = new String[mixers.length];　//配列名.lengthで配列のサイズがわかる
    for (int i=0; i < mixers.length; i++) items[i] = mixers[i].getName();　//items[i]に文字列が入っている
    setPortCombo = new JComboBox<String>(items);  
    setPortCombo.setSelectedIndex(0);
    outputPort  = mixers[setPortCombo.getSelectedIndex()];
    setPortCombo.setFont(new Font("Serif", Font.PLAIN,fontSize));
    setPortCombo.addActionListener(this);
    panel.add(setPortCombo);

    JLabel fileLabel = new JLabel("Select Input File");
    fileLabel.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel.add(fileLabel);  //字を表示

    selectFile = new JButton("File");
    selectFile.setFont(new Font("Serif", Font.PLAIN,fontSize));
    selectFile.addActionListener(this);
    panel.add(selectFile);

<<<<<<< .mine
//Ｔｉｍｅからしたの6行
    JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayout(6,1));
=======
    JPanel panel2 = new JPanel();　
    panel2.setLayout(new GridLayout(6,1));  
>>>>>>> .r587

    stretch = new JLabel("Play Speed: 100%");
    stretch.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(stretch);
<<<<<<< .mine
    tempoSlider = new JSlider(50,200);
    tempoSlider.setValue(100); //初期値
    tempoSlider.setLabelTable(tempoSlider.createStandardLabels(20));　　　　　　　　　　　　　　　　　　　　　　//）
    tempoSlider.setPaintLabels(true);　　　　　　　　　　　　　　　　　　　　　　　　　　　　　                       //）めもり表示
=======
    tempoSlider = new JSlider(50,200); // 50-200
    tempoSlider.setValue(100); //初期値
    tempoSlider.setLabelTable(tempoSlider.createStandardLabels(20)); //20おきにメモリを入れる
    tempoSlider.setPaintLabels(true);　//これでメモリを表示
>>>>>>> .r587
    tempoSlider.addChangeListener(this);
    panel2.add(tempoSlider);

    time = new JLabel("Time: ");　// 上と同じ
    time.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(time);
    timeSlider = new JSlider();
    timeSlider.addChangeListener(this);
    panel2.add(timeSlider);

    volume = new JLabel("Volume: 070");
    volume.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(volume);
    volumeSlider = new JSlider(0,100); volumeSlider.setValue(70); 
    volumeSlider.setLabelTable(volumeSlider.createStandardLabels(10));
    volumeSlider.setPaintLabels(true);
    volumeSlider.addChangeListener(this);
    panel2.add(volumeSlider);

<<<<<<< .mine
//下の４つ
    JPanel panel3 = new JPanel();
=======
    JPanel panel3 = new JPanel();　
>>>>>>> .r587
    panel3.setLayout(new GridLayout(2,2));
    playButton = new JButton("Play");
    playButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    playButton.addActionListener(this);
    panel3.add(playButton);
    stopButton = new JButton("Stop");
    stopButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    stopButton.addActionListener(this);
    panel3.add(stopButton);
    saveButton = new JButton("Save to file(Not ready)");
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
    lastSec = 0.0f;　//あまり関係ない
  }

<<<<<<< .mine
  @Override
  public synchronized void actionPerformed(ActionEvent event) { //action~ボタンをおしたときの処理
    Object obj = event.getSource();  
                        //だれがおくってきたか  
    if (obj == selectFile){　//objはなんですか。。。if文で
=======
  @Override　
  public synchronized void actionPerformed(ActionEvent event)　{//何がおきたか表示
    Object obj = event.getSource(); //どこからきたか

    if (obj == selectFile){
>>>>>>> .r587
       logText.append("selectFile\n");
<<<<<<< .mine
       JFileChooser fc = new JFileChooser(inputDir);　　　//ひらけ
=======
       JFileChooser fc = new JFileChooser(inputDir);　//ひらけ
>>>>>>> .r587
       fc.setFileFilter(
         new FileNameExtensionFilter("Audio Files(wav, mp3)", 
            "wav","mp3")
       );
       fc.setFileSelectionMode(JFileChooser.FILES_ONLY); 
       if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){ //画面が開く
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
<<<<<<< .mine
          startPlay(inputFile, outputPort, lastSec);　　　　//スタートするときの呼び出し
=======
          startPlay(inputFile, outputPort, lastSec);//何秒目から再生か指定できる　再生させる行
>>>>>>> .r587
          playButton.setText("Pause");
       } else if (playButton.getText().equals("Pause")) {
          if (player !=null) lastSec = player.stopPlay();
          player = null;
          playButton.setText("Resume");
       } else if (playButton.getText().equals("Resume")) {
          startPlay(inputFile, outputPort, lastSec);
　　　　　　　　　　　　　　　　　　　　　　　                    //どこから再生を続けるか
          playButton.setText("Pause");
       }
      return;
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
    // logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

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


//スライダーの処理
  @Override 
  public synchronized void stateChanged(ChangeEvent e){　//スライダー
    Object obj = e.getSource();

    if (obj == tempoSlider){
      int value = tempoSlider.getValue();　//値を読み取るのがgetValue();
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　//値を処理
      stretch.setText("Time Stretch: " + 
<<<<<<< .mine
      String.format("%03d", value) + "%");　　　　　　　　　//c言語のprintf
      if(player != null) player.setTempo(value);
=======
      String.format("%03d", value) + "%");　 //表示する
      if(player != null) player.setTempo(value);　//実際にそのテンポに変更
>>>>>>> .r587
      return;
    }

    if (obj == volumeSlider){
      int value = volumeSlider.getValue();　//スライダーは整数のみ,少数なし
      volume.setText("Volume: " + 
      String.format("%03d", value) );
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


//ここからは難しいから後回しでもいい
  private void startPlay(File inputFile, Mixer.Info outputPort, float last){

        try {
          logText.append("Input: " + inputFile.getName() + "\n");
          logText.append("Output: " + outputPort.getName() + "\n");
          player = new TimeStretchPlayer(inputFile, outputPort); //TimeStretchPlayerは別のところに入っている,TimeStrechの計算はまた別のところにある
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
     } else new TimeStretchPlayerGUI();
  }

}
