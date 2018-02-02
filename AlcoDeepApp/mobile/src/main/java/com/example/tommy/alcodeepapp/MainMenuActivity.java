package com.example.tommy.alcodeepapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.converters.CSVSaver;

public class MainMenuActivity extends AppCompatActivity implements
        SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private Intent serviceIntent;

    int hasPermission = 0;


    /*variables for use when using MessageAPI*/
    private GoogleApiClient mGoogleApiClient;
    private String DATA_RECEIVER_CAPABILITY = "data_receiver";
    private String DATA_RECEIVER_PATH = "/data_receiver";

    /*variables for sensors*/
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    boolean serviceStarted = false;

    /*variables for writing phone and watch data to file*/
    private FileWriter writer;
    private File csvOutput;
    private File watchOutputFile;
    ArrayList<Attribute> attributes;
    Instances mDataset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        //set sensors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        //Build a new GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    hasPermission);
        }

        //create an Instance which will hold all the data from the watch
        attributes = new ArrayList<>();
        attributes.add(new Attribute("dt"));
        attributes.add(new Attribute("wax"));
        attributes.add(new Attribute("way"));
        attributes.add(new Attribute("waz"));
        attributes.add(new Attribute("wgx"));
        attributes.add(new Attribute("wgy"));
        attributes.add(new Attribute("wgz"));

        mDataset = new Instances("mqp_features", attributes, 10000);
        mDataset.setClassIndex(mDataset.numAttributes() - 1);

        disableStopButton();
        serviceStarted = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope,
                SensorManager.SENSOR_DELAY_FASTEST);

        try {
            if (serviceStarted) {
                writer = new FileWriter(csvOutput.getAbsolutePath(),true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.wtf("app stopped", "app stopped");
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if(writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy(){
        stopService(serviceIntent);
        super.onDestroy();
    }

    public void pressStartButton(View v) {
        //disable start button and enable stop button
        disableStartButton();
        enableStopButton();

        //make sure Instance holding watch data is initially empty
        mDataset.delete();
        mDataset = new Instances("mqp_features", attributes, 10000);
        mDataset.setClassIndex(mDataset.numAttributes() - 1);

        //start service to listen to messages from the watch
        serviceIntent = new Intent(this, MobileListenerService.class);
        startService(serviceIntent);

        //start main activity on watch
        startWatch();

        //open file for writing phone data to
        csvOutput = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), System.currentTimeMillis() + "phone.csv");
        try {
            writer = new FileWriter(csvOutput.getAbsolutePath(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //update status
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Recording...");
        serviceStarted = true;
    }

    public void pressStopButton(View v) {
        //disable stop button and enable start button
        disableStopButton();
        enableStartButton();

        //stop service to listen to messages from the watch
        serviceIntent = new Intent(this, MobileListenerService.class);
        stopService(serviceIntent);

        //update status
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Stopped...");
        serviceStarted = false;

        //stop main activity on watch
        stopWatch();

        //save data from watch to csv file
        saveWatchToFile();

        //send phone and watch csv's to server
        sendFilesToServer();

        //go to result activity
        Intent intent = new Intent(this, ResultActivity.class);
        startActivity(intent);
    }

    private void enableStartButton() {
        Button btn = (Button) findViewById(R.id.startButton);
        btn.setEnabled(true);
    }

    private void enableStopButton() {
        Button btn = (Button) findViewById(R.id.stopButton);
        btn.setEnabled(true);
    }

    private void disableStartButton() {
        Button btn = (Button) findViewById(R.id.startButton);
        btn.setEnabled(false);
    }

    private void disableStopButton() {
        Button btn = (Button) findViewById(R.id.stopButton);
        btn.setEnabled(false);
    }

    private void startWatch() {
        sendStringToWatch("start");
    }

    private void stopWatch() {
        sendStringToWatch("stop");
    }

    private void sendStringToWatch(String str) {
        new SendToWatch(DATA_RECEIVER_PATH, str).start();
    }

    private void sendFilesToServer() {
        Uri file1Uri = Uri.parse(csvOutput.getAbsolutePath()); // get uri to phone csv
        Uri file2Uri = Uri.parse(watchOutputFile.getAbsolutePath()); // get uri to watch csv

        // create upload service client
        FileUploadService service =
                ServiceGenerator.createService(FileUploadService.class);

        // create part for file
        MultipartBody.Part body1 = prepareFilePart("phonecsv", file1Uri);
        MultipartBody.Part body2 = prepareFilePart("watchcsv", file2Uri);

        // add another part within the multipart request
        RequestBody description = createPartFromString("contains watch and phone csv files");

        // finally, execute the request
        Call<ResponseBody> call = service.uploadMultipleFiles(description, body1, body2);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {
                Log.v("Upload", "success");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Upload error:", t.getMessage());
            }
        });
    }

    protected void saveWatchToFile() {
        watchOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), System.currentTimeMillis() + "watch.csv");
        if (mDataset != null) {
            CSVSaver saver = new CSVSaver();
            saver.setInstances(mDataset);


            Log.wtf("MQP", "FILE " + watchOutputFile.getAbsolutePath());
            try {
                saver.setFile(watchOutputFile);
                saver.writeBatch();

                Intent intent =
                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(watchOutputFile));
                sendBroadcast(intent);
            } catch (IOException e) {
                Log.wtf("MQP", "error saving");
                e.printStackTrace();
            }

        } else {
            Log.wtf("MQP", "Dataset NULL");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!serviceStarted) return;
        else {
            if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)  {
                try {
                    float max = sensorEvent.values[0];
                    float may = sensorEvent.values[1];
                    float maz = sensorEvent.values[2];
                    long t = sensorEvent.timestamp;
                    writer.write("acc,"+t+','+max+','+may+','+maz+','+'\n');

                    Log.wtf("PhoneAccel", max + " " + may + " " + maz);
                } catch (IOException e) {
                    Log.wtf("PHONE",e);
                }
            }
            else if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                try {
                    float mgx = sensorEvent.values[0];
                    float mgy = sensorEvent.values[1];
                    float mgz = sensorEvent.values[2];
                    long t = sensorEvent.timestamp;
                    writer.write("gyr,"+t+','+mgx+','+mgy+','+mgz+','+'\n');


                    Log.wtf("PhoneGyro", mgx + " " + mgy + " " + mgz);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //nothing
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //nothing
    }

    @Override
    public void onConnectionSuspended(int i) {
        //nothing
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //nothing
    }

    class SendToWatch extends Thread {
        String path;
        String data;

        SendToWatch(String path, String data) {
            this.path = path;
            this.data = data;
        }

        @Override
        public void run() {
            String DATA_RECEIVER_CAPABILITY = "data_receiver";

            //retrieve watch node
            CapabilityApi.GetCapabilityResult result =
                    Wearable.CapabilityApi.getCapability(
                            mGoogleApiClient,  DATA_RECEIVER_CAPABILITY,
                            CapabilityApi.FILTER_REACHABLE).await();

            if(result.getCapability().getNodes().isEmpty()) {
                Log.wtf("MQP", "no watch found");
                return;
            }

            Log.wtf("MQP", "sending command to watch");

            String watchNodeId = result.getCapability().getNodes().iterator().next().getId();

            //send message to watch
            Wearable.MessageApi.sendMessage(mGoogleApiClient, watchNodeId,
                    DATA_RECEIVER_PATH, data.getBytes());
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra("datamap");
            if (data.getString("type").equals("status")) {
                //
            } else if (data.getString("type").equals("data")) {

                // Log the data
                long[] dt = data.getLongArray("dt");
                float[] wax = data.getFloatArray("wax");
                float[] way = data.getFloatArray("way");
                float[] waz = data.getFloatArray("waz");
                float[] wgx = data.getFloatArray("wgx");
                float[] wgy = data.getFloatArray("wgy");
                float[] wgz = data.getFloatArray("wgz");

                if (serviceStarted) {
                    if (attributes == null) {
                        Log.wtf("MQP", "attributes null");
                        return;
                    }

                    for (int i = 0; i < wax.length; i++) {
                        Instance inst = new DenseInstance(attributes.size());
                        inst.setDataset(mDataset);
                        inst.setValue(mDataset.attribute("dt"), dt[i]);
                        inst.setValue(mDataset.attribute("wax"), wax[i]);
                        inst.setValue(mDataset.attribute("way"), way[i]);
                        inst.setValue(mDataset.attribute("waz"), waz[i]);
                        inst.setValue(mDataset.attribute("wgx"), wgx[i]);
                        inst.setValue(mDataset.attribute("wgy"), wgy[i]);
                        inst.setValue(mDataset.attribute("wgz"), wgz[i]);
                        Log.wtf("WATCH", Float.toString(wax[i]));
                        mDataset.add(inst);
                    }
                }
            }
        }
    }

    @NonNull
    private RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(
                okhttp3.MultipartBody.FORM, descriptionString);
    }

    @NonNull
    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
        // use the FileUtils to get the actual file by uri
        File file;
        if(partName.equals("phonecsv")) {
            file = csvOutput;
        }
        else {
            file = watchOutputFile;
        }

        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(getContentResolver().getType(fileUri)),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }
}
