import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import jp.kmgoto.music.*;

public class FeedbackBoosterApp extends JFrame 
  implements ActionListener, ChangeListener {
  private int fontSize;
  private JButton processButton, eQButton;
  private JLabel eQGain, octMix, Q, compGain, 
          compThresh, compRatio, volume;
  public JLabel peakHz;
  private JSlider eQSlider, eQGainSlider, octSlider, QSlider, 
          compGainSlider, ratioSlider, volumeSlider;
  public JSlider threshSlider;
  private FeedbackBoosterPlayer player;
  private float totalSec;
  private float lastSec;
  private JComboBox<String> setInputPortCombo, setOutputPortCombo;
  private Mixer.Info outputPort, inputPort;
  private Mixer.Info[] inputMixers, outputMixers; 
  private boolean windows; 

//  private static final double maxFIndex = Math.log(20000/440.0)/Math.log(2.0);
//  private static final double minFIndex = Math.log(11/440.0)/Math.log(2.0);
  public JLabel clip, comp;


  FeedbackBoosterApp() throws Exception {
    if (System.getProperty("os.name").indexOf("Windows")>=0)
      windows = true; else windows = false;

    inputPort = outputPort = null; player = null;

    setTitle("Guitar Feedback Booster");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((d.width*2)/5, (d.width*2)/5);

  
    fontSize = d.width/60;

    JPanel panel = new JPanel(); 
    panel.setLayout(new GridLayout(2,2));

// get audio port info
    GetPortsInfo info = new GetPortsInfo();
    inputMixers = info.getInputPorts();
    outputMixers = info.getOutputPorts();
    String[] items;

// A) input port selector
    items  = new String[inputMixers.length + 1];
    items[0] = "1) Select input port";
    for (int i=0; i < inputMixers.length; i++){ 
          items[i+1] = inputMixers[i].getName();
     if (windows)
       items[i+1] = new String(items[i+1].getBytes(),"UTF-8");
    }
    setInputPortCombo = new JComboBox<String>(items);  
    setInputPortCombo.setFont(new Font("Serif", Font.PLAIN,fontSize));
    setInputPortCombo.addActionListener(this);
    panel.add(setInputPortCombo);

// B) output port selector
    items = new String[outputMixers.length + 1];
    items[0] = "2) Select output port";
    for (int i=0; i < outputMixers.length; i++){ 
       items[i+1] = outputMixers[i].getName();
     if (windows)
       items[i+1] = new String(items[i+1].getBytes("Windows-932"),"UTF-8");
    }
    setOutputPortCombo = new JComboBox<String>(items);  
    setOutputPortCombo.setFont(new Font("Serif", Font.PLAIN,fontSize));
    setOutputPortCombo.addActionListener(this);
    panel.add(setOutputPortCombo);

// I) process/bypass button

    processButton = new JButton("bypass");
    processButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    processButton.addActionListener(this);
    panel.add(processButton);

// C) Auto/ManualEQ button

    eQButton = new JButton("manualEQ");
    eQButton.setFont(new Font("Serif", Font.PLAIN,fontSize));
    eQButton.addActionListener(this);
    panel.add(eQButton);

// panel 2
    JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayout(9,2));

// D) peak Hz
    peakHz = new JLabel("peakHz: 220");
    peakHz.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(peakHz);
    int max = (int) (12 * Math.log(11025/41.2)/Math.log(2));
    eQSlider = new JSlider(0,max); eQSlider.setValue(
       (int) (12 * Math.log(220/41.2)/Math.log(2)));
    eQSlider.setLabelTable(eQSlider.createStandardLabels(12));
    eQSlider.setPaintLabels(true);
    eQSlider.addChangeListener(this);
    panel2.add(eQSlider);

    eQGain = new JLabel("eQGain(dB): 20"); 
    eQGain.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(eQGain);
    eQGainSlider = new JSlider(0,60); eQGainSlider.setValue(20); 
    eQGainSlider.setLabelTable(eQGainSlider.createStandardLabels(10));
    eQGainSlider.setPaintLabels(true);
    eQGainSlider.addChangeListener(this);
    panel2.add(eQGainSlider);

// D2) octave mix
    octMix = new JLabel("OctMix: 0");
    octMix.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(octMix);
    octSlider = new JSlider(-100,100); octSlider.setValue(0); 
    octSlider.setLabelTable(octSlider.createStandardLabels(50));
    octSlider.setPaintLabels(true);
    octSlider.addChangeListener(this);
    panel2.add(octSlider);

// E) Q
    Q = new JLabel("Q: 2.9");
    Q.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(Q);
    QSlider = new JSlider(1,80); QSlider.setValue(29); 
    QSlider.setPaintLabels(true);
    QSlider.setLabelTable(QSlider.createStandardLabels(20));
    QSlider.addChangeListener(this);
    panel2.add(QSlider);

// comp gain
    comp = new JLabel("COMP");
    // comp.setForeground(Color.GREEN); 
    panel2.add(comp);
    clip = new JLabel("CLIP(-3dB)"); 
    // clip.setForeground(Color.RED); 
    panel2.add(clip);
    compGain = new JLabel("CompGain(dB): -10");
    compGain.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(compGain);
    compGainSlider = new JSlider(-20,20); compGainSlider.setValue(-10); 
    compGainSlider.setLabelTable(compGainSlider.createStandardLabels(10));
    compGainSlider.setPaintLabels(true);
    compGainSlider.addChangeListener(this);
    panel2.add(compGainSlider);

// F) comp ratio
    compRatio = new JLabel("CompRatio: 1.0");
    compRatio.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(compRatio);
    ratioSlider = new JSlider(10,100); ratioSlider.setValue(10); 
    ratioSlider.setLabelTable(ratioSlider.createStandardLabels(20));
    ratioSlider.setPaintLabels(true);
    ratioSlider.addChangeListener(this);
    panel2.add(ratioSlider);

// G) comp thresh
    compThresh = new JLabel("CompThresh(dB): 0");
    compThresh.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(compThresh);
    threshSlider = new JSlider(-60,0); threshSlider.setValue(0); 
    threshSlider.setLabelTable(threshSlider.createStandardLabels(10));
    threshSlider.setPaintLabels(true);
    threshSlider.addChangeListener(this);
    panel2.add(threshSlider);

// H) Volume
    volume = new JLabel("Volume: 70/100");
    volume.setFont(new Font("Serif", Font.PLAIN,fontSize));
    panel2.add(volume);
    volumeSlider = new JSlider(0,150); volumeSlider.setValue(70); 
    volumeSlider.setLabelTable(volumeSlider.createStandardLabels(20));
    volumeSlider.setPaintLabels(true);
    volumeSlider.addChangeListener(this);
    panel2.add(volumeSlider);

/*
    JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayout(2,2));
*/

    add(panel, BorderLayout.NORTH);
    add(panel2, BorderLayout.CENTER);

//    add(panel3, BorderLayout.SOUTH);
    pack();
    setVisible(true);  
  }

  @Override
  public synchronized void actionPerformed(ActionEvent event) {
    Object obj = event.getSource();

   if (obj == processButton){
      if (processButton.getText().equals("process")){
        processButton.setText("bypass");
        if(player != null) player.setBypass(true);
      } else {
        processButton.setText("process");
        if(player != null) player.setBypass(false);
      }

      return;
   }
   
   if (obj == eQButton){
      if (eQButton.getText().equals("manualEQ")){
         if(player != null) player.setAutoEQ(true);
         eQButton.setText("autoEQ");
      } else {
         if(player != null) player.setAutoEQ(false);
         eQButton.setText("manualEQ");
      }
      return;
   }

   if (obj == setInputPortCombo){
     if (setInputPortCombo.getSelectedIndex() == 0) return; // select 

     inputPort = inputMixers[setInputPortCombo.getSelectedIndex() - 1];
     System.out.println("input port: " + inputPort.getName());
     return;
   } 

   if (obj == setOutputPortCombo){
     if (setOutputPortCombo.getSelectedIndex() == 0) return; // select

     outputPort = outputMixers[setOutputPortCombo.getSelectedIndex() -1];
     System.out.println("output port: " + outputPort.getName());
     if (player != null) player.stopPlay();

     if (inputPort != null && outputPort != null){
        System.out.println("Starting player thread");
        player = new FeedbackBoosterPlayer(inputPort, outputPort);
        player.setParent(this);
        Thread pt = new Thread(player); 
        pt.start();
     }

     return;
   } 

  } 
  @Override 
  public synchronized void stateChanged(ChangeEvent e){
    Object obj = e.getSource();

    if (obj == eQSlider){
      int value = eQSlider.getValue();
      float Hz = (float) (41.2 * Math.pow(2, value/12.0));
      if (player != null) player.setPeakHz(Hz);
      peakHz.setText("peakHz: " + String.format("%10.2f",Hz));
      return;
    }

    if (obj == compGainSlider){
      int value = compGainSlider.getValue();
      if (player != null) player.setCompGain(value);
      compGain.setText("CompGain(dB): " + value);
      return;
    }

    if (obj == eQGainSlider){
      int value = eQGainSlider.getValue();
      if (player != null) player.setEQGain(value);
      eQGain.setText("EQGain(dB): " + value);
      return;
    }

    if (obj == octSlider){
      int value = octSlider.getValue();
      float mix = value/100f;
      if (player != null) player.setOctMix(mix);
      octMix.setText("octMix: " + mix);
      return;
    }

    if (obj == QSlider){
      int value = QSlider.getValue();
      float q = value/10f;
      if (player != null) player.setQ(q);
      Q.setText("Q: " + q);
      return;
    }

    if (obj == ratioSlider){
      int value = ratioSlider.getValue();
      float ratio = value/10f;
      if (player != null) player.setRatio(ratio);
      compRatio.setText("CompRatio: " + ratio);
      return;
    }

    if (obj == threshSlider){
      int value = threshSlider.getValue();
      if (player != null) player.setThresh(value);
      compThresh.setText("CompThresh(dB): " + value);
      return;
    }
    
    if (obj == volumeSlider){
      int value = volumeSlider.getValue();
      volume.setText("Volume: " + 
      String.format("%03d", value) );
      if(player != null) player.setVolume(value/100f);
      return;
    }

  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) new FeedbackBoosterApp();
    else FeedbackBoosterApp.main(args);
    // System.exit(0);
  }

}

