package com.atm_search.cseh_17.geld_kompass;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

import okhttp3.Response;

public class ReportFormActivity extends AppCompatActivity{

    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState){


        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_form);

        final Spinner question1 = findViewById(R.id.question1_spinner);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.report_spinner_problem, android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        question1.setAdapter(adapter);

        final Spinner question2 = findViewById(R.id.question2_spinner);
        ArrayAdapter adapter1 = ArrayAdapter.createFromResource(this, R.array.report_spinner_banks, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        question2.setAdapter(adapter1);

        final Button submitButton = findViewById(R.id.rf_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ProgressBar loadingProgressBar = ReportFormActivity.this.findViewById(R.id.rf_progresLoader);
                loadingProgressBar.setVisibility(View.VISIBLE);
                new NetworkTraffic(ReportFormActivity.this, question1, question2, loadingProgressBar).execute();
            }
        });
    }
}
