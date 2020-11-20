package epfl.esl.sleepwear;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView accText, gyroText;
    private TextView recBtn, stopBtn;
    private SensorManager sensorManager;
    private Sensor acc_sensor, gyro_sensor;
    final private String TAG = MainActivity.class.getSimpleName();
    ArrayList<Float> accArray = new ArrayList<>();
    ArrayList<Float> gyroArray = new ArrayList<>();
    private boolean recording = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accText = (TextView) findViewById(R.id.acc);
        gyroText = (TextView) findViewById(R.id.gyro);
        recBtn = (TextView) findViewById(R.id.rec_btn);
        stopBtn = (TextView) findViewById(R.id.stp_btn);

        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recButtonClicked();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopButtonClicked();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission("android" + ""
                + ".permission.BODY_SENSORS") == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{"android.permission.BODY_SENSORS"}, 0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission("android" + ""
                + ".permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") ==
                        PackageManager.PERMISSION_DENIED || checkSelfPermission("android" + "" +
                ".permission.INTERNET") == PackageManager.PERMISSION_DENIED)) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android"
                    + ".permission.ACCESS_COARSE_LOCATION", "android.permission.INTERNET"}, 0);
        }

        sensorManager = (SensorManager) getSystemService(MainActivity
                .SENSOR_SERVICE);
        acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float acc0 = (float) event.values[0];
            float acc1 = (float) event.values[1];
            float acc2 = (float) event.values[2];

            accText.setText(acc0 + "\n" + acc1 + "\n" + acc2);
            if (recording) {
                accArray.add(acc0);
                accArray.add(acc1);
                accArray.add(acc2);
            }

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            float gyro0 = (float) event.values[0];
            float gyro1 = (float) event.values[1];
            float gyro2 = (float) event.values[2];

            gyroText.setText(gyro0 + "\n" + gyro1 + "\n" + gyro2);

            if (recording){
                gyroArray.add(gyro0);
                gyroArray.add(gyro1);
                gyroArray.add(gyro2);
            }
        }else{
            Log.d(TAG, "Unrecognized type: " + event.sensor.getType());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyro_sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void recButtonClicked(){
        recBtn.setVisibility(View.INVISIBLE);
        stopBtn.setVisibility(View.VISIBLE);
        recording = true;
    }

    private void stopButtonClicked(){
        recBtn.setVisibility(View.VISIBLE);
        stopBtn.setVisibility(View.INVISIBLE);
        recording = false;

        writeToFile(MainActivity.this, accArray, gyroArray);

    }

    private void writeToFile(Context context, ArrayList<Float> accArray, ArrayList<Float> gyroArray){
        // Create File to save the data
        File path = context.getExternalFilesDir(null);
        File file = new File(path, "IMU.txt");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            for (int i = 0; i < accArray.size(); i++){
                fos.write(Float.toString(accArray.get(i)).getBytes());
            }
            fos.write("GYRO".getBytes());
            for (int i = 0; i < gyroArray.size(); i++){
                fos.write(Float.toString(gyroArray.get(i)).getBytes());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
