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
import android.view.View;
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

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

//import epfl.esl.sleep.ml.ModelDeep;

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener {

    public static final String RECEIVE_HEART_RATE = "RECEIVE_HEART_RATE";
    public static final String RECEIVE_MOTION = "RECEIVE_MOTION";
    public static final String RECEIVE_HEART_RATE_LOCATION = "RECEIVE_HEART_RATE_LOCATION";
    public static final String HEART_RATE = "HEART_RATE";
    public static final String ACCEL = "ACCEL";
    public static final String GYRO = "GYRO";

    private static final String TAG = MainActivity.class.getName();

    TextView recBtn, stopBtn, eegBtn, tcpIpBtn;
    TextView hrTxt, accTxt, posTxt;//, gyroTxt;
    private HeartRateBroadcastReceiver heartRateBroadcastReceiver;

//    // TCP/IP client
//    private CClient mClient;
    private int tcp_cnt = 0;

    private PrintWriter output;
    private BufferedReader input;

    String SERVER_IP = "192.168.43.24";
    int SERVER_PORT = 5500;
    Thread Thread1 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recBtn = (TextView) findViewById(R.id.rec_btn);
        stopBtn = (TextView) findViewById(R.id.stop_btn);
        tcpIpBtn = (TextView) findViewById(R.id.tcp_ip_btn);
        eegBtn = (TextView) findViewById(R.id.eeg_btn);
        hrTxt = findViewById(R.id.hr_value);
        accTxt = findViewById(R.id.acc_value);
        posTxt = findViewById(R.id.pos_value);
//        gyroTxt = findViewById(R.id.gyro_value);

        eegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BrainActivity.class);
                startActivity(intent);
            }
        });

        // Method 1
//        mClient = new CClient();
//        Thread myThready = new Thread(mClient);
//        myThready.start();
//
//        tcpIpBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                proc_Login(arg0);
//            }
//        });

        // Method 2
        Thread1 = new Thread(new Thread1());
        Thread1.start();

        tcpIpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String message = "data TCP/IP: "+ tcp_cnt;
                new Thread(new Thread3(message)).start();
                tcp_cnt++;
            }
        });
    }

