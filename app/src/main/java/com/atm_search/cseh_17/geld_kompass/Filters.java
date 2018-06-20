package com.atm_search.cseh_17.geld_kompass;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.atm_search.cseh_17.geld_kompass.Remote.APIService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.LinkedList;

import static com.atm_search.cseh_17.geld_kompass.BitmapDescriptorFromVector.bitmapDescriptorFromVector;

public class Filters {

    private static LinkedList<AtmDataStructure> cachedEntries;
    private static long lastSaved;
    private static double lat, lng;

    public static boolean nearByBanksFilteredCashGroup(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        // Clear data and map in order to avoid doubles on list.
        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
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

            boolean toDisplay;
            boolean isFirst = true;
            for (AtmDataStructure entry : cachedEntries) {

                toDisplay = entry.mMarkerOptionsTitle.toLowerCase().contains("commerz")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche bank")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("hypo")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("post");

                if (toDisplay) {
                    if (isFirst){

                        isFirst = false;
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude, 0, 0);
                        if (locationToAtmDistance > 400) {

                            //Move map camera
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                        } else {
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }

                    }


                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("commerzbank")) {
                        mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
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
                    mMarkerOptions.snippet(entry.mMarkerOptionSnippet);
                    mMarkerOptions.position(new LatLng(entry.mMarkerOptionLat, entry.mMarkerOptionLng));
                    mMap.addMarker(mMarkerOptions);

                    // Add to ListView
                    data.add(entry.currentAtm);
                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                }
            }
            adapter.notifyDataSetChanged();
        }


        if (data.isEmpty()) {
            CustomAlertDialog alert = new CustomAlertDialog();
            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_cash_group_DE));

            //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlBank(latitude, longitude, "bank", mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlAtm(latitude, longitude, mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            return false;
        } else {

            loadingProgressBar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }, 1000);
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


    public static boolean nearByBanksFilteredCashPool(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
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

            boolean toDisplay;
            boolean isFirst = true;

            for (AtmDataStructure entry : cachedEntries) {

                toDisplay = entry.mMarkerOptionsTitle.toLowerCase().contains("bbb")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("degussa")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("national")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("pax")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("santan")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("sparda")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("südwest")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("targo")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("oldenburgische landesbank")
                        || entry.mMarkerOptionsTitle.toLowerCase().contains("olb");

                if (toDisplay) {

                    if (isFirst){

                        isFirst = false;
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude, 0, 0);
                        if (locationToAtmDistance > 400) {

                            //Move map camera
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                        } else {

                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }

                    }

                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("bb")) {
                        mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_bbbank_marker));
                    } else {
                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("santander")) {
                            mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_santander_marker));
                        } else {
                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("sparda")) {
                                mMarkerOptions.icon(bitmapDescriptorFromVector(mContext, R.drawable.ic_new_sparda_bank_marker5));
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
                                                    mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3 ));
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
                    mMarkerOptions.snippet(entry.mMarkerOptionSnippet);
                    mMarkerOptions.position(new LatLng(entry.mMarkerOptionLat, entry.mMarkerOptionLng));
                    mMap.addMarker(mMarkerOptions);

                    // Add to ListView
                    data.add(entry.currentAtm);
                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                }
            }
            adapter.notifyDataSetChanged();
        }

        if (data.isEmpty()) {

            CustomAlertDialog alert = new CustomAlertDialog();
            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_cash_pool_DE));
            //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlBank(latitude, longitude, "bank", mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlAtm(latitude, longitude, mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            return false;
        } else {

            loadingProgressBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                }, 1000);
                return true;
        }
    }

    public static boolean nearByBanksFilteredSparkasse(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();

        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
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

            boolean toDisplay;
            boolean isFirst = true;
            for (AtmDataStructure entry : cachedEntries) {

                toDisplay = entry.mMarkerOptionsTitle.toLowerCase().contains("sparkasse");

                if (toDisplay) {

                    if (isFirst){

                        isFirst = false;
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude, 0, 0);
                        if (locationToAtmDistance > 400) {

                            //Move map camera
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                        } else {
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }

                    }

                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("sparkasse")) {
                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                    }


                    // Add Marker to map
                    mMarkerOptions.title(entry.mMarkerOptionsTitle);
                    mMarkerOptions.snippet(entry.mMarkerOptionSnippet);
                    mMarkerOptions.position(new LatLng(entry.mMarkerOptionLat, entry.mMarkerOptionLng));
                    mMap.addMarker(mMarkerOptions);

                    // Add to ListView
                    data.add(entry.currentAtm);
                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                }
            }
            adapter.notifyDataSetChanged();
        }

        if (data.isEmpty()) {
            CustomAlertDialog alert = new CustomAlertDialog();
            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_sparkasse_DE));
            //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlBank(latitude, longitude, "bank", mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlAtm(latitude, longitude, mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            return false;
        } else {

                loadingProgressBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                }, 1000);
                return true;
        }
    }

    public static boolean nearByBanksFilteredVolksbank(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
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

            Log.i("Volksbank filter", "deployed");
            MarkerOptions mMarkerOptions = new MarkerOptions();

            boolean toDisplay;
            boolean isFirst = true;
            for (AtmDataStructure entry : cachedEntries) {

                toDisplay = entry.mMarkerOptionsTitle.toLowerCase().contains("volks")
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("aachener"))
                        || (entry.mMarkerOptionsTitle.contains("bopfing"))
                        || (entry.mMarkerOptionsTitle.contains("brühl"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("donau"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("erfurter"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("federsee bank"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("frankenberger bank"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("geno"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("genossenschafts bank münchen"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("gls"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("unterlegäu"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("kölner"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("ievo"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("liga"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("märki"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("münchener bank"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("reiffeisen"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("rv bank"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("darlehenkasse"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("spaar & kredit"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("spaar&kredit"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("spreewald"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("vr"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("waldecker"))
                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("team"));

                if (toDisplay) {

                    if (isFirst){

                        isFirst = false;
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude, 0, 0);
                        if (locationToAtmDistance > 400) {

                            //Move map camera
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                        } else {
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }

                    }

                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));

                    // Add Marker to map
                    mMarkerOptions.title(entry.mMarkerOptionsTitle);
                    mMarkerOptions.snippet(entry.mMarkerOptionSnippet);
                    mMarkerOptions.position(new LatLng(entry.mMarkerOptionLat, entry.mMarkerOptionLng));
                    mMap.addMarker(mMarkerOptions);

                    // Add to ListView
                    data.add(entry.currentAtm);
                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                }
            }
            adapter.notifyDataSetChanged();
        }

        if (data.isEmpty()) {
            CustomAlertDialog alert = new CustomAlertDialog();
            alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_volksbank_DE));
            //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlBank(latitude, longitude, "bank", mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, mContext, GenerateUrls.getUrlAtm(latitude, longitude, mContext.getResources().getString(R.string.browser_key)), mActivity, adapter);
            //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
            return false;
        } else {

            loadingProgressBar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }, 1000);
            return true;
        }
    }

}
