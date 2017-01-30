import java.io.*;
import java.util.Locale;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jp.kmgoto.music.*;

class FilterGUI extends JFrame implements ActionListener {
  private JButton button[][];
  private float[] freqRanges, panRanges;
  private AdvancedFilter filter = null;
  private JRadioButton through,mute,split, harmonic, percussive,
          throughall, muteall, harmonicall, percussiveall, splitall;
  private JRadioButton karaokeMale, drumCover;
  private String operation = new String("T");
  private JButton loadButton, saveButton;
  private Object parent;

  FilterGUI(AdvancedFilter filter, int panStep, int freqStep){
    setTitle("Advanced Filter Setting");
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    JPanel panel = new JPanel();

    this.filter = filter; // external object
    filter.setRangeAndType(-1f, 1f, 0f,30000f,"T");

    int x = panStep, y = freqStep;
    panel.setLayout(new GridLayout(y+1,x+1,0,0));
    button = new JButton[x][y];
    freqRanges = new float[y];
    panRanges = new float[x];


    JLabel label=null;
    for (int f = 0; f < y; f++){
      freqRanges[y-1-f] = (float) (13.75*Math.pow(2.0,6*(y-1-f)/12.0));
      int freq = (int) freqRanges[y-1-f];
      label = new JLabel(String.format("%5d",freq));
      label.setHorizontalAlignment(JLabel.RIGHT);
      panel.add(label);
      for (int p =0; p < x; p++){
        JButton tmp = new JButton("T");
        button[p][y-1-f] = tmp;
        tmp.setMargin(new Insets(0, 0, 0, 0));
        tmp.setBackground(Color.WHITE);
        tmp.addActionListener(this);
        panel.add(tmp);
      }      
    }

    for (int p = 0; p < button.length; p++)
       for (int f = 0; f < button[0].length; f++) 
             button[p][f].setText("T");

//    filter.dumpFilterArray();
    
    label = new JLabel(String.format("Hz/LR"));
    panel.add(label);

    for (int p=0; p < x; p++){
       int pan = (p - x/2);
       panRanges[p] = pan/10f;
       label = new JLabel(String.format("%4.1f",pan/10f));
       label.setHorizontalTextPosition(JLabel.CENTER);
       panel.add(label);
    }
    add(panel, BorderLayout.NORTH); 

    JPanel panel2 = new JPanel();
      panel2.setLayout(new GridLayout(2,10));
      through = new JRadioButton("THROUGH"); 
          through.addActionListener(this);
      mute = new JRadioButton("MUTE"); 
          mute.addActionListener(this);
      percussive = new JRadioButton("PERCUSSIVE"); 
          percussive.addActionListener(this);
      harmonic = new JRadioButton("HARMONIC"); 
          harmonic.addActionListener(this);
      split = new JRadioButton("SPLIT"); 
          split.addActionListener(this);
      throughall = new JRadioButton("THROUGHALL"); 
          throughall.addActionListener(this);
      muteall = new JRadioButton("MUTEALL"); 
         muteall.addActionListener(this);
      harmonicall = new JRadioButton("HARMONICALL"); 
         harmonicall.addActionListener(this);
      percussiveall = new JRadioButton("PERCUSSIVEALL"); 
         percussiveall.addActionListener(this);
      splitall = new JRadioButton("SPLITALL"); 
         splitall.addActionListener(this);

/*
      JSeparator sp = new JSeparator(JSeparator.HORIZONTAL);
      sp.setPreferredSize(new Dimension(100,100));
*/

      JLabel l = new JLabel("Presets: ");

      karaokeMale = new JRadioButton("karaokeMale"); 
         karaokeMale.addActionListener(this);
      drumCover = new JRadioButton("drumCover"); 
         drumCover.addActionListener(this);

    ButtonGroup group = new ButtonGroup();
       group.add(through); group.add(mute); 
       group.add(percussive); group.add(harmonic); group.add(split); 
       group.add(throughall); group.add(muteall); 
       group.add(percussiveall); group.add(harmonicall); group.add(splitall); 
       group.add(karaokeMale); group.add(drumCover); 
       throughall.setSelected(true);

     panel2.add(through); panel2.add(mute);
     panel2.add(percussive); panel2.add(harmonic); panel2.add(split); 
     panel2.add(throughall); panel2.add(muteall); 
     panel2.add(percussiveall); panel2.add(harmonicall); 
     panel2.add(splitall); 
     panel2.add(l);
     panel2.add(karaokeMale); panel2.add(drumCover); 
    add(panel2, BorderLayout.CENTER);

    JPanel panel3 = new JPanel();
    saveButton = new JButton("Save filter"); saveButton.addActionListener(this);
    loadButton = new JButton("load filter"); loadButton.addActionListener(this);
    panel3.add(saveButton); panel3.add(loadButton);

    add(panel3, BorderLayout.SOUTH);

    pack();
    setVisible(true); 
  }

