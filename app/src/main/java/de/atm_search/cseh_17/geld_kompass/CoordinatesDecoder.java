package de.atm_search.cseh_17.geld_kompass;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.util.List;
import java.util.Locale;

import com.google.firebase.perf.metrics.AddTrace;

class CoordinatesDecoder {

    @AddTrace(name = "CoordinateDecoder-getCompleteAdress")
    static String getCompleteAddress(Context mContext, double latitude, double longitude){

        String strAdd = "";
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        try{
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null) {
                strAdd = addresses.get(0).getThoroughfare() + " " + addresses.get(0).getFeatureName() + "\n" + addresses.get(0).getPostalCode() + " " + addresses.get(0).getLocality();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return strAdd;
    }

}