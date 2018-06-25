package com.atm_search.cseh_17.geld_kompass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkTraffic extends AsyncTask<String, String, String> {

    private WeakReference<Activity> mWeakActivity;

    @SuppressLint("StaticFieldLeak")
    private Spinner question1, question2;
    @SuppressLint("StaticFieldLeak")
    private ProgressBar loadingProgressBar;
    private static String resCode;

    NetworkTraffic(Activity activity, Spinner question1, Spinner question2, ProgressBar loadingProgressBar){

        mWeakActivity = new WeakReference<>(activity);
        this.question1 = question1;
        this.question2 = question2;
        this.loadingProgressBar = loadingProgressBar;
    }

    @AddTrace(name = "NetworkTraffic-doInBackground")
    @Override
    protected String doInBackground(String... strings) {

        LatLng mLocation = MainMapAtmDisplay.getLocation();

        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        FormBody body = new FormBody.Builder()
                .add("entry.2144818346", question1.getSelectedItem().toString().trim())
                .add("entry.878012640", question2.getSelectedItem().toString().trim())
                .add("entry.348582834", String.valueOf(mLocation.latitude))
                .add("entry.1885223603", String.valueOf(mLocation.longitude))
                .build();

        Request request = new Request.Builder()
                .url("https://docs.google.com/forms/d/1r4pBDJXR5jfhGq3zi6mDd0mjDkK7NGerwQVe18f122k/formResponse")
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            resCode = String.valueOf(response.code());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resCode;
    }

    @Override
    protected void onPostExecute(String resCode){

        loadingProgressBar.setVisibility(View.GONE);
        Activity activity = mWeakActivity.get();

        Trace myTrace = FirebasePerformance.getInstance().newTrace("GoogleFormResponse");
        myTrace.start();

        if (activity != null) {
            if (resCode.equals("200")){
                Toast.makeText(activity, "Report sent succesfully", Toast.LENGTH_LONG).show();
                myTrace.incrementMetric(resCode,1);
            } else {
                Toast.makeText(activity, "Server error. Please try again later", Toast.LENGTH_LONG).show();
                myTrace.incrementMetric(resCode,1);
            }
            myTrace.stop();
            Intent appMainIntent = new Intent(activity, MainMapAtmDisplay.class);
            activity.startActivity(appMainIntent);
        }

    }

}
