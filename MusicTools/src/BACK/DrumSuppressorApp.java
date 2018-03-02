import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jp.kmgoto.music.*;

public class DrumSuppressorApp extends JFrame 
  implements ActionListener, ChangeListener {
  private int fontSize;
  private int gain;
  private JButton selectDir, selectFile, playButton, stopButton, 
          saveButton, logButton;
  private File inputDir = null;
  private File inputFile = null;
  private JLabel mix, time, volume, freq, pan;
  private JSlider mixSlider, timeSlider, volumeSlider;
  private JSlider lowFSlider, highFSlider, leftPSlider, rightPSlider;
  private  JTextArea logText;
  private JFrame logFrame = null;
  private DrumSuppressPlayer player = null;
  private float totalSec;
  private float lastSec;
  private JComboBox<String> setPortCombo;
  private Mixer.Info[] mixers; 
  private Mixer.Info outputPort = null;
  private static final double maxFIndex = Math.log(20000/440.0)/Math.log(2.0);
  private static final double minFIndex = Math.log(11/440.0)/Math.log(2.0);


  DrumSuppressorApp() throws Exception {
    UIManager.put("FileChooser.readOnly", Boolean.TRUE);
    setTitle("DrumSuppressor");
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
    panel2.setLayout(new GridLayout(12,1));

    freq = new JLabel("Through F range low/high(Hz):  220/ 3400");
    freq.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(freq);
    lowFSlider = new JSlider(0,100); 
    double tmp = Math.log(220/440.0)/Math.log(2.0);
    lowFSlider.setValue((int) (100*(tmp - minFIndex)/(maxFIndex - minFIndex))); 

    panel2.add(lowFSlider); lowFSlider.addChangeListener(this);
    highFSlider = new JSlider(0,100); highFSlider.setValue(0); 
    tmp = Math.log(3400/440.0)/Math.log(2.0);
    highFSlider.setValue(
      (int) (100*(tmp - minFIndex)/(maxFIndex - minFIndex))); 
    panel2.add(highFSlider); highFSlider.addChangeListener(this);

    pan = new JLabel("Through Pan range left/right: -1.0/1.0");
    pan.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(pan);
    leftPSlider = new JSlider(0,100); leftPSlider.setValue(0); 
    panel2.add(leftPSlider); leftPSlider.addChangeListener(this);
    rightPSlider = new JSlider(0,100); rightPSlider.setValue(100); 
    panel2.add(rightPSlider); rightPSlider.addChangeListener(this);

    mix = new JLabel("Harmonic: 100%");
    mix.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(mix);
    mixSlider = new JSlider(0,100);
    mixSlider.setValue(100); 
    mixSlider.setLabelTable(mixSlider.createStandardLabels(20));
    mixSlider.setPaintLabels(true);
    mixSlider.addChangeListener(this);
    panel2.add(mixSlider);

    time = new JLabel("Time: (unknown) ");
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
    
    // pack();
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
      File outputFile = new File(inputFile + "-drumless.wav");
      startSave(inputFile, outputFile);
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
    
    if (obj == lowFSlider || obj == highFSlider){
      int low = lowFSlider.getValue();
      int high = highFSlider.getValue();

      double tmp = (low*maxFIndex + (100-low)*minFIndex)/100.0;
      float lowF = (float) (440.0*Math.pow(2.0,tmp));
      tmp = (high*maxFIndex + (100-high)*minFIndex)/100.0;
      float highF = (float) (440.0*Math.pow(2.0,tmp));

      freq.setText("Through F range low/high(Hz): "
       + String.format("%5.0f/%5.0f", lowF, highF));
      // set
      if(player != null) player.setThroughFreq(lowF, highF);
      return;
    }

    if (obj == leftPSlider || obj == rightPSlider){
      float left = leftPSlider.getValue()/50f - 1f;
      float right = rightPSlider.getValue()/50f - 1f;
      pan.setText("Through Pan range left/right: "
         + String.format("%5.2f/%5.2f",left,right));
      // set
      if(player != null) player.setThroughPan(left,right);
      return;
    }

    if (obj == mixSlider){
      int value = mixSlider.getValue();
      mix.setText("Harmonic: " + 
      String.format("%03d", value) + "%");
      if(player != null) player.setMix(value);
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

  private void startSave(File inputFile, File outputFile){
     try {
        logText.append("Input: " + inputFile.getName() + "\n");
        logText.append("Output: " + outputFile.getName() + "\n");
        player = new DrumSuppressPlayer(inputFile, outputFile);
        totalSec = player.getTotalTime();
        player.setGain(volumeSlider.getValue());
//        player.setSkip(last);
        logText.append("Length: " + totalSec + " sec\n");

        timeSlider.setMinimum(0);
        timeSlider.setMaximum((int) totalSec);
        timeSlider.setLabelTable(timeSlider.createStandardLabels(30));
        timeSlider.setPaintLabels(true);
        // timeSlider.setValue((int) last); 
        player.setTimeSlider(timeSlider);
        Thread pt = new Thread(player);
        pt.start();
        logText.append("Player thread start\n");
      } catch (Exception e){
        e.printStackTrace();
      }
  }

  private void startPlay(File inputFile, Mixer.Info outputPort, float last){

        try {
          logText.append("Input: " + inputFile.getName() + "\n");
          logText.append("Output: " + outputPort.getName() + "\n");
          player = new DrumSuppressPlayer(inputFile, outputPort);
          totalSec = player.getTotalTime();
          player.setGain(volumeSlider.getValue());
          player.setSkip(last);
          player.setThroughFreq(220f,3400f);
          player.setThroughPan(-0.1f, 0.1f);
          player.setMix(mixSlider.getValue());
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
    if (args.length == 0) new DrumSuppressorApp();
    else DrumSuppressPlayer.main(args);
    // System.exit(0);
  }

}

class DrumSuppressPlayer implements Runnable {

  private AudioInputStream ais;
  private File outputFile = null;
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
  private DrumFilter filter;

  DrumSuppressPlayer(File inputFile, File outputFile) throws Exception {
    setOutput(checkMP3(inputFile), outputFile);
    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
  }

  DrumSuppressPlayer(File inputFile, Mixer.Info outputPort) throws Exception {
    setOutput(checkMP3(inputFile), outputPort);
    totalTime = getTotalTime();
    running = false;
    gain = 0.7f;
  }

  private File checkMP3 (File inputFile) throws Exception {
    File tmpFile = inputFile;
    if (inputFile.getName().toUpperCase().endsWith(".MP3")){
      String out = inputFile.getName() + ".wav";
      tmpFile = new File(out);
      Util.mp3ToWaveFile(inputFile.getPath(),out); 
    } 
    return tmpFile;
  }

  private void setOutput(File inputFile, File outputFile) throws Exception {
    this.outputFile = outputFile;
    ais = AudioSystem.getAudioInputStream(inputFile);

    format = ais.getFormat();
    frameSize = format.getFrameSize();
    frameRate = format.getFrameRate();
    sampleRate = format.getSampleRate();
    bigEndian  =  format.isBigEndian();
    channels   = format.getChannels();
    filter = new DrumFilter(sampleRate,4096);
  }

  private void setOutput(File inputFile, Mixer.Info mixer) throws Exception {
    ais = AudioSystem.getAudioInputStream(inputFile);

    format = ais.getFormat();
    frameSize = format.getFrameSize();
    frameRate = format.getFrameRate();
    sampleRate = format.getSampleRate();
    bigEndian  =  format.isBigEndian();
    channels   = format.getChannels();
    filter = new DrumFilter(sampleRate,4096);

    if (mixer == null){
      DataLine.Info info = new DataLine.Info(SourceDataLine.class,format);
      sline = (SourceDataLine) AudioSystem.getLine(info);
    } else
      sline = AudioSystem.getSourceDataLine(format,mixer); 

    sline.open(format);
  }

  public void setMix(int mix){ // 100 scale
    if(filter != null) filter.setMix(mix/100f);
  }

  public void setThroughFreq(float lowF, float highF){
    if(filter != null) filter.setThroughFreq(lowF, highF);
  }

  public void setThroughPan(float leftP, float rightP){
    if(filter != null) filter.setThroughPan(leftP, rightP);
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
    if ((currentTime > lastTime + 1f) && (timeSlider != null)) {
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
    byte[] buf = new byte[1024];
    currentFrame = skipTo/frameSize;
    running = true;
    java.util.List<Byte> buffer = null;
    FFTforward fftForward = new FFTforward(channels,sampleRate,4096);
    IFFToverlapAdd iFFToverlapAdd 
       = new IFFToverlapAdd(channels,sampleRate,4096);
    PanCalculator panCalculator = new PanCalculator(channels,4096);
    PercCalculator percCalculator = new PercCalculator(channels,4096,17);

    try {
       ais.skip(skipTo);
       if (outputFile == null) 
         sline.start();
       else 
         buffer = new ArrayList<Byte>();

       int nread;
       float[] floatSamples, out = null;
       byte[] byteSamples;
       FFTChunk chunk;

       while((nread = ais.read(buf)) > 0 && running) {
          currentFrame += nread/frameSize; 
          floatSamples = Util.LE16ToFloat(buf, nread); 
          showTime();

          fftForward.putSamples(floatSamples);
          chunk  = fftForward.getFFTChunk();

          panCalculator.calcPan(chunk); 
          percCalculator.putFFTChunk(chunk);
          chunk = percCalculator.getFFTChunk();

          // apply FFTchunk filter here
          filter.apply(chunk);

          iFFToverlapAdd.putFFTChunk(chunk);
          out = iFFToverlapAdd.getSamples(); 

          // out = floatSamples; // bypass
          if (out.length > 0){
            Util.adjustFloatGain(out,gain);
            byteSamples = Util.FloatToLE16(out);
            if (outputFile != null){
               for (int i = 0; i < byteSamples.length; i++)
                  buffer.add(byteSamples[i]);
            } else { 
              sline.write(byteSamples,0,byteSamples.length);
            }

          }

       } // end while

/*
       fftForward.flush();
       int added = fftForward.getAdded(); // # of added samples in float
       chunk = fftForward.getFFTChunk(); // last chunk 
       panCalculator.calcPan(chunk);

       percCalculator.putFFTChunk(chunk); // last chunk
       percCalculator.flush();

       while ((chunk = percCalculator.getFFTChunk()) != null){
         // Filter
         filter.apply(chunk);
         iFFToverlapAdd.putFFTChunk(chunk);
       }

       iFFToverlapAdd.flush();
       out = iFFToverlapAdd.getSamples();

       if (out.length > 0){
            Util.adjustFloatGain(out,gain);
            byteSamples = Util.FloatToLE16(out);
            if (outputFile != null){
              for (int i = 0; i < byteSamples.length -2*added; i++)
                    buffer.add(byteSamples[i]);
            } else 
               sline.write(byteSamples,0,byteSamples.length -2*added);
       }
*/

   // write to file
      if (outputFile != null) {
        byte[] outbytes =  new byte[buffer.size()];
        for (int i=0; i < buffer.size(); i++) 
           outbytes[i] = buffer.get(i).byteValue();

        AudioInputStream ois = new AudioInputStream(
             new ByteArrayInputStream(outbytes), format, 
             buffer.size()/format.getFrameSize()
           ); // size()/4 for 16bit stereo
        AudioSystem.write(ois, AudioFileFormat.Type.WAVE, outputFile);
      } 

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (outputFile == null){
       sline.drain(); sline.stop(); sline.close();
      } 
    }
    System.out.println("Player thread end");
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) return;

    File inputFile = new File(args[0]);
    File outputFile = null;
     outputFile = new File(args[0] + "-nodrums.wav");
     DrumSuppressPlayer player = new DrumSuppressPlayer(inputFile,outputFile); 
     player.setGain(100);
     System.out.println("total time: " + player.getTotalTime());
     player.run();
     System.out.println("main end");
     System.exit(0);
  }
  
}

class DrumFilter {
   private float samplingRate;
   private float panL, panR;
   private int lowFIndex, highFIndex, windowSize;
   private float mix; 

   DrumFilter(float samplingRate, int windowSize){
     this.samplingRate = samplingRate;
     this.windowSize = windowSize;
     setMix(1f); // 100% harmonic 
//     setThrough(800f, 3400f, -0.1f, 0.1f);
     setThrough(0f, 0f, 0f, 0f);
   }

   public void setThrough(float lowF, float highF, float panL, float panR){
     this.panL = panL; this.panR  = panR;
     lowFIndex  = (int)  (lowF*windowSize/samplingRate);
     highFIndex  = (int) (highF*windowSize/samplingRate);
   }

   public void setThroughFreq(float lowF, float highF){
     lowFIndex  = (int)  (lowF*windowSize/samplingRate);
     highFIndex  = (int) (highF*windowSize/samplingRate);
   }

   public void setThroughPan(float panL, float panR){
     this.panL = panL; this.panR  = panR;
   }

   public void setMix(float mix){
     this.mix = mix;
   }
  
   public void apply(FFTChunk chunk){
     if (chunk == null) return;

     int channels = chunk.getChannels();
     int windowSize = chunk.getWindowSize();
     float[] left=null, right=null, pL=null, pR=null, pan=null;

     if (channels == 1){
       left = chunk.getFFTCoefL();
       pL = chunk.getPercCoefL();
     } else {
       left = chunk.getFFTCoefL();
       pL = chunk.getPercCoefL();
       right = chunk.getFFTCoefR();
       pR = chunk.getPercCoefR();
       pan = chunk.getPan();
     }

     for (int i = 0; i < windowSize/2 + 1; i++){ // FFTindex

     if (channels == 1){
       if (i >= lowFIndex && i <= highFIndex) return; // no process
     } else {
       if (pan[i] >= panL && pan[i] <= panR
        && i >= lowFIndex && i <= highFIndex) return; // no process
     }

       float mixL=0f, mixR=0f;
       if (mix > 0.5f){ 
         mixL = (1f - pL[i]) + 2f*(1f-mix)*pL[i];
         if (channels == 2) mixR = (1f - pR[i]) + 2f*(1f-mix)*pR[i];
       } else {
         mixL = 2f*mix*(1f - pL[i]) + pL[i];
         if (channels == 2) mixR = 2f*mix*(1f - pR[i]) + pR[i];
       }

       // System.out.println("mix,mixL,mixR = " + mix + ", " + mixL + ", " + mixR);

       left[2*i] *= mixL; left[2*i+1] *= mixL;
       if (channels == 2) right[2*i] *= mixR; right[2*i+1] *= mixR;

       if (i > 0){ // i = 0 is DC
          left[2*(windowSize - i)] *= mixL;
          left[2*(windowSize - i)+1] *= mixL;
        if (channels == 2){
          right[2*(windowSize - i)] *= mixR;
          right[2*(windowSize - i)+1] *= mixR;
        }
       } 

     }

   }

}
