package com.example.tommy.alcodeepapp;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, -5),
                new DataPoint(2, 3),
                new DataPoint(3, -2),
                new DataPoint(5, 6),
                new DataPoint(7, 1),
                new DataPoint(10, -5),
        });

        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(10, -5),
                new DataPoint(12, 3),
                new DataPoint(16, -2),
                new DataPoint(20, 6),
                new DataPoint(22, 1),
                new DataPoint(25, -5),
                new DataPoint(26, 3),
                new DataPoint(30, -2),
                new DataPoint(32, 6)
        });

        series.setColor(Color.GREEN);
        graph.addSeries(series);
        graph.addSeries(series2);
    }

    public void pressDoneButton(View v) {
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }
}
