package com.atm_search.cseh_17.geld_kompass;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;


public class AtmDataStructure implements Serializable {

    RVRowInformation currentAtm;
    String mMarkerOptionsTitle;
    double mMarkerOptionLat;
    double mMarkerOptionLng;
}
