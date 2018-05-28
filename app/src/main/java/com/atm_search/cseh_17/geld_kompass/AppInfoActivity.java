package com.atm_search.cseh_17.geld_kompass;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class AppInfoActivity extends AppCompatActivity{

    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_info_screen);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }
}
