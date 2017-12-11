package com.example.tommy.alcodeepapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        disableStopButton();
    }

    public void pressStartButton(View v) {
        disableStartButton();
        enableStopButton();

        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Recording...");
    }

    public void pressStopButton(View v) {
        disableStopButton();
        enableStartButton();

        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Stopped...");

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
}