class FeedbackBoosterPlayer implements Runnable {

  private TargetDataLine iline;
  private SourceDataLine sline;
  private boolean running;
  private int channels;
  private int frameSize;
  private float frameRate,sampleRate;
  private float eQdBGain,Q, octMix, volume, peakHz, compGain;
  private JSlider timeSlider;
  private boolean bigEndian;
  private boolean bypass, autoEQ;
  private PitchFinder pitch; 
  private BiquadEQ eqBase, eqOct;
  private Compressor comp;
  private FeedbackBoosterApp parent;
  private int processSize;

  FeedbackBoosterPlayer(Mixer.Info input, Mixer.Info output){

    AudioFormat format = new AudioFormat(
         AudioFormat.Encoding.PCM_SIGNED,
         44100f, 16, 1, 2, 44100f,false);

    processSize = 512; // 512 or 1024

    try {
      iline = AudioSystem.getTargetDataLine(format, input);
      sline = AudioSystem.getSourceDataLine(format,output);
      System.out.println("Input buffer: " + iline.getBufferSize());
      System.out.println("Output buffer: " + sline.getBufferSize());
      iline.open(format, processSize*8);
      // sline.open(format, processSize*4);
      sline.open(format);
      System.out.println("Input buffer: " + iline.getBufferSize());
      // System.out.println("Output buffer: " + sline.getBufferSize());
    } catch (Exception e){ e.printStackTrace(); } 

    setBypass(true); setAutoEQ(false);
    setVolume(0.7f);
    running = false;
    pitch = new PitchFinder(44100f);
    eQdBGain = 20f; compGain = (float) Math.pow(10, (double) -eQdBGain/20.0);
    eqBase = new BiquadEQ(BiquadEQ.Type.PEAK); 
    eqOct  = new BiquadEQ(BiquadEQ.Type.PEAK); 
    setPeakHz(440f); setQ(1f);
    setOctMix(0f);
    setCompGain(0f);
    comp = new Compressor(32767); // 16bit
    setRatio(1f); setThresh(0f); 
    parent = null;
  }