  public void setParent(Object parent){
    this.parent = parent;
  }

@Override
  public void actionPerformed(ActionEvent event){
     Object obj = event.getSource();

     if (obj instanceof JRadioButton){

         if (obj == through) {operation = new String("T"); return;}
         if (obj == mute){operation = new String("M"); return;}
         if (obj == harmonic){operation = new String("H"); return;}
         if (obj == percussive){operation = new String("P"); return;}
         if (obj == split) {operation = new String("S"); return;}

         if (obj == throughall) {
           operation = new String("T"); 
           for (int p = 0; p < button.length; p++)
           for (int f = 0; f < button[0].length; f++) 
                       button[p][f].setText(operation);
           filter.setRangeAndType(-1f, 1f, 0f,30000f,operation);
           return;
         }

         if (obj == muteall){
           operation = new String("M"); 
           for (int p = 0; p < button.length; p++)
           for (int f = 0; f < button[0].length; f++)
                button[p][f].setText(operation);
           filter.setRangeAndType(-1f, 1f, 0f,30000f,operation);
           return;
         } 

         if (obj == percussiveall){
           operation = new String("P"); 
           for (int p = 0; p < button.length; p++)
           for (int f = 0; f < button[0].length; f++)
                button[p][f].setText(operation);
           filter.setRangeAndType(-1f, 1f, 0f,30000f,operation);
           return;
         } 

         if (obj == harmonicall){
           operation = new String("H"); 
           for (int p = 0; p < button.length; p++)
           for (int f = 0; f < button[0].length; f++)
                button[p][f].setText(operation);
           filter.setRangeAndType(-1f, 1f, 0f,30000f,operation);
           return;
         } 

         if (obj == splitall){
           operation = new String("S"); 
           for (int p = 0; p < button.length; p++)
           for (int f = 0; f < button[0].length; f++)
                button[p][f].setText(operation);
           filter.setRangeAndType(-1f, 1f, 0f,30000f,operation);
           return;
         }

         if (obj == karaokeMale){
           filter.setRangeAndType(-1f, 1f, 0f,30000f,"T");
           float panL = -0.1f, panR = 0.1f;
           float freqL = 220f, freqH = 3400f;
           filter.setRangeAndType(panL,panR,freqL,freqH,"M");

           for (int p = 0 ; p < button.length; p++){
             for (int f = 0 ; f < button[0].length; f++){
              if (panRanges[p] >= panL && panRanges[p] <= panR
                  && freqRanges[f] >= freqL && freqRanges[f] <= freqH)
                  button[p][f].setText("M");
              else button[p][f].setText("T");
             }
           }

           return;
         }

         if (obj == drumCover){
           filter.setRangeAndType(-1f, 1f, 0f,30000f,"H");

           float panL = -0.1f, panR = 0.1f;
           float freqL = 220f, freqH = 3400f;
           filter.setRangeAndType(panL,panR,freqL,freqH,"T");
           filter.setRangeAndType(-1f,-0.9f,freqL,freqH,"T");
           filter.setRangeAndType(0.9f,1f,freqL,freqH,"T");

           for (int p = 0 ; p < button.length; p++){
             for (int f = 0 ; f < button[0].length; f++){
              if ((freqRanges[f] >= freqL && freqRanges[f] <= freqH)
                && ((panRanges[p] >= panL && panRanges[p] <= panR)
                  || panRanges[p] <= -0.9f
                  || panRanges[p] >= 0.9f)
                )  button[p][f].setText("T");
               else button[p][f].setText("H");
             }
           }
           return;
         }
       return;
     }

     if (obj instanceof JButton){

       if (obj == saveButton) {saveFilter(); return;}
       if (obj == loadButton) {loadFilter(); return;}

       for (int p = 0; p < button.length; p++)
         for (int f = 0; f < button[0].length; f++)
          if (obj == button[p][f]){
            button[p][f].setText(operation);

        float panL=0f, lowF=0f;

        if (p == 0) panL = -1f; else panL = panRanges[p-1] + 0.05f;
        if (f == 0) lowF = 0f; else lowF = freqRanges[f-1];

        filter.setRangeAndType(panL, panRanges[p]+0.05f,
                               lowF,freqRanges[f],operation);
/*
            System.out.print("button (" + p + "," + f + "): "); 
            if (p == 0)
              System.out.print("pan " + " -1 -- " 
              + String.format("%6.2f", panRanges[p]+0.05)); 
            else if (p == button[0].length - 1)
              System.out.print("pan " 
                + String.format("%6.2f", panRanges[p]-0.05) + " -- 1"); 
            else 
              System.out.print("pan " 
               + String.format("%6.2f", panRanges[p-1]+0.05) 
               + " -- " + String.format("%6.2f", panRanges[p]+0.05)); 

            if (f > 0)
              System.out.println(", freq " 
                  + String.format("%8.2f",freqRanges[f-1]) 
                  + " -- " + String.format("%8.2f", freqRanges[f])); 
            else 
              System.out.println(", freq " + "0 -- " 
                  + String.format("%8.2f",freqRanges[f])); 
*/
            return;
          }
     }

  }

