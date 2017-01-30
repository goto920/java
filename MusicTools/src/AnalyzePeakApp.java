/* 
Libraries used  FFT, MP3 
*/
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jp.kmgoto.music.*;

public class AnalyzePeakApp extends JFrame 
  implements ActionListener, ChangeListener {
  private int fontSize;
  private int gain;
  private JButton selectDir, selectFile, playButton, stopButton, 
          saveButton, logButton;
  private File inputDir = null;
  private File inputFile = null;
  private JLabel mix, time, volume, freq, pan;
  private JSlider timeSlider, volumeSlider;
  private JTextArea logText;
  private JFrame logFrame = null;
  private AnalyzePeakPlayer player = null;
  private float totalSec;
  private float lastSec;
  private JComboBox<String> setPortCombo;
  private Mixer.Info[] mixers; 
  private Mixer.Info outputPort = null;
  private PeakStat stat = null;
  private static final double maxFIndex = Math.log(20000/440.0)/Math.log(2.0);
  private static final double minFIndex = Math.log(11/440.0)/Math.log(2.0);


  AnalyzePeakApp() throws Exception {

    UIManager.put("FileChooser.readOnly", Boolean.TRUE);
    setTitle("Filtered Player");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    stat = new PeakStat(44100,4096,20*2+1,20*2 +1);

    // get screen size and estimate an appropreate font size
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((d.width*2)/5, (d.width*2)/5);

    /* Prepare log window */
    logText = new JTextArea("");
    logText.setEditable(false);
    fontSize = d.width/60;
    logText.setFont(new Font("Serif", Font.PLAIN,fontSize));
    logText.append("FontSize: " + fontSize + "\n"); // write to the log

    JPanel panel = new JPanel(); 
    panel.setLayout(new GridLayout(2,2)); // main panel 2 x 2 grid

// Just a label (left)
    JLabel portLabel = new JLabel("Set output port");
    portLabel.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel.add(portLabel);

// output port selector (right)
    GetPortsInfo info = new GetPortsInfo();
    mixers = info.getOutputPorts(); // get OutputPort info
    String[] items = new String[mixers.length]; 
        // string array for mixer(output port) names 
    for (int i=0; i < mixers.length; i++) items[i] = mixers[i].getName();
       // put mixer names in the array
    setPortCombo = new JComboBox<String>(items);  
       // create a JComboBox for output port selection
    for (int i=0; i < mixers.length; i++) {
      if (items[i].matches("(?i:.*default.*)")){
        setPortCombo.setSelectedIndex(i);
        break;
      }
    } // set default

    outputPort  = mixers[setPortCombo.getSelectedIndex()];
        // set default
    setPortCombo.setFont(new Font("Serif", Font.PLAIN,fontSize));
    setPortCombo.addActionListener(this);
    panel.add(setPortCombo);

// left in 2nd line
    JLabel fileLabel = new JLabel("Select Input File");
    fileLabel.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel.add(fileLabel);

// right in 2nd line
    selectFile = new JButton("File");
    selectFile.setFont(new Font("Serif", Font.PLAIN,fontSize));
    selectFile.addActionListener(this);
    panel.add(selectFile);

// 2nd panel
    JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayout(4,1)); 

//  Label (1st) and Time slider (2nd) 
    time = new JLabel("Time: (unknown) ");
    time.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(time);
    timeSlider = new JSlider();
    timeSlider.setValue(0);
    timeSlider.addChangeListener(this);
    panel2.add(timeSlider);

// Label (3rd) and volume Slider (4th)
    volume = new JLabel("Volume: 70");
    volume.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(volume);
    volumeSlider = new JSlider(0,100); volumeSlider.setValue(70); 
    volumeSlider.setLabelTable(volumeSlider.createStandardLabels(10));
    volumeSlider.setPaintLabels(true);
    volumeSlider.addChangeListener(this);
    panel2.add(volumeSlider);

// 3rd panel
    JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayout(2,2)); 

// Play/Pause/Resume button (left)
    playButton = new JButton("Play");
    playButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    playButton.addActionListener(this);
    panel3.add(playButton);
// Stop   (right)
    stopButton = new JButton("Stop");
    stopButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    stopButton.addActionListener(this);
    panel3.add(stopButton);
