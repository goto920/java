FeedbacBooster

Guitar ----> Guitar Amp ----> Mic or amp line/headphone output 
vib. SP                         |
   ^                            | input
   |                            V
   +--------------- Audio Interface ----+
                          ^             | (mono)
                          |             V
                          |          Pitch detection
                          |             |
                          |             V
                          |       peakEQ  peakEQ(octave up) 
                          |            (mix)
                          |             |
                          |             V  
                          |         compressor
                          |             |
                          |(mono)       V
                          +-------- output volume

Controls with GUI:
 A) Input device selector (wave file input for test)               
 B) Output device selector
 C) Auto/Manual EQ (using pitch)
 D) Manual EQ peak frequency
    D2) mix
 E) Auto/Manual EQ Q value adjustment
 F) compressor threshold, G) ratio (1 .. max)
 H) output volume
 I) process/bypass (for comparison)

GUI design

------------------------
   A   |   B (JComboBox)
------------------------
   I   |   C (JButton)
-----------------------
 D value (JLabel) | D slider(JSlider) (manual only)
 D2 value         | D2 slider
--------------------------
 E value          | E slider
--------------------------
 F value          | F slider
 G value          | G slider
--------------------------
 H value          | H slider
------------------------
 Others (if necessary)
----------------------

File selector (external)

++++++++++++++++++++++++++
Q and bandwidth

http://site2913.com/music/eq-bandwidth/
 1.41 12 semitone
 2.14  8 semitone
 2.87  6
 4.32  4
 8.65  2 
17.31 1
++++++++++++++++++++++++
