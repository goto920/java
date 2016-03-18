/**
 * Written by goto@kmgoto.jp 
 */
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jp.kmgoto.musicplayer.swing.ConvertToWave;

class ConvertToWave16App extends JFrame 
  implements ActionListener {

  private static final long serialVersionUID = 20160316;
  private Locale locale;

  private JFrame frame; // for pack() 
  private File inputFile;
  private JButton inputFileButton, closeButton, startButton;
  private JLabel  inputFileLabel;
  private JTextArea statusText;

  public ConvertToWave16App() { // constructor

    if (Locale.getDefault() != Locale.JAPAN) {
       Locale.setDefault(Locale.US);
       JFileChooser.setDefaultLocale(Locale.US);
    }

    ResourceBundle bundle = null;
    String help = "help";
    try {
      bundle = ResourceBundle.getBundle("HELP");
      help = bundle.getString("help");
    } catch (Exception e) { e.printStackTrace();}

    frame = new JFrame();
    frame.setTitle("Convert to 16bit wave");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 

    JPanel bassPanel = new JPanel();
    bassPanel.setLayout(new BoxLayout(bassPanel,BoxLayout.Y_AXIS));
    inputFileButton = new JButton("Select Input File");
    inputFileButton.setFont(new Font("Serif", Font.PLAIN, 20));
    inputFileButton.setAlignmentX(1f);
    inputFileButton.addActionListener(this);
    bassPanel.add(inputFileButton);

    startButton = new JButton("Start");
    startButton.setFont(new Font("Serif", Font.PLAIN, 20));
    startButton.setAlignmentX(1f);
    startButton.addActionListener(this);
    bassPanel.add(startButton);


    JTextArea helpText = new JTextArea("Build: " + serialVersionUID 
        + "\n" + help);
    helpText.setFont(new Font("Serif", Font.PLAIN, 18));
    helpText.setEditable(false);
    bassPanel.add(helpText);

    statusText = new JTextArea("Status: ");
    statusText.setFont(new Font("Serif", Font.PLAIN, 18));
    statusText.setEditable(false);
    bassPanel.add(statusText);

    closeButton = new JButton("Close");
    closeButton.setFont(new Font("Serif", Font.PLAIN, 20));
//    closeButton.setAlignmentX(1f);
    closeButton.addActionListener(this);
    bassPanel.add(closeButton);

    frame.add(bassPanel); 
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args){
     new ConvertToWave16App();
  }

  public void actionPerformed(ActionEvent event){
      Object obj = event.getSource();

      if(obj == inputFileButton){
        inputFile = getInputFile();
        System.out.println("inputFileButton file " 
           + inputFile.getAbsolutePath());
        startButton.setText("Start");
        return;
      }

      if(obj == startButton){
        String filename = inputFile.getAbsolutePath();
        System.out.println("startButton");
        String output = filename + "-16bit.wav";
        int samplingRate 
          = ConvertToWave.anyToWaveFile(filename, output);
        if (samplingRate > 0){
          startButton.setText("COMPLETE"); 
          statusText.setText("Status: " 
            + "\nInput: " + filename
            + "\nOutput: " + output);
          frame.pack();
        } else if(samplingRate == 0) {
          statusText.setText(
             "Status: No conversion necessary\nInput wave file is in 16bit."); 
        } else {
          statusText.setText("Status: Sorry, something is wrong"); 
        }
        return;
      } 

      if(obj == closeButton){
         System.out.println("closeButton");
         dispose();
         System.exit(0);
         return;
      } 

  }

  private File getInputFile(){

     UIManager.put("FileChooser.readOnly", Boolean.TRUE);
     JFileChooser fc = new JFileChooser();
     fc.setFileFilter( 
       new FileNameExtensionFilter("Audio Files(mp3,m4a,aac,mp4,wav)", 
          "mp3","m4a","aac","mp4","wav")
     );

     if(fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION)
        return null; 

     return fc.getSelectedFile();
    
  }

}
