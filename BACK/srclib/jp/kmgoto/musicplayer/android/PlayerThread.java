// docs.oracle.com -- Java Thread primitive Deprecation
package jp.kmgoto.musicplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.widget.SeekBar;

//import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
// import java.util.ArrayList;

public class PlayerThread extends Thread {

        private MediaExtractor soundMedia;
        private int trackID;
        private MediaCodec decoder;
        private AudioTrack player;
        private String saveFile;
        private BufferedOutputStream bos;

        private volatile boolean running;
        private volatile long sampleTime;
        private volatile SeekBar playTimeSlider;
        private boolean play;

        public synchronized void stopPlay(){ running = false; }

        public PlayerThread(MediaExtractor soundMedia, int trackID, boolean play, String saveFile, SeekBar playTimeSlider){
            this.soundMedia = soundMedia; this.trackID = trackID;
            if(saveFile != null) this.saveFile = saveFile;
            this.play = play;
            this.playTimeSlider = playTimeSlider;
            init();
        }

        public synchronized void setVolume(float vol){
          if (player != null) player.setVolume(vol);
        }

        public synchronized long getSampleTime() { return sampleTime;}

        @Override
        public void run(){
            decoder.start();
            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

  //          if (player !=null) player.play();
  //          Log.d("PLAYER","Player Starting");

            boolean outputDone = false;
            boolean sawInputEOS = false;
            running = true;
            long lastTime = 0;
            while(!outputDone && running) {

               if (!sawInputEOS) {
                   int inputBufferID = decoder.dequeueInputBuffer(0);

                   if (inputBufferID >= 0) {
                       // ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferID);
                       int len = soundMedia.readSampleData(inputBuffers[inputBufferID], 0);

                       if (len < 0) {
                           Log.d("PLAYER", "Input done");
                           sawInputEOS = true;
                           decoder.queueInputBuffer(inputBufferID, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                           // fill 0 for len and sample time
                       } else {
                           sampleTime = soundMedia.getSampleTime(); // usec
                           decoder.queueInputBuffer(inputBufferID, 0, len, sampleTime, 0);

                           if (sampleTime > lastTime + 100000) { // 0.1 sec
                               lastTime = sampleTime;
                               if (playTimeSlider != null) {
                                   playTimeSlider.setProgress((int) (sampleTime / 1000)); // msec
                               }
                           }
                                                  }

                       if (!sawInputEOS) soundMedia.advance();
                   } // End if there is some input

               } // End Input part

               int outputBufferID = decoder.dequeueOutputBuffer(info, 100000); // usec timeout (-1 inf.)

               if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                     Log.d("PLAYER", "Output done exiting loop...");
                     outputDone = true;
               }

               if (outputBufferID >= 0) {
                     final byte[] chunk = new byte[info.size];
                     outputBuffers[outputBufferID].get(chunk);

                     if (chunk.length > 0) {
                         if (player != null) player.write(chunk, 0, chunk.length); // player output

                         if (bos != null) {
                             try {
                                 bos.write(chunk, 0, chunk.length);
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                         } // file output

                         decoder.releaseOutputBuffer(outputBufferID, false);
                     }

               } else if (outputBufferID == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                     // Log.d("PLAYER_THREAD", "Output buffers changed");
                      outputBuffers = decoder.getOutputBuffers();
               } else if (outputBufferID == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                      MediaFormat oformat = decoder.getOutputFormat();
                      Log.d("PLAYER_THREAD","output format changed " + oformat);
               } else {
                      Log.d("PLAYER_THREAD", "dequeueOutputBuffer " + outputBufferID);
               }
               // end output part

            } // End while()

            Log.d("PLAYER","Player exited from run() loop");
            decoder.stop(); decoder.release();
           // close output file
           if (bos != null) try {bos.close(); }  catch (IOException e) {e.printStackTrace();}
            // finish player
           if (player != null) {player.stop(); player.release();}
    
           Log.d("PLAYER", "Player end of run() method");
        } // End run()

        private void init(){
            MediaFormat format = soundMedia.getTrackFormat(trackID);
            String mime = format.getString(MediaFormat.KEY_MIME);
            // Log.d("PLAYER_THREAD", "init()");

            if (saveFile != null) {
                File outputFile = new File(saveFile);
                try {
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    bos = new BufferedOutputStream(fos);
               } catch (IOException e) {e.printStackTrace();}
            }
            if (play) {
               int bufSize = android.media.AudioTrack.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

                player = new AudioTrack(AudioManager.STREAM_MUSIC,
                        44100, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, bufSize,
                        AudioTrack.MODE_STREAM);
                player.play();

                Log.d("playerThread","new player");
            }

            try {
                decoder = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {e.printStackTrace();}

            decoder.configure(format,null,null,0);
            // MediaFormat outputFormat = decoder.getOutputFormat();
        }
}
