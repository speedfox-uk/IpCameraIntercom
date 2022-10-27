package uk.co.speedfox.intercomtest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Take a stream of audio data and send it to the peaker
 */
public class AudioReceiver {

    private volatile Thread receiveThread = null;
    private boolean running = false;
    private AudioTrack audioPlayer;
    private G711UCodec codec = new G711UCodec();

    public synchronized void audioChannelOpen(InputStream is)
    {
        //This is all hardcoded to match the settings I have on my camera. If your camera is different you will need to change this.
        int bufInc = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioPlayer = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufInc * 2, AudioTrack.MODE_STREAM);
        Log.d(AudioReceiver.this.getClass().getSimpleName(), "STATE: " + audioPlayer.getState() + " " + audioPlayer.getPlayState());
        audioPlayer.play();

        if(running)
            return;

        Runnable receiveTask = new Runnable() {
            @Override
            public void run() {
                byte[] buf = new byte[1024];
                byte[] normBuf = new byte[1024];
                int prevMax = 6000;
                long bytesRead = 0;
                try {
                    Log.d(AudioReceiver.this.getClass().getSimpleName(),  "Started receiving audio" + running);
                    while (running){
                        int read = is.read(buf);
                        bytesRead +=read;
                        prevMax = normalise(buf, normBuf, read, prevMax);
                        audioPlayer.write(normBuf, 0, read);
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                }
                running = false;
                audioPlayer.flush();
                audioPlayer.stop();
                audioPlayer.release();
                Log.d(AudioReceiver.this.getClass().getSimpleName(),  "Ending receiving. Max vol:" + prevMax + " bytes:" + bytesRead);
            }
        };

        receiveThread = new Thread(receiveTask);
        running = true;
        receiveThread.start();
    }

    private int normalise(byte[] buf, byte[] normBuf, int read, int prevMax) {
        short[] pcmVals = new short[(read/2)];
        ByteBuffer shortsIn = ByteBuffer.wrap(buf);
        for(int i=0; i<pcmVals.length; i++)
        {
            short thisShort = (short)((buf[(2*i)+1] << 8) | (buf[2*i]));
            prevMax = (thisShort > prevMax)? thisShort:prevMax;
            pcmVals[i] = thisShort;
        }


        for(int i=0; i<pcmVals.length; i++)
        {
            short thisShort = (short)((pcmVals[i] * Short.MAX_VALUE) / prevMax);
            normBuf[(i*2)+1] = (byte)((thisShort >> 8) &0xFF);
            normBuf[i*2] = (byte)(thisShort &0xFF);;
        }

        return prevMax;
    }

    public synchronized void stop(){
        if(!running)
            return;
        running = false;
        try {
            receiveThread.join();
        } catch (InterruptedException e) {

        }
    }
}
