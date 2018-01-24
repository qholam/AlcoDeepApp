package com.example.tommy.alcodeepapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


/**
 * Created by michaelHahn on 1/16/15.
 * Listener service or data events on the data layer
 */
public class MobileListenerService extends WearableListenerService {

    private static final String MOBILE_DATA_PATH = "/mobile_data";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(MOBILE_DATA_PATH)) {
            String dataString = new String(messageEvent.getData());

            try {
                JSONObject dataJson = new JSONObject(dataString);
                Bundle data = jsonToBundle(dataJson);

                Intent messageIntent = new Intent();
                messageIntent.setAction(Intent.ACTION_SEND);
                messageIntent.putExtra("datamap", data);
                LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            }
            catch (JSONException e) {
                //handle exception
            }
        }
    }

    public Bundle jsonToBundle(JSONObject jsonObject) throws JSONException{
        Bundle bundle = new Bundle();
        Iterator iter = jsonObject.keys();
        while(iter.hasNext()){
            String key = (String)iter.next();
            if(key.equals("type")) {
                String value = jsonObject.getString(key);
                bundle.putString(key, value);
            }
            else if(key.equals("dt")){
                JSONArray jsonArray = jsonObject.getJSONArray(key);
                long[] sensorData = new long[jsonArray.length()];
                for(int i = 0; i < jsonArray.length(); i++) {
                    sensorData[i] = Long.parseLong(jsonArray.get(i).toString());
                    Log.wtf("Watchdt", Long.toString(sensorData[i]));
                }
                bundle.putLongArray(key, sensorData);
            }
            else {
                JSONArray jsonArray = jsonObject.getJSONArray(key);
                float[] sensorData = new float[jsonArray.length()];
                for(int i = 0; i < jsonArray.length(); i++) {
                    sensorData[i] = Float.parseFloat(jsonArray.get(i).toString());
                    Log.wtf("Watch" + key, Float.toString(sensorData[i]));
                }
                bundle.putFloatArray(key, sensorData);
            }
        }
        return bundle;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        int result = super.onStartCommand(i, flags, startId);

        Log.wtf("MQP", "PHONE SERVICE STARTED");

        return result;
    }

    @Override
    public void onDestroy() {
        Log.wtf("MQP", "ONDESTROY");

    }
}