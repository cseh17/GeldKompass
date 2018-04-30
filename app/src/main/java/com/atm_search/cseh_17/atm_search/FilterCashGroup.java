package com.atm_search.cseh_17.atm_search;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.atm_search.cseh_17.atm_search.Model.MyAtms;
import com.atm_search.cseh_17.atm_search.Model.Results;
import com.atm_search.cseh_17.atm_search.Remote.GoogleAPIService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.LinkedList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilterCashGroup {

    public static void nearByBanksFilteredCashGroup(final GoogleMap mMap, final LinkedList<RVRowInformation> data, GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, String url) {
        mMap.clear();
        data.clear();
        Log.i("Filter ausgeführt", "now");
        Log.i("Longitude", ""+ longitude);
        Log.i("Latitude", ""+ latitude);

        mService.getNearByPoi(url)
                .enqueue(new Callback<MyAtms>() {
                    @Override
                    public void onResponse(Call<MyAtms> call, Response<MyAtms> response) {
                        if (response.isSuccessful()){

                            int zaehler = response.body().getResults().length;

                            if (zaehler > 10){
                                zaehler = 10;
                            }
                            for (Integer i = 0; i < zaehler; i++){

                                boolean toDisplay = true;
                                Results googlePlace = response.body().getResults()[i];
                                RVRowInformation thisRow = new RVRowInformation();

                                if (i == 0) {
                                    Double locationToAtmDistance = Distance.distance1(Double.parseDouble(googlePlace.getGeometry().getLocation().getLat()), latitude, Double.parseDouble(googlePlace.getGeometry().getLocation().getLng()), longitude, 0, 0);
                                    if (locationToAtmDistance > 500) {
                                        //Move map camera
                                        LatLng latLng = new LatLng(latitude, longitude);
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                    }
                                }

                                String placeName = googlePlace.getName();
                                toDisplay = BlackListFilter.isBlacklisted(placeName.toLowerCase());
                                if (toDisplay == false) {
                                    if (zaehler < 20) {
                                        zaehler = zaehler + 1;
                                    }
                                }
                                String[] checkType = googlePlace.getTypes();
                                for (int j = 0; j< checkType.length; j++) {
                                    if (checkType[j].equals("insurance_agency")) {
                                        toDisplay = false;
                                        if (zaehler < 20) {
                                            zaehler = zaehler + 1;
                                        }
                                    }
                                }

                                // Calculates air distance between location & atm
                                Double locationToAtmDistance = Distance.distance1(Double.parseDouble(googlePlace.getGeometry().getLocation().getLat()), latitude, Double.parseDouble(googlePlace.getGeometry().getLocation().getLng()), longitude, 0, 0);
                                Log.i("Zähler Bank ", i.toString());
                                Log.i("Distance to bank: ", locationToAtmDistance.toString());
                                Log.i("Bank Name", googlePlace.getName());
                                if (locationToAtmDistance > 1500) {
                                    toDisplay = false;
                                }



                                if (!placeName.toLowerCase().contains("commerz")
                                        && !placeName.toLowerCase().contains("deutsche")
                                        && !placeName.toLowerCase().contains("hypo")
                                        && !placeName.toLowerCase().contains("post")){
                                    toDisplay = false;
                                    if (zaehler < 20) {
                                        zaehler = zaehler + 1;
                                    }
                                }



                                if (toDisplay) {
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                                    double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());


                                    LatLng latLng = new LatLng(lat, lng);
                                    markerOptions.position(latLng);
                                    if (placeName.toLowerCase().contains("commerzbank")){
                                        markerOptions.title("Commerzbank");
                                        thisRow.iconId = images[1];
                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.commerzbank_logo_final));
                                    } else {
                                        if (placeName.toLowerCase().contains("deutsche")){
                                            markerOptions.title("Deutsche Bank");
                                            thisRow.iconId = images[2];
                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                        } else {
                                            if (placeName.toLowerCase().contains("post")) {
                                                markerOptions.title("Postbank");
                                                thisRow.iconId = images[7];
                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                            } else {
                                                if (placeName.toLowerCase().contains("hypo")){
                                                    markerOptions.title("HypoVereinsbank");
                                                    thisRow.iconId = images[4];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                                    }
                                            }
                                        }
                                    }

                                    // Add Marker to map
                                    mMap.addMarker(markerOptions);

                                    // Add to ListView
                                    thisRow.rowTitle = markerOptions.getTitle();
                                    thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                    //thisRow.rowSubtitle = new StringBuilder().append("umgefähre Entfernung: ").append(String.format(Locale.GERMAN, "%.0f", locationToAtmDistance)).append(" m").toString();
                                    data.add(thisRow);
                                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mContext));

                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<MyAtms> call, Throwable t) {

                    }
                });
    }
}
