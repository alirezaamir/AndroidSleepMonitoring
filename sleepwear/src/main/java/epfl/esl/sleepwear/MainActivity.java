package epfl.esl.sleepwear;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView accText, gyroText;
    private SensorManager sensorManager;
    private Sensor acc_sensor, gyro_sensor;
    final private String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accText = (TextView) findViewById(R.id.acc);
        gyroText = (TextView) findViewById(R.id.gyro);

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
            double acc0 = (double) event.values[0];
            double acc1 = (double) event.values[1];
            double acc2 = (double) event.values[2];

            accText.setText(acc0 + ", " + acc1 + ", " + acc2);

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            double gyro0 = (double) event.values[0];
            double gyro1 = (double) event.values[1];
            double gyro2 = (double) event.values[2];

            gyroText.setText(gyro0 + "\n" + gyro1 + "\n" + gyro2);
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
}
