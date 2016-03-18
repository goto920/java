/**
 * Written by goto@kmgoto.jp 
 */
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import java.nio.file.Files;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.sound.sampled.*;
import jp.kmgoto.musicplayer.FFTEffector;
import jp.kmgoto.musicplayer.swing.Utils;
import jp.kmgoto.musicplayer.swing.ConverterThread;
import jp.kmgoto.musicplayer.swing.PlayerThread;

class MusicPlayerApp extends JFrame 
  implements ActionListener, ChangeListener {

  private static final long serialVersionUID = 20160316;
  private Locale locale;
  private static String Top_help, Player_help, PanCanceler_help,
     PercussionSplitter_help;

  private JFrame frame; // for pack() 
  private JPanel bassPanel, topPanel, topHelpPanel,
          playerPanel, panCancelerPanel, percussionSplitterPanel;
  // topPanel controls 
  private JComboBox<String> effectCombo; 
  private JButton inputFileButton, closeButton;
  private JLabel inputFileLabel;
//  private String inputFilePath;
  private File inputFile;
  // playerPanel controls
  private JLabel  plTime, plTotalTime;
  private JSlider plTimeSlider;
//  private JButton plPlaySaveButton;
  private JButton plStartStopButton;
  private JSlider plVolumeSlider;
  private PlayerThread pt;

  // panCanceler controls
  private JLabel  pcPan, pcWidth, pcLowF, pcHighF;
  private JSlider pcPanSlider;
  private JSlider pcWidthSlider;
  private JSlider pcLowFSlider;
  private JSlider pcHighFSlider;
  private JButton pcCancelExtractButton;
  private JButton pcStartAbortButton;
  private JProgressBar pcProgressBar;

  // percussionSplitter controls
  private JLabel  psMix, psLowF, psHighF;
  private JSlider psMixSlider;
  private JSlider psLowFSlider;
  private JSlider psHighFSlider;
  private JButton psStartAbortButton;
  private JProgressBar psProgressBar;

  // Converter
  private int samplingRate;
  private boolean conversion;
  private static final double maxFIndex = Math.log(20000/440.0)/Math.log(2.0);
  private static final double minFIndex = Math.log(11/440.0)/Math.log(2.0);
  private FFTEffector effector;
  private ConverterThread ct;


  public MusicPlayerApp() { // constructor

    if (Locale.getDefault() != Locale.JAPAN) {
       Locale.setDefault(Locale.US);
       JFileChooser.setDefaultLocale(Locale.US);
    }

    ResourceBundle bundle = null;
    try {
       bundle = ResourceBundle.getBundle("HELP");
       Top_help = bundle.getString("Top_help");
       Player_help =bundle.getString("Player_help");
       PanCanceler_help =bundle.getString("PanCanceler_help");
       PercussionSplitter_help =bundle.getString("PercussionSplitter_help");
    } catch (Exception e) { e.printStackTrace();}

    frame = new JFrame("Sample Frame");
    frame.setTitle("Music Player");

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    JLabel label;

    bassPanel = new JPanel();
    bassPanel.setLayout(new BoxLayout(bassPanel,BoxLayout.Y_AXIS));
    topPanel = createTopPanel(); 
    bassPanel.add(topPanel);
    bassPanel.setVisible(true);

    topHelpPanel = createTopHelpPanel(); 
    bassPanel.add(topHelpPanel);
    topHelpPanel.setVisible(true);

    playerPanel = createPlayerPanel(); 
    bassPanel.add(playerPanel);
    playerPanel.setVisible(false);

    panCancelerPanel = createPanCancelerPanel(); 
    bassPanel.add(panCancelerPanel);
    panCancelerPanel.setVisible(false);

    percussionSplitterPanel = createPercussionSplitterPanel(); 
    bassPanel.add(percussionSplitterPanel);
    percussionSplitterPanel.setVisible(false);

    frame.add(bassPanel); 
    frame.pack();
    frame.setVisible(true);
    samplingRate = 44100;
    conversion = true;
  }

  public static void main(String[] args){
       new MusicPlayerApp();
  }

  public void actionPerformed(ActionEvent event){
      Object obj = event.getSource();

   // TOP
      if (obj == inputFileButton){
         inputFile = getInputFile();

         if (inputFile == null){
            inputFileLabel.setText("Select input file please!");
            return;
         } 

         String mimeType = null;
         try {
           mimeType = Files.probeContentType(inputFile.toPath());
         } catch (Exception e) {
            e.printStackTrace();
         }
         inputFileLabel.setText( 
               "<HTML>" + inputFile.getParent() 
             + "/<BR>" + inputFile.getName() 
             + "(" + mimeType + ")"
             + "</HTML>");

         effectCombo.setSelectedIndex(0);

       return; 
      } 

      if(obj == effectCombo){
 
        String item = (String) effectCombo.getSelectedItem();
        if (inputFile == null){
           inputFileLabel.setText("Select input file please!");
           effectCombo.setSelectedIndex(0);
           return;
        }

      if (pt != null) pt.stopPlay();
      if (ct != null) ct.stopConv();

      String in = inputFile.getAbsolutePath();
  //    System.out.println("Input: " + in);

      AudioFormat format = Utils.checkFormat(in);

      if (format == null){
        inputFileLabel.setText("<HTML>unsupported file<BR>"
             + in + "</HTML>");
        in = null;
        effectCombo.setSelectedIndex(0);
        return;
      }
 
       samplingRate = (int) format.getSampleRate();

      int length = 0;
      try {
       AudioFileFormat fformat
            = AudioSystem.getAudioFileFormat(inputFile);
        length = fformat.getFrameLength();
      } catch(Exception e) { e.printStackTrace();}

      float totalSec = 
          (2*format.getChannels()*length) /(samplingRate*4f);

      plTotalTime.setText("Total(sec): " + totalSec);

        switch(item){
           case "Select One":
             System.out.println("NO SELECTION");
             break;
           case "Player":
             plTimeSlider.setValue(0);
             topPanel.setVisible(true); 
             topHelpPanel.setVisible(false);
             playerPanel.setVisible(true); 
             panCancelerPanel.setVisible(false);
             percussionSplitterPanel.setVisible(false);
             break;
           case "PanCanceler":
             effector = new FFTEffector(FFTEffector.TYPE_PANCANCEL);
             float pan = 0f;
             effector.setParam(FFTEffector.OP_PAN, pan);
               pcPanSlider.setMinimum(0);
               pcPanSlider.setMaximum(100);
               int sliderVal = (int) (50*pan) + 50;
               pcPanSlider.setValue(sliderVal);

             float width = 0.3f;
             effector.setParam(FFTEffector.OP_WIDTH, width);
               pcWidthSlider.setMinimum(0);
               pcWidthSlider.setMaximum(100);
               sliderVal = (int) (100*width);
               pcWidthSlider.setValue(sliderVal);

             float lowF = 220f;
             double tmp = Math.log(lowF/440.0)/Math.log(2.0);
              sliderVal 
                 = (int) (100*(tmp - minFIndex)/(maxFIndex - minFIndex));
             pcLowFSlider.setMinimum(0);
             pcLowFSlider.setMaximum(100);
             pcLowFSlider.setValue(sliderVal);

             float highF = 4000f;
             effector.setParam(FFTEffector.OP_HIGHF, highF);
               tmp = Math.log(highF/440.0)/Math.log(2.0);
               sliderVal 
                 = (int) (100*(tmp - minFIndex)/(maxFIndex - minFIndex));
               pcHighFSlider.setMinimum(0);
               pcHighFSlider.setMaximum(100);
               pcHighFSlider.setValue(sliderVal);
 
             topPanel.setVisible(true); 
             topHelpPanel.setVisible(false);
             playerPanel.setVisible(false);
             panCancelerPanel.setVisible(true);
             percussionSplitterPanel.setVisible(false);
 
             break;
           case "PercussionSplitter":
             effector = new FFTEffector(FFTEffector.TYPE_PERCSPLIT);

               float mix = 1f;
               effector.setParam(FFTEffector.OP_MIX, mix);
               psMixSlider.setMinimum(0);
               psMixSlider.setMaximum(100);
               sliderVal = (int) (100*mix);
               psMixSlider.setValue(sliderVal);

               lowF = 800f;
               effector.setParam(FFTEffector.OP_LOWF, lowF);
               psLowF.setText(", lowF: "+ (int) lowF);
               psLowFSlider.setMinimum(0);
               psLowFSlider.setMaximum(100);
               tmp = Math.log(lowF/440.0)/Math.log(2.0);
               sliderVal 
                 = (int) (100*(tmp - minFIndex)/(maxFIndex - minFIndex));
               psLowFSlider.setValue(sliderVal);

               highF = 3400f;
               effector.setParam(FFTEffector.OP_HIGHF, highF);
               psHighF.setText(", HighF: "+ (int) highF);
               psHighFSlider.setMinimum(0);
               psHighFSlider.setMaximum(100);
               tmp = Math.log(highF/440.0)/Math.log(2.0);
               sliderVal 
                 = (int) (100*(tmp - minFIndex)/(maxFIndex - minFIndex));
               psHighFSlider.setValue(sliderVal);

             topPanel.setVisible(true); 
             topHelpPanel.setVisible(false);
             playerPanel.setVisible(false);
             panCancelerPanel.setVisible(false);
             percussionSplitterPanel.setVisible(true);

             break;
           default:
             System.out.println("Effect selection " + item);
        }

        frame.pack();
        return;
      } 

      if(obj == closeButton){
         System.out.println("closeButton");
         dispose();
         System.exit(0);
         return;
      } 

 // Player
/*
      if(obj == plPlaySaveButton){
         String text = plPlaySaveButton.getText();
         System.out.println("plPlaySaveButton: " + text);
         if (text.equals("Play")){
            plPlaySaveButton.setText("Save");
         } else if (text.equals("Save")){
            plPlaySaveButton.setText("Play&Save");
         } else if (text.equals("Play&Save")){
            plPlaySaveButton.setText("Play");
         } else {
            System.out.println("plPlaySaveButton: unkown value =" + text);
         }
         return;
      } 
*/

      if(obj == plStartStopButton){
         String  text = plStartStopButton.getText();
         // System.out.println("plStartStopButton: " + text);
         if (text.equals("Start")){
            System.out.println("Starting Player..  " + inputFile.getName());
            pt = new PlayerThread(inputFile.getAbsolutePath(), plTimeSlider);
            pt.start();
            plStartStopButton.setText("Stop");
         } else if (text.equals("Stop")){
            if (pt != null) pt.stopPlay();
            plStartStopButton.setText("Start");
         } else {
            System.out.println(
             "plStartStopButton: unkown value = " + text);
         }
         return;
      } 

 // PanCanceler
      if(obj == pcCancelExtractButton){
         String  text = pcCancelExtractButton.getText();
         // System.out.println("pcCancelExtractButton: pressed");

         if (text.equals("Cancel")){
            pcCancelExtractButton.setText("Extract");
         } else if (text.equals("Extract")){ 
            pcCancelExtractButton.setText("Cancel");
         } else {
            System.out.println(
            "pcCancelExtractButton: unkown value = " + text);
         }
        return;
      }

      if(obj == pcStartAbortButton){
         String  text = pcStartAbortButton.getText();
         if (text.equals("Start")){
           if (pcProgressBar == null) 
             System.out.println("Warn: pcProgressBar is NOT set");

           ct = new ConverterThread(inputFile.getAbsolutePath(),
             inputFile.getAbsolutePath() + "-conv.wav", 
             effector, pcProgressBar);
           ct.start(); 

           pcStartAbortButton.setText("Abort");
         } else if (text.equals("Abort")){ 
           if(ct != null) ct.stopConv(); 
            pcStartAbortButton.setText("Start");
         } else if (text.equals("Play Saved?")){ 
            System.out.println ("Play Saved? pressed"); 
            String newFile = inputFile.getAbsolutePath() + "-conv.wav";
            inputFile = new File(newFile);
            inputFileLabel.setText(
              "<HTML>" + inputFile.getParent() + "/<BR>" 
               + inputFile.getName() + "</HTML>");
            pcStartAbortButton.setText("Start");
            effectCombo.setSelectedIndex(1); // Play
         } else {
            System.out.println(
            "pcStartAbortButton: unkown value = " + text);
         }
         return;
      }

 // PercussionSplitter
     if(obj == psStartAbortButton){
         String text = psStartAbortButton.getText(); 
 //        System.out.println("psStartAbortButton");
         if (text.equals("Start")){
           if (psProgressBar == null) 
              System.out.println("Warn: psProgressBar is NOT set");

           ct = new ConverterThread(
             inputFile.getAbsolutePath(),
             inputFile.getAbsolutePath() + "-conv.wav",
             effector, psProgressBar);
           ct.start(); 

           psStartAbortButton.setText("Abort");
         } else if (text.equals("Abort")){ 
           if (ct!=null) ct.stopConv();
           psStartAbortButton.setText("Start");
         } else if (text.equals("Play Saved?")){ 
            System.out.println ("Play Saved? pressed"); 
            String newFile = inputFile.getAbsolutePath() + "-conv.wav";
            inputFile = new File(newFile);
            inputFileLabel.setText("<HTML>"
             + inputFile.getParent() + "/<BR>" + inputFile.getName()
             + "</HTML>");
            psStartAbortButton.setText("Start");
            effectCombo.setSelectedIndex(1); // Play
         } else {
            System.out.println(
            "psStartAbortButton: unkown value = " + text);
         }
       return;
     } 

      // else
      System.out.println("actionPerformed() from unknown object");
  }


  public synchronized void stateChanged (ChangeEvent event){
    Object obj = event.getSource();
  // Player
    if (obj == plTimeSlider){
        int val = plTimeSlider.getValue();
        plTime.setText(" | Current(sec): " 
        + String.format("%7.3f",(val*1024)/(samplingRate*4f)));
        return;
    }

    if (obj == plVolumeSlider){
         int val = plVolumeSlider.getValue();
         // System.out.println("plVolumeSlider: " + val);
         if (pt != null) pt.setVolume(val); // max 150
        return;
    }

  // PanCanceler 
    if (obj == pcPanSlider){
         int val = pcPanSlider.getValue();
         // System.out.println("pcPanSlider: " + val);
         pcPan.setText("Pan: " + (val-50)/50f);
       return;
    }

    if (obj == pcWidthSlider){
         int val = pcWidthSlider.getValue();
         // System.out.println("pcWidthSlider: " + val);
         pcWidth.setText(", Width: " + val/100f);
       return;
    }

    if (obj == pcLowFSlider){
         int val = pcLowFSlider.getValue();
         // System.out.println("pcLowFSlider: " + val);
         double tmp = (val*maxFIndex + (100-val)*minFIndex)/100.0;
         float lowF = (float) (440.0*Math.pow(2.0,tmp));
         effector.setParam(FFTEffector.OP_LOWF, lowF);
         pcLowF.setText(", lowF: "+ (int) lowF);
       return;
    }

    if (obj == pcHighFSlider){
         int val = pcHighFSlider.getValue();
         // System.out.println("pcHighFSlider: " + val);
         double tmp = (val*maxFIndex + (100-val)*minFIndex)/100.0;
         float highF = (float) (440.0*Math.pow(2.0,tmp));
         effector.setParam(FFTEffector.OP_HIGHF, highF);
         pcHighF.setText(", HighF: "+ (int) highF);
       return;
    }

    if (obj == pcProgressBar){
         int val = pcProgressBar.getValue();
         if (val == pcProgressBar.getMaximum()){
           // System.out.println("pcProgressBar: " + val);
           // pcStartAbortButton.setText("COMPLETE");
           pcStartAbortButton.setText("Play Saved?");
         }
       return;
    }

    if (obj == psProgressBar){
         int val = psProgressBar.getValue();
         if (val == psProgressBar.getMaximum()){
           // System.out.println("psProgressBar: " + val);
           psStartAbortButton.setText("Play Saved?");
         }
       return;
    }

 // PercussionSplitter
    if (obj == psMixSlider){
       int val = psMixSlider.getValue();
       effector.setParam(FFTEffector.OP_MIX, val/100f);
       psMix.setText("Mix: " + val/100f);
        // System.out.println("psMixSlider: " + val);
      return;
    }
    if (obj == psLowFSlider){
         int val = psLowFSlider.getValue();
          // System.out.println("psLowFSlider: " + val);
         double tmp = (val*maxFIndex + (100-val)*minFIndex)/100.0;
         float lowF = (float) (440.0*Math.pow(2.0,tmp));
         effector.setParam(FFTEffector.OP_LOWF, lowF);
         psLowF.setText(", lowF: "+ (int) lowF);
       return;
    }
    
    if (obj == psHighFSlider){
       int val = psHighFSlider.getValue();
        // System.out.println("psHighFSlider: " + val);
       double tmp = (val*maxFIndex + (100-val)*minFIndex)/100.0;
         float highF = (float) (440.0*Math.pow(2.0,tmp));
         effector.setParam(FFTEffector.OP_HIGHF, highF);
         psHighF.setText(", highF: "+ (int) highF);
       return;
    }

   // else
     System.out.println("stateChanged() from unknown object");

  }

  private JPanel createTopPanel(){
       JPanel panel = new JPanel();
 //      panel.add(new JSeparator(SwingConstants.HORIZONTAL));
       panel.setLayout(new GridLayout(4,2,10,10)); // 10,10 are hgap, vgap

       JLabel label;
       label = new JLabel("Version: " + serialVersionUID);
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(label);
       label = new JLabel("by goto@kmgoto.jp");
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(label);
 //      panel.add(new JSeparator(SwingConstants.HORIZONTAL));

       inputFileButton = new JButton("select input file"); 
       inputFileButton.setFont(new Font("Serif", Font.PLAIN, 20));
       inputFileButton.addActionListener(this);
       inputFileLabel = new JLabel(
"<HTML>Select supported sound file<BR>(16bit stereo wav)</HTML>");
       inputFileLabel.setFont(new Font("Serif", Font.PLAIN, 20));
       
       panel.add(inputFileButton);
       panel.add(inputFileLabel);

       // button = new JButton("Input File");
       String[] items = {"Select One","Player",
          "PanCanceler","PercussionSplitter"};

//       JComboBox<String> 
       effectCombo = new JComboBox<String>(items);
       effectCombo.setSelectedIndex(0); // select one
       effectCombo.setFont(new Font("Serif", Font.PLAIN, 20));
       effectCombo.addActionListener(this);
       label = new JLabel("Choose Effect");
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(effectCombo);
       panel.add(label);

       closeButton = new JButton("Close");
       closeButton.setFont(new Font("Serif", Font.PLAIN, 20));
       closeButton.addActionListener(this);
       label = new JLabel("Close Application");
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(closeButton);
       panel.add(label);
 
       return panel;
  }

  private JPanel createTopHelpPanel(){
       JPanel panel = new JPanel();
       panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
//       panel.add(Box.createVerticalStrut(30));
//       panel.add(Box.createHorizontalStrut(10)); 
       panel.add(new JSeparator(SwingConstants.HORIZONTAL));

       JTextArea text = new JTextArea(Top_help);
       text.setFont(new Font("Serif", Font.PLAIN, 18));
       text.setEditable(false);
       panel.add(text);

       return panel;
  }

  private JPanel createPlayerPanel(){

       JPanel panel = new JPanel();
       panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
       JLabel label = new JLabel("Player Menu");
//       label.setAlignmentX(RIGHT_ALIGNMENT);
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(label);

       JPanel subpanel = new JPanel();
//       subpanel.setLayout(new BoxLayout(subpanel,BoxLayout.X_AXIS));
 //      subpanel.setAlignmentX(LEFT_ALIGNMENT);
       plTotalTime = new JLabel("Total(sec): unknown");
       plTotalTime.setFont(new Font("Serif", Font.PLAIN, 20));
       subpanel.add(plTotalTime);

       plTime = new JLabel(" | Current(sec): 0");
       plTime.setFont(new Font("Serif", Font.PLAIN, 20));
       subpanel.add(plTime);
       panel.add(subpanel);

       plTimeSlider = new JSlider();
       plTimeSlider.setValue(0);
       plTimeSlider.addChangeListener(this);
       panel.add(plTimeSlider);

/*
       subpanel = new JPanel();
//       subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.X_AXIS));
//       subpanel.setAlignmentX(LEFT_ALIGNMENT);
       plPlaySaveButton = new JButton("Play");
       plPlaySaveButton.setFont(new Font("Serif", Font.PLAIN, 20));
       plPlaySaveButton.addActionListener(this);
       label = new JLabel("Play/Save/Play&Save");
//       label.setAlignmentX(RIGHT_ALIGNMENT);
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       subpanel.add(plPlaySaveButton); 
       subpanel.add(label);
*/

       subpanel = new JPanel();
//       subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.X_AXIS));
       plStartStopButton = new JButton("Start");
       plStartStopButton.setFont(new Font("Serif", Font.PLAIN, 20));
       plStartStopButton.addActionListener(this);
       label = new JLabel("Start/Stop(Pause)");
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       subpanel.add(plStartStopButton); subpanel.add(label);
       panel.add(subpanel);

       label = new JLabel("Volume: 100");
//       label.setAlignmentX(RIGHT_ALIGNMENT);
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(label);

       plVolumeSlider = new JSlider();
       plVolumeSlider.setMinimum(0);
       plVolumeSlider.setMaximum(150);
       plVolumeSlider.setValue(100);
       plVolumeSlider.addChangeListener(this);
       panel.add(plVolumeSlider);

       JTextArea text = new JTextArea(Player_help);
       text.setFont(new Font("Serif", Font.PLAIN, 18));
       text.setEditable(false);
       panel.add(text);

       return panel;
  }

  private JPanel createPanCancelerPanel(){

       JPanel panel = new JPanel();
       panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//       panel.setAlignmentX(0.1f);

       JLabel label = new JLabel("-----PanCanceler Menu----------------");
       label.setFont(new Font("Serif", Font.PLAIN, 20));
//       label.setAlignmentX(0.1f);
       panel.add(label);

       JPanel subpanel = new JPanel();
//       subpanel.setAlignmentX(0.1f);
//       subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.X_AXIS));
       pcPan = new JLabel("Pan: ");
       pcPan.setFont(new Font("Serif", Font.PLAIN, 20));
       pcWidth = new JLabel(", Width: ");
       pcWidth.setFont(new Font("Serif", Font.PLAIN, 20));
       pcLowF = new JLabel(", LowF(Hz): ");
       pcLowF.setFont(new Font("Serif", Font.PLAIN, 20));
       pcHighF= new JLabel(", HighF(Hz): ");
       pcHighF.setFont(new Font("Serif", Font.PLAIN, 20));
       subpanel.add(pcPan); 
       subpanel.add(pcWidth);
       subpanel.add(pcLowF); 
       subpanel.add(pcHighF);
       panel.add(subpanel);

       pcPanSlider = new JSlider();
       pcPanSlider.addChangeListener(this);
       panel.add(Box.createVerticalStrut(10));
       panel.add(pcPanSlider);

       pcWidthSlider = new JSlider();
       pcWidthSlider.addChangeListener(this);
       panel.add(Box.createVerticalStrut(10));
       panel.add(pcWidthSlider);

       pcLowFSlider = new JSlider();
       pcLowFSlider.addChangeListener(this);
       panel.add(Box.createVerticalStrut(10));
       panel.add(pcLowFSlider);

       pcHighFSlider = new JSlider();
       pcHighFSlider.addChangeListener(this);
       panel.add(pcHighFSlider);

       // JPanel subpanel = new JPanel();
       subpanel = new JPanel();
//       subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.X_AXIS));
//       subpanel.setAlignmentX(0.1f);

       label = new JLabel("Cancel/Extract");
       label.setFont(new Font("Serif", Font.PLAIN, 20));
//       label.setAlignmentX(0.1f);
       subpanel.add (label);

       pcCancelExtractButton= new JButton("Cancel");
//       pcCancelExtractButton.setAlignmentX(0.1f);
       pcCancelExtractButton.setFont(new Font("Serif", Font.PLAIN, 20));
       pcCancelExtractButton.addActionListener(this);
       subpanel.add (pcCancelExtractButton);

       label = new JLabel("Start/Abort");
 //      label.setAlignmentX(LEFT_ALIGNMENT);
//       label.setAlignmentX(0.1f);
       label.setFont(new Font("Serif", Font.PLAIN, 20));
//       subpanel.add(Box.createVerticalStrut(10));
//       subpanel.add(Box.createHorizontalStrut(10)); 
       subpanel.add (label);

       pcStartAbortButton = new JButton("Start");
       pcStartAbortButton.setAlignmentX(0.1f);
       pcStartAbortButton.setFont(new Font("Serif", Font.PLAIN, 20));
       pcStartAbortButton.addActionListener(this);
       subpanel.add(Box.createHorizontalStrut(10)); 
       subpanel.add (pcStartAbortButton);

       panel.add(Box.createVerticalStrut(10));
       panel.add(subpanel);


       JLabel pcProgress = new JLabel("Progress: ");
       pcProgress.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(pcProgress);

       pcProgressBar = new JProgressBar();
       pcProgressBar.setStringPainted(true);
       pcProgressBar.addChangeListener(this);
       panel.add(pcProgressBar);

       JTextArea text = new JTextArea(PanCanceler_help);
       text.setFont(new Font("Serif", Font.PLAIN, 20));
       text.setEditable(false);
       panel.add(Box.createVerticalStrut(10));
       panel.add(text);

       return panel;
  }

  private JPanel createPercussionSplitterPanel(){

       JPanel panel = new JPanel();
       panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

       JLabel label = new JLabel("<HTML><HR>Percussion Splitter Menu</HTML>");
       label.setFont(new Font("Serif", Font.PLAIN, 20));
       panel.add(label);

       JPanel subpanel = new JPanel();
//       subpanel.setAlignmentX(0.1f);
//       subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.X_AXIS));
       psMix = new JLabel(); psMix.setFont(new Font("Serif", Font.PLAIN, 20));
       psLowF = new JLabel(); psLowF.setFont(new Font("Serif", Font.PLAIN, 20));
       psHighF = new JLabel(); psHighF.setFont(new Font("Serif", Font.PLAIN, 20));
       subpanel.add(psMix);
       subpanel.add(psLowF);
       subpanel.add(psHighF);
       panel.add(subpanel);

       psMixSlider = new JSlider();
       psMixSlider.addChangeListener(this);
       panel.add(Box.createVerticalStrut(10));
       panel.add(psMixSlider);

       psLowFSlider = new JSlider();
       psLowFSlider.addChangeListener(this);
       panel.add(Box.createVerticalStrut(10));
       panel.add(psLowFSlider);

       psHighFSlider = new JSlider();
       psHighFSlider.addChangeListener(this);
       panel.add(Box.createVerticalStrut(10));
       panel.add(psHighFSlider);

       psStartAbortButton = new JButton("Start");
       psStartAbortButton.setFont(new Font("Serif", Font.PLAIN, 20));
       psStartAbortButton.addActionListener(this);
       panel.add(psStartAbortButton);

       psProgressBar = new JProgressBar();
       psProgressBar.setStringPainted(true);
       psProgressBar.addChangeListener(this);
       panel.add(psProgressBar);

       JTextArea text = new JTextArea(PercussionSplitter_help);
       text.setFont(new Font("Serif", Font.PLAIN, 18));
       text.setEditable(false);
       panel.add(text);

       return panel;
  }

  private File getInputFile(){

     File retval = null;
     UIManager.put("FileChooser.readOnly", Boolean.TRUE);
     JFileChooser fc = new JFileChooser();
     fc.setFileFilter( 
       new FileNameExtensionFilter("Audio Files(wav)", "wav")
     );

     if(fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
        return null; 

     retval = fc.getSelectedFile();
/*
// Check audio format
     AudioInputStream inputStream = null;
     try {
       inputStream = AudioSystem.getAudioInputStream(retval);
     } catch (Exception e) { e.printStackTrace(); } 
    
     AudioFormat audioFormat = inputStream.getFormat();
     System.out.println("Format: " + audioFormat.toString());
*/

     return retval;
  }

}
