import java.io.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class TimeStretchPlayer extends JFrame 
  implements ActionListener, ChangeListener {
  private int fontSize;
  private int gain;
  private JButton selectDir, selectFile, playButton, stopButton, 
          saveButton, logButton;
  private File inputDir = null;
  private File inputFile = null;
  private JLabel stretch, time, volume;
  private JSlider tempoSlider, timeSlider, volumeSlider;
  private  JTextArea logText;
  private JFrame logFrame = null;
  private PlayWaveFile player = null;
  private float totalSec;
  private float lastSec;
  private JComboBox<String> setPortCombo;
  private Mixer.Info[] mixers; 
  private Mixer.Info outputPort = null;

  TimeStretchPlayer() throws Exception {
    UIManager.put("FileChooser.readOnly", Boolean.TRUE);
    setTitle("TimeStretch Player");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((d.width*2)/5, (d.width*2)/5);

    logText = new JTextArea("");
    logText.setEditable(false);
    fontSize = d.width/60;
    logText.append("FontSize: " + fontSize + "\n");
    logText.setFont(new Font("Serif", Font.PLAIN,fontSize));

    JPanel panel = new JPanel(); 
    panel.setLayout(new GridLayout(2,2));

// Just a label
    JLabel portLabel = new JLabel("Set output port");
    portLabel.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel.add(portLabel);

// output port selector
    GetPortsInfo info = new GetPortsInfo();
    mixers = info.getOutputPorts();
    String[] items = new String[mixers.length];
    for (int i=0; i < mixers.length; i++) items[i] = mixers[i].getName();
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
    panel2.setLayout(new GridLayout(6,1));

    stretch = new JLabel("Time Stretch: 100%");
    stretch.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(stretch);
    tempoSlider = new JSlider(20,250);
    tempoSlider.setValue(100); 
    tempoSlider.setLabelTable(tempoSlider.createStandardLabels(20));
    tempoSlider.setPaintLabels(true);
    tempoSlider.addChangeListener(this);
    panel2.add(tempoSlider);

    time = new JLabel("Time: ");
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
    saveButton = new JButton("Save to wave file");
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
    lastSec = 0.0f;
  }

  @Override
  public synchronized void actionPerformed(ActionEvent event) {
    Object obj = event.getSource();

    if (obj == selectFile){
       logText.append("selectFile\n");
       JFileChooser fc = new JFileChooser(inputDir);
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
  public synchronized void stateChanged(ChangeEvent e){
    Object obj = e.getSource();

    if (obj == tempoSlider){
      int value = tempoSlider.getValue();
      stretch.setText("Time Stretch: " + 
      String.format("%03d", value) + "%");
      return;
    }

    if (obj == volumeSlider){
      int value = volumeSlider.getValue();
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

  private void startPlay(File inputFile, Mixer.Info outputPort, float last){

        try {
          logText.append("Input: " + inputFile.getName() + "\n");
          logText.append("Output: " + outputPort.getName() + "\n");
          player = new PlayWaveFile(inputFile, outputPort);
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
     } else new TimeStretchPlayer();
  }
}
