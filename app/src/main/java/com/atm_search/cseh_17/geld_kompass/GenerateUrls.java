package com.atm_search.cseh_17.geld_kompass;

import android.util.Log;

public class GenerateUrls {

    public static String getUrlAtm(double latitude, double longitude, String browserKey) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude).append("&rankby=distance").append("&keyword=").append("atm").append("&sensor=true").append("&key=").append(browserKey);
        Log.i("Url generated: ", googlePlacesUrl.toString());

        return googlePlacesUrl.toString();
    }

    public static String getUrlBank(double latitude, double longitude, String type, String browserKey) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude).append("&rankby=distance").append("&type=").append(type).append("&sensor=true").append("&key=").append(browserKey);
        Log.i("Url generated: ", googlePlacesUrl.toString());

        return googlePlacesUrl.toString();
    }

}
