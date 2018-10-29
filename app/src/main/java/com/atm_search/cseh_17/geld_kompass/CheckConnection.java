package com.atm_search.cseh_17.geld_kompass;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

class CheckConnection {

    static boolean isConnected(ConnectivityManager cm) {

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null) {
            Log.i("Connected to:", " No connection");
            return false;
        } else {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i("Connected to", " WiFi");
            }
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                Log.i("Connected to", " mobile");
            }
            return true;
        }
    }
}
