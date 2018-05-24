package com.atm_search.cseh_17.geld_kompass;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Filters {

    protected static LinkedList<AtmDataStructure> cachedEntries;
    protected static long lastSaved;
    protected static double lat, lng;

    public static boolean nearByBanksFilteredCashGroup(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {
        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        try {

            // Retrieve the list from internal storage
            cachedEntries = (LinkedList<AtmDataStructure>) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_atms));
            lastSaved = (long) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_timestamp));

            // Retrieve latitude and longitude for distance calculation
            lat = (double) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_latitude));
            lng = (double) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_longitude));

        } catch (IOException | ClassNotFoundException e) {
            Log.e("Cache error:", "No data found in cache");
        }

        if (cachedEntries != null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude, 0, 0) < 101 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            Log.i("CashGroup filter", "deployed");
            MarkerOptions mMarkerOptions = new MarkerOptions();
            AtmDataStructure firstEntry = cachedEntries.getFirst();

            Double locationToAtmDistance = Distance.distance1(firstEntry.mMarkerOptionLat, latitude, firstEntry.mMarkerOptionLng, longitude, 0, 0);
            if (locationToAtmDistance > 500) {
                //Move map camera
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            } else {
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            boolean toDisplay;
            for (AtmDataStructure entry : cachedEntries) {
                toDisplay = true;

                if (!entry.mMarkerOptionsTitle.toLowerCase().contains("commerz")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("hypo")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("post")) {
                    toDisplay = false;
                }
                if (toDisplay) {

                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("commerzbank")) {
                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.commerzbank_logo_final));
                    } else {
                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("hypo")) {
                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                        } else {
                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche")) {
                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                            } else {
                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("post")) {
                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                }
                            }
                        }
                    }

                    // Add Marker to map
                    mMarkerOptions.title(entry.mMarkerOptionsTitle);
                    mMarkerOptions.position(new LatLng(entry.mMarkerOptionLat, entry.mMarkerOptionLng));
                    mMap.addMarker(mMarkerOptions);

                    // Add to ListView
                    data.add(entry.currentAtm);
                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                }
            }
        }


        if (data.isEmpty()) {
            CustomAlertDialog alert = new CustomAlertDialog();
            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));

            SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlBank(latitude, longitude, "bank", mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlAtm(latitude, longitude, mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            return false;
        } else {

            adapter.notifyDataSetChanged();
            loadingProgressBar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }, 1500);
            return true;
        }
    }



        /*
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
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                    }*/


    public static boolean nearByBanksFilteredCashPool(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        try {

            // Retrieve the list from internal storage
            cachedEntries = (LinkedList<AtmDataStructure>) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_atms));
            lastSaved = (long) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_timestamp));

            // Retrieve latitude and longitude for distance calculation
            lat = (double) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_latitude));
            lng = (double) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_longitude));

        } catch (IOException | ClassNotFoundException e) {
            Log.e("Cache error:", "No data found in cache");
        }

        if (cachedEntries!=null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude, 0, 0) < 101 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            Log.i("CashPool filter", "deployed");
            MarkerOptions mMarkerOptions = new MarkerOptions();
            AtmDataStructure firstEntry = cachedEntries.getFirst();

            Double locationToAtmDistance = Distance.distance1(firstEntry.mMarkerOptionLat, latitude, firstEntry.mMarkerOptionLng, longitude, 0, 0);
            if (locationToAtmDistance > 500) {
                //Move map camera
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            } else {
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            boolean toDisplay;
            for (AtmDataStructure entry : cachedEntries) {
                toDisplay = true;

                if (!entry.mMarkerOptionsTitle.toLowerCase().contains("bbb")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("degussa")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("national")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("pax")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("santan")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("sparda")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("südwest")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("targo")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("oldenburgische landesbank")
                        && !entry.mMarkerOptionsTitle.toLowerCase().contains("olb")) {
                    toDisplay = false;
                }

                if (toDisplay) {

                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("bb")) {
                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                        } else {
                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("santander")) {
                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.santander_logo_final));
                            } else {
                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("sparda")) {
                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparda_bank_logo_final));
                                } else {
                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("targo")) {
                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.targobank_logo_final));
                                    } else {
                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("degussa")) {
                                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                        } else {
                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("oldenburgische landesbank") || entry.mMarkerOptionsTitle.toLowerCase().contains("olb")) {
                                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.oldenburgische_landesbank_logo_final));
                                            } else {
                                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("südwest")) {
                                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.suedwestbank_logo_final));
                                                } else {
                                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("pax")) {
                                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                    } else {
                                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.generic_logo_final));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        // Add Marker to map
                        mMarkerOptions.title(entry.mMarkerOptionsTitle);
                        mMarkerOptions.position(new LatLng(entry.mMarkerOptionLat, entry.mMarkerOptionLng));
                        mMap.addMarker(mMarkerOptions);

                        // Add to ListView
                        data.add(entry.currentAtm);
                        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                    }
                }
            }


            if (data.isEmpty()) {
                CustomAlertDialog alert = new CustomAlertDialog();
                alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlBank(latitude, longitude, "bank", mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
                SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlAtm(latitude, longitude, mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
                return false;
            } else {

                adapter.notifyDataSetChanged();
                loadingProgressBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                }, 1500);
                return true;
            }
        }

    public static boolean nearByBanksFilteredSparkasse(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final String url, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        try {

            // Retrieve the list from internal storage
            cachedEntries = (LinkedList<AtmDataStructure>) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_atms));
            lastSaved = (long) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_timestamp));

            // Retrieve latitude and longitude for distance calculation
            lat = (double) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_latitude));
            lng = (double) CacheData.readObject(mContext, mContext.getString(R.string.KEY_for_longitude));

        } catch (IOException | ClassNotFoundException e) {
            Log.e("Cache error:", "No data found in cache");
        }

        if (cachedEntries != null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude, 0, 0) < 101 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            Log.i("Sparkasse filter", "deployed");
            MarkerOptions mMarkerOptions = new MarkerOptions();
            AtmDataStructure firstEntry = cachedEntries.getFirst();

            Double locationToAtmDistance = Distance.distance1(firstEntry.mMarkerOptionLat, latitude, firstEntry.mMarkerOptionLng, longitude, 0, 0);
            if (locationToAtmDistance > 500) {
                //Move map camera
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            } else {
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            boolean toDisplay;
            for (AtmDataStructure entry : cachedEntries) {
                toDisplay = true;

                if (!entry.mMarkerOptionsTitle.toLowerCase().contains("sparkasse")) {
                    toDisplay = false;
                }
                if (toDisplay) {


                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("sparkasse")) {
                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                        }


                        // Add Marker to map
                        mMarkerOptions.title(entry.mMarkerOptionsTitle);
                        mMarkerOptions.position(new LatLng(entry.mMarkerOptionLat, entry.mMarkerOptionLng));
                        mMap.addMarker(mMarkerOptions);

                        // Add to ListView
                        data.add(entry.currentAtm);
                        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                    }
                }
            }


            if (data.isEmpty()) {
                CustomAlertDialog alert = new CustomAlertDialog();
                alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlBank(latitude, longitude, "bank", mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
                SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlAtm(latitude, longitude, mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
                return false;
            } else {

                adapter.notifyDataSetChanged();
                loadingProgressBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                }, 1500);
                return true;
            }
        }
}