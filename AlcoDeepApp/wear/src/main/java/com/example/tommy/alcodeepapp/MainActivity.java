package com.example.tommy.alcodeepapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener{

    private BoxInsetLayout mContainerView;
    private TextView mLogoView;
    private TextView mStatusView;
    private TextView mClockView;

    private GoogleApiClient mGoogleApiClient;

    private String DATA_RECEIVER_PATH = "/data_receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mLogoView = (TextView) findViewById(R.id.text);
        mStatusView = (TextView) findViewById(R.id.status);
        mClockView = (TextView) findViewById(R.id.clock);

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SENDTO);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        //create new GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //set up listener for MessageApi
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        startService(new Intent(this, WatchSensorService.class));
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
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        mContainerView.setBackground(null);
        mLogoView.setTextColor(Color.BLACK);
        mStatusView.setTextColor(Color.BLACK);
        mClockView.setVisibility(View.GONE);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.MessageApi.addListener( mGoogleApiClient, this );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.wtf("MQP", "we got a message");
        if(messageEvent.getPath().equals(DATA_RECEIVER_PATH)) {
            String data = new String(messageEvent.getData());
            Log.wtf("MQP", data);
            switch(data) {
                case "start":
                    mStatusView.setText("Recording...");
                    //startService(new Intent(this, WatchSensorService.class));
                    break;
                case "stop":
                    mStatusView.setText("Idle...");
                    //stopService(new Intent(this, WatchSensorService.class));
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 500, 50, 300};
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    break;
                default:
                    break;
            }
        }
    }

    public void sendDataToPhone(Bundle data) {
        String MOBILE_DATA_PATH = "/mobile_data";

        new SendToPhone(MOBILE_DATA_PATH, data).start();
    }


    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra("datamap");
            Log.wtf("MQP", "we got message");
            if (data.getString("type").equals("status")) {
//                final TextView tv = (TextView) findViewById(R.id.statustext);
//                tv.setText(data.getString("content"));
            } else if (data.getString("type").equals("data")) {
                Log.wtf("MQP", "Sending data to phone");
                sendDataToPhone(data);
            }

        }
    }

    /**
     * Convert a bundle to JSON. Messsage API only takes strings, may be easier
     * to convert JSON to string and back than converting bundle to string and back
     * @param bundle
     * @return
     */
    public JSONObject bundleToJSON(Bundle bundle) {
        JSONObject json = new JSONObject();

        for(String key : bundle.keySet()) {
            try {
                json.put(key, JSONObject.wrap(bundle.get(key)));

            } catch(JSONException e) {
                //handle exception
            }
        }

        return json;
    }


    class SendToPhone extends Thread {
        String path;
        Bundle data;

        // Constructor for sending data objects to the data layer
        SendToPhone(String p, Bundle data) {
            path = p;
            this.data = data;
        }

        public void run() {

            String DATA_RECEIVER_CAPABILITY = "data_receiver";

            //retrieve node that can receive data(phone)
            CapabilityApi.GetCapabilityResult result =
                    Wearable.CapabilityApi.getCapability(
                            mGoogleApiClient, DATA_RECEIVER_CAPABILITY,
                            CapabilityApi.FILTER_REACHABLE).await();
            String nodeId = result.getCapability().getNodes().iterator().next().getId();

            //send data to phone
            JSONObject dataJson = bundleToJSON(data);
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId,
                    path, dataJson.toString().getBytes());
        }
    }
}
