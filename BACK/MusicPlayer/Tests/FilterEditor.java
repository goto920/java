/**
 * Written by goto@kmgoto.jp 
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import jp.kmgoto.musicplayer.PanPercFilter;

class FilterEditor extends JFrame implements ActionListener {

  private final String THROUGH = "t";
  private final String CANCEL  = "c";
  private final String SPLIT   = "s";
  private final int SELECT_THROUGH = 1;
  private final int SELECT_CANCEL = 2;
  private final int SELECT_SPLIT = 3;
  private final int SELECT_THROUGHALL = 4;
  private final int SELECT_CANCELALL = 5;
  private final int SELECT_SPLITALL = 6;
  private int OPERAITON; // one of SELECT
  private JRadioButton through,cancel,split, throughall, cancelall, splitall;
  private JButton button[][];
  private JFrame frame;
  private PanPercFilter filter;
  private int x, y;
  private Object parent; 

  public FilterEditor(Object parent){
     this.parent = parent;
     frame = new JFrame(); 
     frame.setTitle("Advanced setting");
     frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
     JPanel panel = new JPanel();
     JPanel subpanel;
     filter = new PanPercFilter();
 
     panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));

     x = 21;
     y = 23; 
     subpanel = new JPanel(); 
     subpanel.setLayout(new GridLayout(y+1,x+1,0,0));
//     JCheckBox button;
     JLabel label;

     button = new JButton[y][x];

     float[] freqRanges = new float[y];
     float[] panRanges = new float[x];

     for (int f = 0; f < y; f++){
      freqRanges[y-1-f] = (float) (13.75*Math.pow(2.0,6*(y-1-f)/12.0));
      int freq = (int) freqRanges[y-1-f];
      label = new JLabel(String.format("%5d",freq));
      label.setHorizontalTextPosition(JLabel.RIGHT);
      subpanel.add(label);
      for (int p =0; p < x; p++){
        button[y-1-f][p] = new JButton(THROUGH);
        button[y-1-f][p].setBackground(Color.GREEN);
        button[y-1-f][p].addActionListener(this);
        subpanel.add(button[y-1-f][p]);
      }      
     }

      label = new JLabel(String.format("Hz/LR"));
      subpanel.add(label);

      for (int p=0; p < x; p++){
       int pan = (p - x/2);
       panRanges[p] = pan/10f;
       label = new JLabel(String.format("%4.1f",pan/10f));
       label.setHorizontalTextPosition(JLabel.CENTER);
       subpanel.add(label);
      }
     panel.add(subpanel);

     filter.setFreqRanges(freqRanges);
     filter.setNumPanRanges(x);

     subpanel = new JPanel();
     label = new JLabel("Select operation: ");
     subpanel.add(label);
      through = new JRadioButton("THROUGH");
      cancel = new JRadioButton("CANCEL");
      split = new JRadioButton("SPLIT");
      throughall = new JRadioButton("THROUGHALL");
        throughall.addActionListener(this);
      cancelall = new JRadioButton("CANCELALL");
        cancelall.addActionListener(this);
      splitall = new JRadioButton("SPLITALL");
        splitall.addActionListener(this);

     ButtonGroup group = new ButtonGroup();
     group.add(through); 
     group.add(cancel); 
     group.add(split); 
     group.add(throughall); 
     group.add(cancelall); 
     group.add(splitall); 

     subpanel.add(through);
     subpanel.add(cancel);
     subpanel.add(split);
     subpanel.add(throughall);
     subpanel.add(cancelall);
     subpanel.add(splitall);
     panel.add(subpanel);

     subpanel = new JPanel();
     JButton b = new JButton("Apply");
           b.addActionListener(this); subpanel.add(b);
         b = new JButton("Apply&Exit");
           b.addActionListener(this); subpanel.add(b);
         b = new JButton("Load"); 
           b.addActionListener(this); subpanel.add(b);
         b = new JButton("Save"); 
           b.addActionListener(this); subpanel.add(b);
     panel.add(subpanel);

     frame.add(panel);
     frame.pack();
     frame.setVisible(true);
  }

  public static void main(String[] args){
     new FilterEditor(null);
  }

@Override
  public void actionPerformed(ActionEvent event){
     Object obj = event.getSource();

     if (obj instanceof JButton){
       JButton b = (JButton) obj;

       if (b.getText().equals("Apply")){ 
         System.out.println("Apply");
         if (parent != null)
           ((OpenFilterEditor) parent).setAdvancedFilter(null);
         return;
       }
       if (b.getText().equals("Apply&Exit")){
         System.out.println("Apply&Exit not implemented yet.");
         return;
       }
       if (b.getText().equals("Load")){
         System.out.println("Load not implemented yet.");
         return;
       }
       if (b.getText().equals("Save")){
         String[][] data = new String[y][x];
         for (int f = 0; f < y; f++)
           for (int p = 0; p < x; p++)
             data[f][p] = button[f][p].getText();

         filter.setData(data);
//         filter.printContent();
         filter.saveContent("test.flt");

        // System.out.println("Save");
         return;
       }


       if (b.getText().equals(THROUGH)
          || b.getText().equals(CANCEL)
          || b.getText().equals(SPLIT)) {

         if (through.isSelected()){
          b.setText(THROUGH);
          b.setBackground(Color.GREEN);
         } else if (cancel.isSelected()){
          b.setText(CANCEL);
          b.setBackground(Color.RED);
         } else if (split.isSelected()){
          b.setText(SPLIT);
          b.setBackground(Color.BLUE);
         }
         frame.pack();
         return;
      } 
     } // End JButton

    if (obj instanceof JRadioButton){
        JRadioButton rb = (JRadioButton) obj;
        String text = "";
        Color color = null;
        if (rb == throughall){
           text = THROUGH; color = Color.GREEN;
        } else if (rb == cancelall){
           text = CANCEL; color = Color.RED;
        } else if (rb == splitall){
           text = SPLIT; color = Color.BLUE;
        } 

      for (int f = 0; f < button.length; f++){
        for (int p = 0; p < button[0].length; p++){
          button[f][p].setText(text);  
          button[f][p].setBackground(color);
        }
      }
      frame.pack();
      return;
    }

  }

}