  public void stopPlay(){ running = false;}

  public void setCompGain(float gain){ 
    compGain = (float) Math.pow(10,(double) gain/20.0); 
  }
  public void setBypass(boolean flag){ bypass = flag;}
  public void setAutoEQ(boolean flag){ autoEQ = flag;}

  public void setPeakHz(float peakHz){ 
    this.peakHz = peakHz;
    eqBase.setParams(peakHz/44100f, eQdBGain, Q);
    eqOct.setParams((2*peakHz)/44100f, eQdBGain, Q);  
    if (parent != null){
       parent.peakHz.setText("peakHz: " + String.format("%10.1f",peakHz));
    }
  }
  public void setEQGain(int gain){
    eQdBGain = (float) Math.pow(10,(double) gain/20.0); 
    eqBase.setParams(peakHz/44100f, eQdBGain, Q);
    eqOct.setParams((2*peakHz)/44100f, eQdBGain, Q);  
  }

  public void setOctMix(float mix){octMix = mix;}
      // -1 (root 100%) .. 1(oct up 100%)
  public void setQ(float Q){
    this.Q = Q;
    eqBase.setParams(peakHz/44100f, eQdBGain, Q);
    eqOct.setParams((2*peakHz)/44100f, eQdBGain, Q);  
  }
  public void setParent(FeedbackBoosterApp obj){parent = obj;}
  public void setRatio(float ratio){comp.setRatio(ratio); }
  public void setThresh(float thresh){comp.setThresh(thresh);}
  public void setVolume(float vol){volume = vol;}

