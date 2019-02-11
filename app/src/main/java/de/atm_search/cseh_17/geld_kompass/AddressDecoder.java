package de.atm_search.cseh_17.geld_kompass;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.perf.metrics.AddTrace;

import java.io.IOException;
import java.util.List;

class AddressDecoder {

    @AddTrace(name = "AddressDecoder-getLocationFromAddress")
    static LatLng getLocationFromAddress(Context mContext, String mAddress){

        Geocoder coder = new Geocoder(mContext);
        List<Address> address;
        LatLng coordinates = null;

        try{

            address = coder.getFromLocationName(mAddress, 5);
            if (address == null){
                return null;
            }

            Address location = address.get(0);
            coordinates = new LatLng(location.getLatitude(), location.getLongitude());
        } catch (IOException e){
            e.printStackTrace();
        }

        return coordinates;
    }
}
