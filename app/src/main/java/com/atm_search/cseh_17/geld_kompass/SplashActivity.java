package com.atm_search.cseh_17.geld_kompass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.perf.metrics.AddTrace;

public class
SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        startActivity(new Intent(SplashActivity.this, MainMapAtmDisplay.class));
        finish();
    }
}
