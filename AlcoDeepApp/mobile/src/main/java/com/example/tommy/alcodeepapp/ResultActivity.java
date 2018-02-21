package com.example.tommy.alcodeepapp;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONObject;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String bodyString = getIntent().getStringExtra("bodystring");

        try {
            JSONObject jsonObject = new JSONObject(bodyString);

            Log.wtf("Upload", jsonObject.get("result").toString());

            TextView result = (TextView) findViewById(R.id.result);
            result.setText(jsonObject.getString("result"));
    }
        catch (Exception e) {

        }
    }

    public void pressDoneButton(View v) {
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }
}
