package epfl.esl.sleep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String RECEIVE_HEART_RATE = "RECEIVE_HEART_RATE";
    public static final String RECEIVE_LOCATION = "RECEIVE_LOCATION";
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
    }


    @Override
    protected void onResume() {
        super.onResume();
        heartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(heartRateBroadcastReceiver, new
                IntentFilter(RECEIVE_HEART_RATE));
    }


    private class HeartRateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Show HR in a TextView
            int heartRateWatch = intent.getIntExtra(HEART_RATE, -1);
            hrTxt.setText(String.valueOf(heartRateWatch));
        }
    }

}