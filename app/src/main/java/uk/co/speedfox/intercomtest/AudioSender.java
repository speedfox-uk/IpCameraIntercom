package uk.co.speedfox.intercomtest;

import android.annotation.SuppressLint;
import android.media.*;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Takes a stream of audio from the mic and writes it to an output stream.
 */
public class AudioSender implements AudioSendChannelListener{

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);


    AudioRecord audioRecord;
    volatile boolean running = false;
    Thread sendingThread;
    G711UCodec codec;
    long sentCount;

    @SuppressLint("MissingPermission")
    public AudioSender() throws IOException {
        
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, BUFFER_SIZE);
        codec = new G711UCodec();
    }


    @Override
    public synchronized void audioChannelOpen(OutputStream os) {
        sendingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                audioRecord.startRecording();
                try{
                    while (running) {
                        byte[] rawBuff = new byte[BUFFER_SIZE];
                        int numRead = audioRecord.read(rawBuff, 0, rawBuff.length);
                        byte[] g711 = new byte[numRead/2];
                        int i = 0, j=0;
                        while (i < numRead) {
                            os.write(rawBuff[i]);
                            i++;
                        }
                    }
                }
                catch (IOException e) {

                }
                try {
                    os.close();
                } catch (IOException e) {
                    //Nothing to do here.
                }
                audioRecord.stop();
                running = false;
            }

        });

        sentCount = 0;
        running = true;
        sendingThread.start();
    }

    @Override
    public synchronized void audioChannelClosing() {
        if(running){
            running = false;
            try {
                sendingThread.join();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(getClass().getSimpleName(), "Audio sending finished. Sent count: " + sentCount);
        }

    }
}
