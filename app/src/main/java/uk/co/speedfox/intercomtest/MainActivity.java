package uk.co.speedfox.intercomtest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.alexvas.rtsp.widget.RtspSurfaceView;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

//Notes on how to disable the lock screen here: https://www.androidcentral.com/how-disable-lock-screen-android
public class MainActivity extends AppCompatActivity {

    private ISAPIHandler handler;
    private View sendAudioButton, recieveAudioButton, taklButton;
    private BatteryManager bm;
    private RtspSurfaceView screen;
    private BroadcastReceiver batteryReceiver;
    private TextView batteryIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendAudioButton = findViewById(R.id.sendAudioButton);
        sendAudioButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d(MainActivity.class.getName(), "GOT TOUCH");
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    startAudio(true, false);
                }
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    stopAudio();
                }
                return true;
            }
        });

        recieveAudioButton = findViewById(R.id.recieveAudioButton);
        recieveAudioButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d(MainActivity.class.getName(), "GOT TOUCH");
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    startAudio(false, true);
                }
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    stopAudio();
                }
                return true;
            }
        });


        taklButton = findViewById(R.id.talkButton);
        taklButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d(MainActivity.class.getName(), "GOT TOUCH");
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                {
                    startAudio(true, true);
                }
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    stopAudio();
                }
                return true;
            }
        });

        try {
            handler= new ISAPIHandler(getString(R.string.camera_host), getString(R.string.username), getString(R.string.password));
        } catch (ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1234);
        }


        batteryIndicator = findViewById(R.id.batteryIndicator);
        bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        screen = findViewById(R.id.screen);
        screen.init(Uri.parse("rtsp://" + getString(R.string.camera_host)), getString(R.string.username), getString(R.string.password));

        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryState();
            }
        };
        registerReceiver(batteryReceiver, batteryFilter);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        screen.start(true, false);
        updateBatteryState();
    }

    private void updateBatteryState() {
        String isCharging = bm.isCharging() ? "" +Character.toChars(0x26a1)[0] : new String(new int[]{0x1F50B}, 0, 1);
        String batLevel = "" + bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "% " + isCharging;
        batteryIndicator.setText(batLevel);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        screen.stop();
    }

    public void gotoSettings(View v)
    {
        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
    }

    public void startAudio(boolean sendAudio, boolean receiveAudio)
    {
        Log.d(MainActivity.class.getName(), "Starting audio");
        try {
            handler.startAudio(sendAudio, receiveAudio);
        }
        catch (IOException e)
        {
            Log.d(MainActivity.class.getName(), "Failed to start audio " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopAudio()
    {
        Log.d(MainActivity.class.getName(), "Stopping audio");
        try {
            handler.stopAudioOutput("1"); //Hack. This needs to be passed back somehow
        } catch (ISAPIException e) {
            Log.d(MainActivity.class.getName(), "Failed to stop audio " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1234: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Log.d("TAG", "permission denied by user");
                }
                return;
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}