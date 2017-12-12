package com.example.tommy.alcodeepapp;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

public class MainMenuActivity extends AppCompatActivity implements
        SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener{
    private GoogleApiClient mGoogleApiClient;
    private String DATA_RECEIVER_CAPABILITY = "data_receiver";
    private String DATA_RECEIVER_PATH = "/data_receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        //Build a new GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        disableStopButton();
    }

    public void pressStartButton(View v) {
        //disable start button and enable stop button
        disableStartButton();
        enableStopButton();

        //update status
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Recording...");

        //start main activity on watch
        startWatch();
    }

    public void pressStopButton(View v) {
        //disable stop button and enable start button
        disableStopButton();
        enableStartButton();

        //update status
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Stopped...");

        //stop main activity on watch
        stopWatch();

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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        https://github.com/nhaarman/supertooltips
    }

    class SendToWatch extends Thread {
        String path;
        String data;

        SendToWatch(String p, String d) {
            path = p;
            data = d;
        }

        public void run() {
            //retrieve watch node
            CapabilityApi.GetCapabilityResult result =
                    Wearable.CapabilityApi.getCapability(
                            mGoogleApiClient,  DATA_RECEIVER_CAPABILITY,
                            CapabilityApi.FILTER_REACHABLE).await();

            if(result.getCapability().getNodes().isEmpty())
                return;

            String watchNodeId = result.getCapability().getNodes().iterator().next().getId();

            //send message to watch, telling it to start the recording process
            Wearable.MessageApi.sendMessage(mGoogleApiClient, watchNodeId,
                    DATA_RECEIVER_PATH, data.getBytes());
        }
    }
}
