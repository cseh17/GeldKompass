package com.atm_search.cseh_17.geld_kompass;


import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class CheckConnection {

    public static boolean isConnected(ConnectivityManager cm) {
        //ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i("Connected to", " WiFi");
            }
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                Log.i("Connected to", " mobile");
            }
            return true;
        } else {
            Log.i("Connected to:", " No connection");
            return false;
        }
    }
}