// Save (left)
    saveButton = new JButton("Save to file");
    saveButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    saveButton.addActionListener(this);
    panel3.add(saveButton);
// open log window (right)
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

  @Override // process all action events
  public synchronized void actionPerformed(ActionEvent event) {
    Object obj = event.getSource();

    if (obj == selectFile){ // select file 
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
          if (player !=null) lastSec = player.stopPlay();
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
   
   if (obj == saveButton){
      startSave(inputFile); // save in current directory
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

   if (obj == setPortCombo){ // port selector
     outputPort = mixers[setPortCombo.getSelectedIndex()];
     logText.append("output port: " + outputPort.getName() + "\n");
     return;
   } 

  } 

  @Override // process all JSlider events 
  public synchronized void stateChanged(ChangeEvent e){
    Object obj = e.getSource();
    
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

  private void startSave(File inputFile){
  }

  private void startPlay(File inputFile, Mixer.Info outputPort, float last){
    // start play from last(sec)

        try {
          logText.append("Input: " + inputFile.getName() + "\n");
          logText.append("Output: " + outputPort.getName() + "\n");
          player = new AnalyzePeakPlayer(inputFile, outputPort, stat);
             // create a player(runnable)
          totalSec = player.getTotalTime(); // get total time(sec)
          player.setGain(volumeSlider.getValue()); 
             // set gain from the slider value (0--100) 
          player.setSkip(last); // set last (sec) in player

          logText.append("Length: " + totalSec + " sec\n");

          // set the timeSlider 
          timeSlider.setMinimum(0);
          timeSlider.setMaximum((int) totalSec);
          timeSlider.setLabelTable(timeSlider.createStandardLabels(30));
          timeSlider.setPaintLabels(true);
          timeSlider.setValue((int) last); 

          player.setTimeSlider(timeSlider); 
            // pass the slider to player to be controlled
          Thread pt = new Thread(player); // create thread and run the player
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
     new AnalyzePeakApp();
  }

}

// Player for Analysis only
class AnalyzePeakPlayer implements Runnable {

  private AudioInputStream ais;
  private SourceDataLine sline;
  private boolean running;
  private int channels;
  private int frameSize;
  private float frameRate,sampleRate;
  private float gain, tempo, currentTime = 0f, lastTime = 0f, totalTime;
  private JSlider timeSlider;
  private boolean bigEndian;
  private long currentFrame = 0, skipTo = 0;
  private AudioFormat format;
  private PeakStat stat = null;
  private WaveWriter waveWriter = null;
  private File inputFile;

  // Constructor for play/save
  AnalyzePeakPlayer(File inputFile, 
       Mixer.Info outputPort, PeakStat stat) throws Exception {

    setOutput(checkMP3(inputFile), outputPort);
    this.stat = stat;
    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
  }

  // convert mp3 to wav if necessary
  private File checkMP3 (File inputFile) throws Exception {
    File tmpFile = inputFile;
    if (inputFile.getName().toUpperCase().endsWith(".MP3")){
      String out = inputFile.getName() + ".wav";
      tmpFile = new File(out);
      Util.mp3ToWaveFile(inputFile.getPath(),out); 
       // mp3 to wav converter
    } 
    return tmpFile;
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

    if (mixer == null){
      sline = null;
    } else {
      sline = AudioSystem.getSourceDataLine(format,mixer); 
      sline.open(format, 4096*4*20);
    }
  }

  public void setGain(int intGain){ // 100 scale
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
    if ((currentTime >= lastTime + 1f) && (timeSlider != null)) {
        timeSlider.setValue((int) currentTime);
        lastTime = currentTime;
    }
  }

  public float getTotalTime(){
    long len = ais.getFrameLength();
    // AudioFormat format = ais.getFormat();
    totalTime = len/format.getFrameRate();
    return totalTime;
  }

  public void run() {

    byte[] buf = new byte[1024*4]; // 1024 16bit LR samples

    running = true;
    java.util.List<Byte> buffer = null; // output buffer (1 sec or more) 

    FFTforward fftForward = new FFTforward(channels,sampleRate,4096); 
     // FFT forward calculator
    IFFToverlapAdd iFFToverlapAdd 
        = new IFFToverlapAdd(channels,sampleRate,4096);
     // FFT inverse and overlap add calculator
    PanCalculator panCalculator = new PanCalculator(channels,4096);
    PercCalculator percCalculator = new PercCalculator(channels,4096,17);

    currentFrame = skipTo/frameSize;

    try {
       if (sline == null){
          waveWriter = new WaveWriter(format, inputFile,"filtered"); 
       }

       ais.skip(skipTo);

       if (sline != null) sline.start();
       buffer = new ArrayList<Byte>();

       float[] floatSamples, out = null;
       byte[] byteSamples;
       FFTChunk chunk;

// Main Loop
       int nread, nwritten;
       while((nread = ais.read(buf)) > 0 && running) {

         floatSamples = Util.LE16ToFloat(buf, nread); 

         fftForward.putSamples(floatSamples); // put float samples to FFT 
         chunk  = fftForward.getFFTChunk(); // get FFT result(shifted)

         panCalculator.calcPan(chunk); // add pan info
         percCalculator.putFFTChunk(chunk); // add perc info
         chunk = percCalculator.getFFTChunk(); // get the chunk
         stat.process(chunk); // apply filter

         iFFToverlapAdd.putFFTChunk(chunk); // inverse fft
         out = iFFToverlapAdd.getSamples(); // get samples if any 

         if (out.length == 0) continue; // skip the rest if no output

         Util.adjustFloatGain(out,gain); 
         byteSamples = Util.FloatToLE16(out); // Float to bytes

         for (int i = 0; i < byteSamples.length; i++) 
             buffer.add(byteSamples[i]); // store in byte list

         if (buffer.size() < 44100*4) continue; 
             // if less than 1 sec samples

          // copy byte from buffer byte list 
          byte[] outbytes =  new byte[44100*4];
          for (int i=0; i < 44100*4; i++) 
              outbytes[i] = buffer.get(i).byteValue();
          buffer.subList(0, 44100*4).clear(); // clear copied samples

          // write to file or write to audio output line
          if (sline == null){ 
              waveWriter.rawWrite(outbytes); 
              nwritten = 44100*4;
          } else 
              nwritten = 44100*4;  
              // sline.write(outbytes,0,44100*4);

          currentFrame += nwritten/frameSize; 
          showTime();

       } // end while

       // now process the rest in buffer in FFT forward
       fftForward.flush();
       int added = fftForward.getAdded(); 
               // # of added samples in float (L R is separately counted) 
       chunk = fftForward.getFFTChunk(); // get the last chunk 
       panCalculator.calcPan(chunk);
       percCalculator.putFFTChunk(chunk);
       percCalculator.flush(); // all chunks processed now

       while ((chunk = percCalculator.getFFTChunk()) != null){
         stat.process(chunk);
         iFFToverlapAdd.putFFTChunk(chunk);
       }
       iFFToverlapAdd.flush();
       out = iFFToverlapAdd.getSamples();

       if (out.length > 0){
          Util.adjustFloatGain(out,gain);
          byteSamples = Util.FloatToLE16(out);
          for (int i = 0; i < byteSamples.length - 2*added; i++)
                      buffer.add(byteSamples[i]);
          // added samples removed (added is in float)
       }

       // copy samples in buffer (list) to byte array  
       byte[] outbytes =  new byte[buffer.size()];
       for (int i=0; i < buffer.size(); i++) 
          outbytes[i] = buffer.get(i).byteValue();
       buffer.clear();

       // write to file or audio output
       if (sline == null) {
           waveWriter.rawWrite(outbytes);
           nwritten = outbytes.length;
       } else {
           nwritten = outbytes.length; 
          // sline.write(outbytes,0,outbytes.length);
       }
       System.out.println("Last output(bytes): " + outbytes.length);

       currentFrame += nwritten/frameSize; 
       showTime();

   // write to file
      if (sline == null) 
        waveWriter.waveWrite();

      stat.dumpPeak("TPH-Peak.plot");

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (sline != null){
       sline.drain(); sline.stop(); sline.close();
       sline = null;
      } 
     fftForward = null;
     iFFToverlapAdd = null; 
     panCalculator = null;
     percCalculator = null;
    }
    System.out.println("Player thread end");
  }
  
}