 public void saveFilter(){
  if (filter == null) {System.out.println("No filter"); return;}
  UIManager.put("FileChooser.readOnly", Boolean.FALSE);
  JFileChooser fc = new JFileChooser("./");
  fc.setFileFilter(new FileNameExtensionFilter("AdvancedFilter(flt)", "flt"));
  fc.setFileSelectionMode(JFileChooser.FILES_ONLY); 

  File saveFile = null;
  if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
     saveFile = fc.getSelectedFile();
     String path = saveFile.getAbsolutePath();
     if(!path.substring(path.length()-4).equals(".flt"))
        saveFile = new File(path + ".flt");
  } else return;

  try {
    FileOutputStream fos = new FileOutputStream(saveFile);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(filter);
    oos.close();
  } catch (Exception e){
    e.printStackTrace();
  }

 }
 public void loadFilter(){
  UIManager.put("FileChooser.readOnly", Boolean.TRUE);
  JFileChooser fc = new JFileChooser("./");
  fc.setFileFilter( new FileNameExtensionFilter("AdvancedFilter(flt)", "flt"));
  fc.setFileSelectionMode(JFileChooser.FILES_ONLY); 

  File saveFile = null;
  if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
     saveFile = fc.getSelectedFile();
  } else return;

  try {
    FileInputStream fis = new FileInputStream(saveFile);
    ObjectInputStream ois = new ObjectInputStream(fis);
    filter = (AdvancedFilter) ois.readObject();
    ois.close();
    ((FilteredPlayerApp) parent).resetFilter(filter);
  } catch (Exception e){
    e.printStackTrace();
  }

  for (int p = 0; p < button.length; p++){
    for (int f = 0; f < button[0].length; f++){
      float panL = 0f; float lowF = 0f;
      if (p == 0) panL = -1f; else panL = panRanges[p-1] + 0.05f;
      if (f == 0) lowF = 0f; else lowF = freqRanges[f-1];

      button[p][f].setText(
         filter.getValue(
          (panL+panRanges[p]+0.05f)/2f, (lowF+freqRanges[f])/2f
         )
      );
    }
  }
 }

 public static void main(String[] args){
   Locale.setDefault(Locale.US);
   AdvancedFilter filter = new AdvancedFilter(44100f,4096,21);
   new FilterGUI(filter,21,23);
 }

}
