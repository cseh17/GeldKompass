package de.atm_search.cseh_17.geld_kompass;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

class GetCityNameFromCoordinates {
    static String getCityName(double lat, double lng, Context mContext) {

        String city = "";
        try {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses.isEmpty()) {
                city = "error";
            } else {
                city = addresses.get(0).getLocality();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return city;
    }
}
