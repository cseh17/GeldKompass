package com.atm_search.cseh_17.geld_kompass;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.atm_search.cseh_17.geld_kompass.Model.MyAtms;
import com.atm_search.cseh_17.geld_kompass.Model.Results;
import com.atm_search.cseh_17.geld_kompass.Remote.GoogleAPIService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Filters {

    public static void nearByBanksFilteredCashGroup(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final String url, final Activity mActivity, final RVAdapter adapter) {
        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        Log.i("Filter ausgeführt", "now");
        Log.i("Longitude", ""+ longitude);
        Log.i("Latitude", ""+ latitude);

        mService.getNearByPoi(url)
                .enqueue(new Callback<MyAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyAtms> call, @NonNull Response<MyAtms> response) {
                        if (response.isSuccessful()){

                            int zaehler = Objects.requireNonNull(response.body()).getResults().length;

                            if (zaehler > 10){
                                zaehler = 10;
                            }
                            for (Integer i = 0; i < zaehler; i++){

                                boolean toDisplay;
                                Results googlePlace = Objects.requireNonNull(response.body()).getResults()[i];
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
                                if (!toDisplay) {
                                    if (zaehler < 20) {
                                        zaehler = zaehler + 1;
                                    }
                                }
                                String[] checkType = googlePlace.getTypes();
                                for (String aCheckType : checkType) {
                                    if (aCheckType.equals("insurance_agency")) {
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
                        if (data.isEmpty()) {
                            CustomAlertDialog alert = new CustomAlertDialog();
                            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                            SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, url, mActivity, adapter);
                        }
                        adapter.notifyDataSetChanged();
                        loadingProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                    }
                });
    }

    public static void nearByBanksFilteredCashPool(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final String url, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        Log.i("Filter ausgeführt", "now");
        Log.i("Longitude", ""+ longitude);
        Log.i("Latitude", ""+ latitude);

        mService.getNearByPoi(url)
                .enqueue(new Callback<MyAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyAtms> call, @NonNull Response<MyAtms> response) {
                        if (response.isSuccessful()) {
                            int zaehler = Objects.requireNonNull(response.body()).getResults().length;

                            if (zaehler > 10) {
                                zaehler = 10;
                            }
                            for (Integer i = 0; i < zaehler; i++) {

                                boolean toDisplay;
                                Results googlePlace = Objects.requireNonNull(response.body()).getResults()[i];
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
                                if (!toDisplay) {
                                    if (zaehler < 20) {
                                        zaehler = zaehler + 1;
                                    }
                                }
                                String[] checkType = googlePlace.getTypes();
                                for (String aCheckType : checkType) {
                                    if (aCheckType.equals("insurance_agency")) {
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


                                if (!placeName.toLowerCase().contains("bbb")
                                        && !placeName.toLowerCase().contains("degussa")
                                        && !placeName.toLowerCase().contains("national")
                                        && !placeName.toLowerCase().contains("pax")
                                        && !placeName.toLowerCase().contains("santan")
                                        && !placeName.toLowerCase().contains("sparda")
                                        && !placeName.toLowerCase().contains("südwest")
                                        && !placeName.toLowerCase().contains("targo")
                                        && !placeName.toLowerCase().contains("oldenburgische landesbank")
                                        && !placeName.toLowerCase().contains("olb")) {
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
                                    if (placeName.toLowerCase().contains("bb")) {
                                        markerOptions.title("BBBank");
                                        thisRow.iconId = images[0];
                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                    } else {
                                        if (placeName.toLowerCase().contains("santander")) {
                                            markerOptions.title("Santander");
                                            thisRow.iconId = images[9];
                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.santander_logo_final));
                                        } else {
                                            if (placeName.toLowerCase().contains("sparda")) {
                                                markerOptions.title("Sparda-Bank");
                                                thisRow.iconId = images[10];
                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparda_bank_logo_final));
                                            } else {
                                                if (placeName.toLowerCase().contains("targo")) {
                                                    markerOptions.title("TargoBank");
                                                    thisRow.iconId = images[12];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.targobank_logo_final));
                                                } else {
                                                    if (placeName.toLowerCase().contains("degussa")) {
                                                        markerOptions.title("Degussa Bank");
                                                        thisRow.iconId = images[15];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("oldenburgische landesbank") || placeName.toLowerCase().contains("olb")) {
                                                            markerOptions.title("Oldenburgische Landesbank");
                                                            thisRow.iconId = images[18];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.oldenburgische_landesbank_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("südwest")) {
                                                                markerOptions.title("Südwestbank");
                                                                thisRow.iconId = images[19];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.suedwestbank_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("pax")) {
                                                                    markerOptions.title("Pax-Bank");
                                                                    thisRow.iconId = images[6];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                                } else {
                                                                    markerOptions.title(placeName);
                                                                    thisRow.iconId = images[3];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.generic_logo_final));
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Add Marker to map
                                    mMap.addMarker(markerOptions);

                                    // Add to ListView
                                    thisRow.rowTitle = markerOptions.getTitle();
                                    thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                    data.add(thisRow);
                                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mContext));

                                }
                            }
                        }
                        if (data.isEmpty()) {
                            CustomAlertDialog alert = new CustomAlertDialog();
                            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                            SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, url, mActivity, adapter);
                        }
                        adapter.notifyDataSetChanged();
                        loadingProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                    }
                });
    }

    public static void nearByBanksFilteredSparkasse(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final String url, final Activity mActivity, final RVAdapter adapter) {
        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        Log.i("Filter ausgeführt", "now");
        Log.i("Longitude", ""+ longitude);
        Log.i("Latitude", ""+ latitude);

        mService.getNearByPoi(url)
                .enqueue(new Callback<MyAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyAtms> call, @NonNull Response<MyAtms> response) {
                        if (response.isSuccessful()) {

                            int zaehler = Objects.requireNonNull(response.body()).getResults().length;

                            if (zaehler > 10) {
                                zaehler = 10;
                            }
                            for (Integer i = 0; i < zaehler; i++) {

                                boolean toDisplay;
                                Results googlePlace = Objects.requireNonNull(response.body()).getResults()[i];
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
                                if (!toDisplay) {
                                    if (zaehler < 20) {
                                        zaehler = zaehler + 1;
                                    }
                                }
                                String[] checkType = googlePlace.getTypes();
                                for (String aCheckType : checkType) {
                                    if (aCheckType.equals("insurance_agency")) {
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


                                if (!placeName.toLowerCase().contains("sparkasse")) {
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
                                    if (placeName.toLowerCase().contains("sparkasse")) {
                                        markerOptions.title(placeName);
                                        thisRow.iconId = images[11];
                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                    }

                                    // Add Marker to map
                                    mMap.addMarker(markerOptions);

                                    // Add to ListView
                                    thisRow.rowTitle = markerOptions.getTitle();
                                    thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                    data.add(thisRow);
                                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mContext));

                                }
                            }
                        }
                        if (data.isEmpty()) {
                            CustomAlertDialog alert = new CustomAlertDialog();
                            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                            SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, url, mActivity, adapter);
                        }
                        adapter.notifyDataSetChanged();
                        loadingProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                    }
                });
    }
}
