package com.atm_search.cseh_17.geld_kompass;

import android.app.Application;

import java.util.Timer;
import java.util.TimerTask;

public class GeldKompassApp extends Application {

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    public  boolean wasInBackground;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 1500;


    public void startActivityTransitionTimer() {
        this.mActivityTransitionTimer = new Timer();
        this.mActivityTransitionTimerTask = new TimerTask() {
            @Override
            public void run() {
                GeldKompassApp.this.wasInBackground = true;
            }
        };

        this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask, MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    public void stopActivityTransitionTimer(){
        if (this.mActivityTransitionTimerTask != null){
            this.mActivityTransitionTimerTask.cancel();
        }

        if (this.mActivityTransitionTimer != null){
            this.mActivityTransitionTimer.cancel();
        }

        this.wasInBackground = false;
    }

}