  public void run() {
    // byte[] buf = new byte[processSize*4]; 
        // 256, 512, 1024, etc. x 4 (16bit*2ch)
    byte[] buf = new byte[1024*4];
    running = true;

    try {
       iline.start();
       sline.start();

       float[] floatSamples;
       byte[] byteSamples;
       int nread, nwritten;
       float alpha = 0.2f;

       while(running) {
          nread = iline.read(buf,0,iline.available());
          floatSamples = Util.StereoToMono(Util.LE16ToFloat(buf, nread)); 

          if (!bypass){
          // process effect here
            if (autoEQ){
              float tmp = pitch.findPitch(floatSamples);
              if (tmp > 0){
                peakHz = (1-alpha)*peakHz + alpha*tmp; // slow adjustment
                setPeakHz(peakHz);
              }
            }

            float[] base = eqBase.processArray(floatSamples);
            float[] oct  = eqOct.processArray(floatSamples);

            // add base and oct
            for (int i = 0; i < base.length; i++){
               floatSamples[i] = ((1-octMix)*base[i] + (1+octMix)*oct[i])/2f;
            }

            Util.adjustFloatGain(floatSamples,compGain);

            double peakdB = -100;
            for (int i = 0; i < floatSamples.length; i++){
               double current = 20*Math.log10(Math.abs(floatSamples[i])/32767);
               if (current > peakdB) peakdB = current;
            }

            if (parent != null){
               if (peakdB > -3){
                 parent.clip.setForeground(Color.RED);
                 parent.comp.setForeground(Color.GREEN);
               } else if (peakdB > (double) parent.threshSlider.getValue()){
                 parent.clip.setForeground(Color.BLACK);
                 parent.comp.setForeground(Color.GREEN);
               } else {
                 parent.clip.setForeground(Color.BLACK);
                 parent.comp.setForeground(Color.BLACK);
               }
            }

            comp.processArray(floatSamples);

          }

          Util.adjustFloatGain(floatSamples,volume); // output volume
          floatSamples = Util.MonoToStereo(floatSamples);
          byteSamples = Util.FloatToLE16(floatSamples);
          nwritten = sline.write(byteSamples,0,byteSamples.length);
       } // end while

   } catch (Exception e){
     e.printStackTrace();
   } 
   iline.stop(); iline.flush(); iline.close();
   sline.stop(); sline.flush(); sline.close();

   System.out.println("Player thread end");
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) return;
    File inputFile = new File(args[0]);
  }
  
}
