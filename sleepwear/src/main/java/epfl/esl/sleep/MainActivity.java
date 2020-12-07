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
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView accText, gyroText;
    private TextView recBtn, stopBtn;
    private SensorManager sensorManager;
    private Sensor acc_sensor, gyro_sensor, hr_sensor;
    final private String TAG = MainActivity.class.getSimpleName();
    ArrayList<Float> accArray = new ArrayList<>();
    ArrayList<Float> gyroArray = new ArrayList<>();
    private int acc_buff_idx = 0;
    private int acc_buff_size = 128;
    private float[][] acc_buff = new float[3][acc_buff_size];
    private boolean recording = false;
    private static final int SENDING_PERIOD= 2000;
    private float[] accGyrValues = {0, 0, 0, 0, 0, 0};
    private int hrValues = 0;
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
        hr_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        handler.post(runnableSendData);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float acc0 = (float) event.values[0];
            float acc1 = (float) event.values[1];
            float acc2 = (float) -event.values[2]; // z axis upward

            double normAcc = Math.sqrt(Math.pow(acc0, 2) + Math.pow(acc1, 2) + Math.pow(acc2, 2));
            float ang0 = (float) Math.toDegrees(Math.acos(acc0 / normAcc));
            float ang1 = (float) Math.toDegrees(Math.acos(acc1 / normAcc));
            float ang2 = (float) Math.toDegrees(Math.acos(acc2 / normAcc));

            // Method 1