//    public void proc_Login(View v)
//    {
//        mClient.Send("data TCP/IP: "+ tcp_cnt);
//        tcp_cnt++;
//    }

    class Thread1 implements Runnable {
        public void run() {
            Socket socket;
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        tvMessages.setText("Connected\n");
                        Log.v(TAG, "TCP/IP connection success");
                    }
                });
                new Thread(new Thread2()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                tvMessages.append("server: " + message + "\n");
                                Log.v(TAG, "TCP/IP server: " + message + "\n");
                            }
                        });
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class Thread3 implements Runnable {
        private String message;
        Thread3(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            output.write(message);
            output.flush();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    tvMessages.append("client: " + message + "\n");
                    Log.v(TAG, "TCP/IP client: " + message + "\n");
//                    etMessage.setText("");
                }
            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
//        heartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
//        LocalBroadcastManager.getInstance(this).registerReceiver(heartRateBroadcastReceiver, new
//                IntentFilter(RECEIVE_MOTION));

        Wearable.getDataClient(this).addListener(this);
//        int stage = get_sleep_stage(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.v("MainActivity", "data received: ");

//        String[][] positions = {{"sit"}, {"back"}, {"left"}, {"right"}, {"front"}};

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Uri uri = event.getDataItem().getUri();
                assert uri.getPath() != null;
                switch (uri.getPath()) {
                    case BuildConfig.W_motion_path:
                        float[] motion = dataMapItem.getDataMap().getFloatArray(BuildConfig.W_motion_key);
                        int posEst = (int) motion[3]; //getPositionEstimation(motion);
                        int hr = dataMapItem.getDataMap().getInt(BuildConfig.W_heart_rate_key);
                        hrTxt.setText(Integer.toString(hr));

                        float acc0 = motion[0];
                        float acc1 = motion[1];
                        float acc2 = motion[2];
//
                        double normAcc = Math.sqrt(Math.pow(acc0, 2) + Math.pow(acc1, 2) + Math.pow(acc2, 2));
                        float ang0 = (float) Math.toDegrees(Math.acos(acc0 / normAcc));
                        float ang1 = (float) Math.toDegrees(Math.acos(acc1 / normAcc));
                        float ang2 = (float) Math.toDegrees(Math.acos(acc2 / normAcc));
//                        String posString = String.format("%.2f\n%.2f\n%.2f\n",ang0, ang1, ang2);
                        String pos_est;// = String.format("%d", posEst);
                        if (posEst==0) {
                            pos_est = "moving";
                        } else if (posEst==1) {
                            pos_est = "sit/standing";
                        } else if (posEst==2) {
                            pos_est = "back";
                        } else if (posEst==3) {
                            pos_est = "left";
                        } else if (posEst==4) {
                            pos_est = "right";
                        } else if (posEst==5) {
                            pos_est = "front";
                        } else {
                            pos_est = "unknown";
                        }

//                        String accString = String.format("%.2f\n%.2f\n%.2f\n",motion[0], motion[1], motion[2]) + pos_est;
                        String accString = String.format("%.2f\n%.2f\n%.2f\n",ang0, ang1, ang2) + pos_est;
//                        String gyroString = motion[3] + "\n" + motion[4] + "\n" + motion[5];
//                        String accString = pos_est;
                        accTxt.setText(accString);
                        posTxt.setText(pos_est);
//                      gyroTxt.setText(gyroString);
                        break;
                }
            }


        }
    }

    private int getPositionEstimation(float[] motionData) {
        int posEst = 0;

        float acc0 = motionData[0];
        float acc1 = motionData[1];
        float acc2 = motionData[2];

        double normAcc = Math.sqrt(Math.pow(acc0, 2) + Math.pow(acc1, 2) + Math.pow(acc2, 2));
        float ang0 = (float) Math.toDegrees(Math.acos(acc0 / normAcc));
        float ang1 = (float) Math.toDegrees(Math.acos(acc1 / normAcc));
        float ang2 = (float) Math.toDegrees(Math.acos(acc2 / normAcc));

        // positions ref angles 12x3
        int npos = 12;
        float mn_sq_err = 0;
        float[][] pos = {{90, 90, 180},  // sit
                {180, 90, 90},  // back
                {110, 20, 90},  // back
                {110, 160, 90}, // back
                {110, 30, 110}, // left
                {90, 30, 120},  // left
                {110, 150, 110},// right
                {90, 150, 120}, // right
                {90, 140, 125}, // front
                {90, 40, 125},  // front
                {90, 160, 110}, // front
                {90, 20, 110}}; // front

        int pos_idx = 0;
        float min_err = ((float) (Math.pow(pos[0][0]-ang0, 2) + Math.pow(pos[0][1]-ang1, 2) + Math.pow(pos[0][2]-ang2, 2))) / 3;
        for (int i=1; i<npos; i++)
        {
            mn_sq_err = ((float) (Math.pow(pos[i][0]-ang0, 2) + Math.pow(pos[i][1]-ang1, 2) + Math.pow(pos[i][2]-ang2, 2))) / 3;
            if (mn_sq_err < min_err)
                pos_idx = i;
        }
//        String[][] positions = {{"sit"}, {"back"}, {"left"}, {"right"}, {"front"}};
//        String pos_est = "sit";

        if (pos_idx==1 || pos_idx==2)
            posEst = 1;
        else if (pos_idx==3 || pos_idx==4)
            posEst = 2;
        else if (pos_idx==6 || pos_idx==7)
            posEst = 3;
        else if (pos_idx==8 || pos_idx==9 || pos_idx==10 || pos_idx==11)
            posEst = 4;

        return pos_idx;
    }


    private class HeartRateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Show HR in a TextView
            int heartRateWatch = intent.getIntExtra(HEART_RATE, -1);
//            hrTxt.setText(String.valueOf(heartRateWatch));
        }
    }

    private int get_sleep_stage(Context context){
        Random rand = new Random();
        ByteBuffer byteBuffer = ByteBuffer.allocate(4000);
        int[] inArray = new int[4000];
        int predictStage = 5;
        for(int i=0; i<4000; i++){
            byteBuffer.put((byte) rand.nextInt());
            inArray[i] = rand.nextInt();
        }
//        try {
//            ModelDeep model = ModelDeep.newInstance(context);
//
//            // Creates inputs for reference.
//            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 4000, 1}, DataType.FLOAT32);
//            inputFeature0.loadArray(inArray);
//
//            // Runs model inference and gets result.
//            ModelDeep.Outputs outputs = model.process(inputFeature0);
//            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
//            ByteBuffer outBuffer = outputFeature0.getBuffer();
//            Log.d(TAG, "Out Buffer" + outBuffer);
//
//
//            // Releases model resources if no longer used.
//            model.close();
//        } catch (IOException e) {
//            // TODO Handle the exception
//        }

        return predictStage;
    }


}