package epfl.esl.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener {

    public static final String RECEIVE_HEART_RATE = "RECEIVE_HEART_RATE";
    public static final String RECEIVE_MOTION = "RECEIVE_MOTION";
    public static final String RECEIVE_HEART_RATE_LOCATION = "RECEIVE_HEART_RATE_LOCATION";
    public static final String HEART_RATE = "HEART_RATE";
    public static final String ACCEL = "ACCEL";
    public static final String GYRO = "GYRO";

    TextView recBtn, stopBtn, hrTxt, accTxt, gyroTxt;
    private HeartRateBroadcastReceiver heartRateBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recBtn = (TextView) findViewById(R.id.rec_btn);
        stopBtn = (TextView) findViewById(R.id.stop_btn);
        hrTxt = findViewById(R.id.hr_value);
        accTxt = findViewById(R.id.acc_value);
        gyroTxt = findViewById(R.id.gyro_value);


        // Intent to the Brainactivity
        // Comment these lines to stay in this activity
//        Intent intent = new Intent(this, BrainActivity.class);
//        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();
//        heartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
//        LocalBroadcastManager.getInstance(this).registerReceiver(heartRateBroadcastReceiver, new
//                IntentFilter(RECEIVE_MOTION));

        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.v("MainActivity", "data received: ");
        for (DataEvent event : dataEventBuffer){
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Uri uri = event.getDataItem().getUri();
                assert uri.getPath() != null;
                switch (uri.getPath()) {
                    case BuildConfig.W_motion_path:
                        float[] motion = dataMapItem.getDataMap().getFloatArray(BuildConfig.W_motion_key);
                        int hr = dataMapItem.getDataMap().getInt(BuildConfig.W_heart_rate_key);
                        hrTxt.setText(Integer.toString(hr));
                        String accString = motion[0] + "\n" + motion[1] + "\n" + motion[2];
                        String gyroString = motion[3] + "\n" + motion[4] + "\n" + motion[5];
                        accTxt.setText(accString);
                        gyroTxt.setText(gyroString);
                        break;
                }
            }


        }
    }


    private class HeartRateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Show HR in a TextView
            int heartRateWatch = intent.getIntExtra(HEART_RATE, -1);
//            hrTxt.setText(String.valueOf(heartRateWatch));
        }
    }



    }