//            // positions ref angles 12x3
//            int npos = 12;
//            float mn_sq_err = 0;
//            float[][] pos = {{90, 90, 180},  // sit
//                             {180, 90, 90},  // back
//                             {110, 20, 90},  // back
//                             {110, 160, 90}, // back
//                             {110, 30, 110}, // left
//                             {90, 30, 120},  // left
//                             {110, 150, 110},// right
//                             {90, 150, 120}, // right
//                             {90, 140, 125}, // front
//                             {90, 40, 125},  // front
//                             {90, 160, 110}, // front
//                             {90, 20, 110}}; // front
//
//            int pos_idx = 0;
//            float min_err = ((float) (Math.pow(pos[0][0]-ang0, 2) + Math.pow(pos[0][1]-ang1, 2) + Math.pow(pos[0][2]-ang2, 2))) / 3;
//            for (int i=1; i<npos; i++)
//            {
//                mn_sq_err = ((float) (Math.pow(pos[i][0]-ang0, 2) + Math.pow(pos[i][1]-ang1, 2) + Math.pow(pos[i][2]-ang2, 2))) / 3;
//                if (mn_sq_err < min_err)
//                    pos_idx = i;
//            }
//            String[][] positions = {{"moving"}, {"sit"}, {"back"}, {"left"}, {"right"}, {"front"}};
//            String pos_est = "sit";
//            int posEst_int = 0;
//
//            if (pos_idx==1 || pos_idx==2) {
//                pos_est = "back";
//                posEst_int = 1;
//            } else if (pos_idx==3 || pos_idx==4) {
//                pos_est = "left";
//                posEst_int = 2;
//            } else if (pos_idx==6 || pos_idx==7) {
//                pos_est = "right";
//                posEst_int = 3;
//            } else if (pos_idx==8 || pos_idx==9 || pos_idx==10 || pos_idx==11) {
//                pos_est = "front";
//                posEst_int = 4;
//            }

            // Method 2
            String pos_est;
            int posEst_int = 0;
            int moving_f = 0;
            float[] sum = {0, 0, 0};
            float[] mean = {0, 0, 0};

            acc_buff[0][acc_buff_idx] = acc0;
            acc_buff[1][acc_buff_idx] = acc1;
            acc_buff[2][acc_buff_idx] = acc2;
            acc_buff_idx++;
            acc_buff_idx = acc_buff_idx%acc_buff_size;

            for(int i=0;i<acc_buff_size;i++)
	        {
		        sum[0]=sum[0]+acc_buff[0][i];
                sum[1]=sum[1]+acc_buff[1][i];
                sum[2]=sum[2]+acc_buff[2][i];
	        }
	        mean[0]=sum[0]/acc_buff_size;
            mean[1]=sum[1]/acc_buff_size;
            mean[2]=sum[2]/acc_buff_size;

            sum[0] = 0;
            sum[1] = 0;
            sum[2] = 0;

            for(int i=0;i<acc_buff_size;i++)
            {
                sum[1]+=Math.pow((acc_buff[1][i]-mean[1]),2);
            }
            mean[1]=sum[1]/(acc_buff_size-1);
            double deviation = Math.sqrt(mean[1]);

            if (deviation > 0.8) {
                moving_f = 1;
            }

            if (moving_f == 0)
            {
                double normAcc2 = Math.sqrt(Math.pow(acc0, 2) + Math.pow(acc1, 2) + Math.pow(acc2, 2));
                float angX = (float) Math.toDegrees(Math.acos(acc0 / normAcc2));
                float angY = (float) Math.toDegrees(Math.acos(acc1 / normAcc2));
                float angZ = (float) Math.toDegrees(Math.acos(acc2 / normAcc2));

                if (angZ > 150) {
                    posEst_int = 1; // sit
                } else if (angX > 140) {
                    posEst_int = 2; // back
                } else if (angY < 50 || angY > 120) {
                    if (angZ < 105) {
                        posEst_int = 2; // back
                    } else if (angX > 100 || (angZ-angX) < 20) {
                        if (angY < 90) {
                            posEst_int = 3; // left
                        } else {
                            posEst_int = 4; // right
                        }
                    } else if (angX < 100 && angZ > 100) {
                        posEst_int = 5; // front
                    } else {
                        posEst_int = 6; // unknown
                    }
                } else {
                    posEst_int = 6; // unknown
                }
            } else {
                posEst_int = 0; // moving
            }

            if (posEst_int==0) {
                pos_est = "moving";
            } else if (posEst_int==1) {
                pos_est = "sit";
            } else if (posEst_int==2) {
                pos_est = "back";
            } else if (posEst_int==3) {
                pos_est = "left";
            } else if (posEst_int==4) {
                pos_est = "right";
            } else if (posEst_int==5) {
                pos_est = "front";
            } else {
                pos_est = "unknown";
            }

                    accText.setText(acc0 + "\n" + acc1 + "\n" + acc2 + "\n" +
                            ang0 + "\n" + ang1 + "\n" + ang2 + "\n" + pos_est);

            if (recording) {
                accArray.add(acc0);
                accArray.add(acc1);
                accArray.add(acc2);
            }

            accGyrValues[0] = acc0;
            accGyrValues[1] = acc1;
            accGyrValues[2] = acc2;
            accGyrValues[3] = (float) posEst_int;
            accGyrValues[4] = 0;
            accGyrValues[5] = 0;

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

//            accGyrValues[3] = gyro0;
//            accGyrValues[4] = gyro1;
//            accGyrValues[5] = gyro2;
        } else if(event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            hrValues = (int) event.values[0];

        }else{
            Log.d(TAG, "Unrecognized type: " + event.sensor.getType());
        }

    }

    private void sendDataMap() {
        final float[] values = accGyrValues;
        final int hrValue = hrValues;
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(BuildConfig.W_motion_path);
        putDataMapRequest.getDataMap().putFloatArray(BuildConfig.W_motion_key, values);
        putDataMapRequest.getDataMap().putInt(BuildConfig.W_heart_rate_key, hrValue);
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
        sensorManager.registerListener(this, hr_sensor, SensorManager.SENSOR_DELAY_NORMAL);
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
            if (accGyrValues != null)
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
//            fos_acc.flush();
            fos_acc.close();
            fos_gyro.write("\n".getBytes());
//            fos_gyro.flush();
            fos_gyro.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
