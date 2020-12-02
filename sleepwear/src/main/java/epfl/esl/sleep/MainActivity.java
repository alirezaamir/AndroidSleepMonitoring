package epfl.esl.sleep;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView accText, gyroText;
    private TextView recBtn, stopBtn;
    private SensorManager sensorManager;
    private Sensor acc_sensor, gyro_sensor;
    final private String TAG = MainActivity.class.getSimpleName();
    ArrayList<Float> accArray = new ArrayList<>();
    ArrayList<Float> gyroArray = new ArrayList<>();
    private boolean recording = false;
    private static final int SENDING_PERIOD= 2000;
    private float[] acc_gyr_values = {0, 0, 0, 0, 0, 0};
    Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK
                        | PowerManager.ON_AFTER_RELEASE,
                TAG);

        accText = (TextView) findViewById(R.id.acc);
//        gyroText = (TextView) findViewById(R.id.gyro);
        recBtn = (TextView) findViewById(R.id.rec_btn);
        stopBtn = (TextView) findViewById(R.id.stp_btn);

//        wl.acquire();

        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recButtonClicked(wl);
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopButtonClicked(wl);
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

        sensorManager = (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
        acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        handler.post(runnableSendData);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float acc0 = (float) event.values[0];
            float acc1 = (float) event.values[1];
            float acc2 = (float) event.values[2];

            double normAcc = Math.sqrt(Math.pow(acc0, 2) + Math.pow(acc1, 2) + Math.pow(acc2, 2));
            float ang0 = (float) Math.toDegrees(Math.acos(acc0 / normAcc));
            float ang1 = (float) Math.toDegrees(Math.acos(acc1 / normAcc));
            float ang2 = (float) Math.toDegrees(Math.acos(acc2 / normAcc));

            accText.setText(acc0 + "\n" + acc1 + "\n" + acc2 + "\n" +
                    ang0 + "\n" + ang1 + "\n" + ang2);
            if (recording) {
                accArray.add(acc0);
                accArray.add(acc1);
                accArray.add(acc2);
            }

            acc_gyr_values[0] = acc0;
            acc_gyr_values[1] = acc1;
            acc_gyr_values[2] = acc2;

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float gyro0 = (float) event.values[0];
            float gyro1 = (float) event.values[1];
            float gyro2 = (float) event.values[2];

//            gyroText.setText(gyro0 + "\n" + gyro1 + "\n" + gyro2);

            if (recording) {
                gyroArray.add(gyro0);
                gyroArray.add(gyro1);
                gyroArray.add(gyro2);
            }

            acc_gyr_values[3] = gyro0;
            acc_gyr_values[4] = gyro1;
            acc_gyr_values[5] = gyro2;
        } else {
            Log.d(TAG, "Unrecognized type: " + event.sensor.getType());
        }

    }

    private void sendDataMap() {
        final float[] values = acc_gyr_values;
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_motion_path);
        putDataMapRequest.getDataMap().putFloatArray(BuildConfig.W_motion_key, values);
        putDataMapRequest.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataReq);

        putDataTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Log.v(TAG, "Task completed!");
            }
        });
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

    private Runnable runnableSendData = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Runnable is called!");
            if (acc_gyr_values != null)
                sendDataMap();

            // Repeat the task
            handler.postDelayed(this, SENDING_PERIOD);
        }
    };

    private void recButtonClicked(PowerManager.WakeLock wl) {
        recBtn.setVisibility(View.INVISIBLE);
        stopBtn.setVisibility(View.VISIBLE);
        recording = true;
        wl.acquire();
    }

    private void stopButtonClicked(PowerManager.WakeLock wl) {
        recBtn.setVisibility(View.VISIBLE);
        stopBtn.setVisibility(View.INVISIBLE);
        recording = false;

        writeToFile(MainActivity.this, accArray, gyroArray);
        wl.release();
    }

    private void writeToFile(Context context, ArrayList<Float> accArray, ArrayList<Float> gyroArray) {
        // Create File to save the data
        File path = context.getExternalFilesDir(null);
        File file_acc = new File(path, "acc_data.txt");
        File file_gyro = new File(path, "gyro_data.txt");

        try {
            FileOutputStream fos_acc = new FileOutputStream(file_acc, false);
            for (int i = 0; i < accArray.size(); i++) {
                fos_acc.write(Float.toString(accArray.get(i)).getBytes());
                if ((i % 3) == 0) {
                    fos_acc.write("\n".getBytes());
                } else {
                    fos_acc.write(", ".getBytes());
                }
            }
            FileOutputStream fos_gyro = new FileOutputStream(file_gyro, false);
            for (int i = 0; i < gyroArray.size(); i++) {
                fos_gyro.write(Float.toString(gyroArray.get(i)).getBytes());
                if ((i % 3) == 0) {
                    fos_gyro.write("\n".getBytes());
                } else {
                    fos_gyro.write(", ".getBytes());
                }
            }
            fos_acc.write("\n".getBytes());
            fos_acc.flush();
            fos_acc.close();
            fos_gyro.write("\n".getBytes());
            fos_gyro.flush();
            fos_gyro.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
