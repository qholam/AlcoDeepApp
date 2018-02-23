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
            String resultString = jsonObject.getString("result");
            String results;

            switch(resultString) {
                case "green_goggles":
                    results = "0.00-0.08 BAC Level";
                    break;
                case "black_goggles":
                    results = "0.08-0.15 BAC Level";
                    break;
                case "red_goggles":
                    results = "0.15-0.25 BAC Level";
                    break;
                case "orange_goggles":
                    results = "0.25-0.35 BAC Level";
                    break;
                case "no_goggles":
                    results = "0.00 BAC Level";
                    break;
                case "-1":
                    results = "watch not connected";
                    break;
                default:
                    results = "error";
                    break;
            }

            TextView tv = (TextView) findViewById(R.id.result);
            tv.setText(results);
        }
        catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }

    public void pressDoneButton(View v) {
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }
}
