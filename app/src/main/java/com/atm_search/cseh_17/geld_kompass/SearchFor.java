package com.atm_search.cseh_17.geld_kompass;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.atm_search.cseh_17.geld_kompass.Model.MyAtms;
import com.atm_search.cseh_17.geld_kompass.Model.Results;
import com.atm_search.cseh_17.geld_kompass.ModelOSM.Elements;
import com.atm_search.cseh_17.geld_kompass.ModelOSM.MyOsmAtms;
import com.atm_search.cseh_17.geld_kompass.Remote.APIService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;


import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;
import static com.atm_search.cseh_17.geld_kompass.BitmapDescriptorFromVector.bitmapDescriptorFromVector;


public class SearchFor {

    private static   LinkedList<AtmDataStructure> cachedEntries;
    private static long lastSaved;
    private static double lat, lng;
    private static final LinkedList<AtmDataStructure> toCache = new LinkedList<>(Collections.<AtmDataStructure>emptyList());

    // Function to search and display Bank branches on Google (Places API)
    public static void nearByBanks(final GoogleMap mMap, final LinkedList<RVRowInformation> data, APIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, String url, final Activity mActivity, final RVAdapter adapter) {

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

        if (cachedEntries==null || cachedEntries.isEmpty() || Distance.distance1(lat, latitude, lng, longitude, 0, 0) > 100 || ((System.currentTimeMillis() / 1000) - lastSaved) > 600) {

            // Clear map & data set. Avoids duplicates on map & on list
            mMap.clear();
            data.clear();
            toCache.clear();
            adapter.notifyDataSetChanged();

            Log.i("nearByBanks", "Request sent");

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

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
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
                                    //Log.i("Zähler Bank ", i.toString());
                                    //Log.i("Distance to bank: ", locationToAtmDistance.toString());
                                    if (locationToAtmDistance > 1500) {
                                        toDisplay = false;
                                    }



                                    if (!placeName.toLowerCase().contains("bank")
                                            && !placeName.toLowerCase().contains("kasse")
                                            && !placeName.toLowerCase().contains("diba")
                                            && !placeName.toLowerCase().contains("santander")
                                            && !placeName.toLowerCase().contains("seb")){
                                        toDisplay = false;
                                        if (zaehler < 20) {
                                            zaehler = zaehler + 1;
                                        }
                                    }



                                    if (toDisplay) {
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();
                                        double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                                        double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());

                                        toCacheElement.mMarkerOptionLat = lat;
                                        toCacheElement.mMarkerOptionLng = lng;

                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);
                                        if (placeName.toLowerCase().contains("commerzbank")){
                                            markerOptions.title("Commerzbank");
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_commerzbank_map_marker;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
                                        } else {
                                            if (placeName.toLowerCase().contains("sparkasse")) {
                                                markerOptions.title(placeName);
                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                thisRow.iconId = images[11];
                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                            } else {
                                                if (placeName.toLowerCase().contains("deutsche")) {
                                                    markerOptions.title("Deutsche Bank");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = images[2];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                                } else {
                                                    if (placeName.toLowerCase().contains("post")) {
                                                        markerOptions.title("Postbank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = images[7];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("volks")
                                                                || (placeName.toLowerCase().contains("aachener"))
                                                                || (placeName.toLowerCase().contains("bopfing"))
                                                                || (placeName.toLowerCase().contains("brühl"))
                                                                || (placeName.toLowerCase().contains("donau"))
                                                                || (placeName.toLowerCase().contains("erfurter"))
                                                                || (placeName.toLowerCase().contains("federsee bank"))
                                                                || (placeName.toLowerCase().contains("frankenberger bank"))
                                                                || (placeName.toLowerCase().contains("geno"))
                                                                || (placeName.toLowerCase().contains("genossenschafts bank münchen"))
                                                                || (placeName.toLowerCase().contains("gls"))
                                                                || (placeName.toLowerCase().contains("unterlegäu"))
                                                                || (placeName.toLowerCase().contains("kölner"))
                                                                || (placeName.toLowerCase().contains("ievo"))
                                                                || (placeName.toLowerCase().contains("liga"))
                                                                || (placeName.toLowerCase().contains("märki"))
                                                                || (placeName.toLowerCase().contains("münchener bank"))
                                                                || (placeName.toLowerCase().contains("reiffeisen"))
                                                                || (placeName.toLowerCase().contains("rv"))
                                                                || (placeName.toLowerCase().contains("darlehenkasse"))
                                                                || (placeName.toLowerCase().contains("spaar & kredit"))
                                                                || (placeName.toLowerCase().contains("spaar&kredit"))
                                                                || (placeName.toLowerCase().contains("spreewald"))
                                                                || (placeName.toLowerCase().contains("vr"))
                                                                || (placeName.toLowerCase().contains("waldecker"))
                                                                || (placeName.toLowerCase().contains("team"))) {
                                                            markerOptions.title("Volksbank Gruppe");
                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                            thisRow.iconId = images[13];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("bb")) {
                                                                markerOptions.title("BBBank");
                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                thisRow.iconId = images[0];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("hypo")) {
                                                                    markerOptions.title("HypoVereinsbank");
                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                    thisRow.iconId = images[4];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("psd")) {
                                                                        markerOptions.title("PSD Bank");
                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                        thisRow.iconId = images[8];
                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.psd_bank_logo_final));
                                                                    } else {
                                                                        if (placeName.toLowerCase().contains("santander")) {
                                                                            markerOptions.title("Santander");
                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                            thisRow.iconId = images[9];
                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.santander_logo_final));
                                                                        } else {
                                                                            if (placeName.toLowerCase().contains("sparda")) {
                                                                                markerOptions.title("Sparda-Bank");
                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                thisRow.iconId = images[10];
                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparda_bank_logo_final));
                                                                            } else {
                                                                                if (placeName.toLowerCase().contains("targo")) {
                                                                                    markerOptions.title("TargoBank");
                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                    thisRow.iconId = images[12];
                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.targobank_logo_final));
                                                                                } else {
                                                                                    if (placeName.toLowerCase().contains("apo")) {
                                                                                        markerOptions.title("Deutsche Apotheker und Ärzte Bank");
                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                        thisRow.iconId = images[14];
                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.apotheker_und_aerztebank_logo_final));
                                                                                    } else {
                                                                                        if (placeName.toLowerCase().contains("degussa")) {
                                                                                            markerOptions.title("Degussa Bank");
                                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                            thisRow.iconId = images[15];
                                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                                                                        } else {
                                                                                            if (placeName.toLowerCase().contains("lbbw") || placeName.toLowerCase().contains("wüttemb")) {
                                                                                                markerOptions.title("LBBW");
                                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                thisRow.iconId = images[16];
                                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lb_bw_logo_final));
                                                                                            } else {
                                                                                                if (placeName.toLowerCase().contains("lbb") || placeName.toLowerCase().contains("landesbank berlin")) {
                                                                                                    markerOptions.title("Landesbank Berlin");
                                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                    thisRow.iconId = images[17];
                                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lbb_logo_final));
                                                                                                } else {
                                                                                                    if (placeName.toLowerCase().contains("oldenburgische landesbank") || placeName.toLowerCase().contains("olb")) {
                                                                                                        markerOptions.title("Oldenburgische Landesbank");
                                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                        thisRow.iconId = images[18];
                                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.oldenburgische_landesbank_logo_final));
                                                                                                    } else {
                                                                                                        if (placeName.toLowerCase().contains("südwest")) {
                                                                                                            markerOptions.title("Südwestbank");
                                                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                            thisRow.iconId = images[19];
                                                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.suedwestbank_logo_final));
                                                                                                        } else {
                                                                                                            if (placeName.toLowerCase().contains("pax")) {
                                                                                                                markerOptions.title("Pax-Bank");
                                                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                thisRow.iconId = images[6];
                                                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                                                                            } else {
                                                                                                                markerOptions.title(placeName);
                                                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                thisRow.iconId = R.drawable.ic_new_general_map_marker3;
                                                                                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3));
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                        toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();

                                        // Add Marker to map
                                        mMap.addMarker(markerOptions);

                                        // Add to ListView
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                        toCacheElement.currentAtm = thisRow;
                                        data.add(thisRow);
                                        toCache.add(toCacheElement);
                                        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                                    }
                                }

                               /* // Check if the result Object is empty
                                if (data.isEmpty()){

                                    // If empty show the user an alert
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                                } else try {

                                    // If not empty try and cache the data for later usage
                                    CacheData.writeAtmData(mContext, mContext.getString(R.string.KEY_for_atms), toCache);

                                    // Also save the time the data was cached in UNIX timestamp format
                                    long unixTime = System.currentTimeMillis() / 1000;
                                    CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_timestamp), unixTime);

                                    // Save the location where the data was chached
                                    CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_latitude), latitude);
                                    CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_longitude), longitude);

                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                } */
                            }
                            adapter.notifyDataSetChanged();
                            loadingProgressBar.setVisibility(View.GONE);
                        }


                        @Override
                        public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                            Log.e("OnFailure", "Something went wrong. No API response");
                            CustomAlertDialog alert = new CustomAlertDialog();
                            alert.showDialog(mActivity, mActivity.getString(R.string.on_failure_alert_DE));
                        }
                    });
        }
    }

    // Function to search and display atms on Google (Places API)
    public static void nearByAtms(final GoogleMap mMap, final LinkedList<RVRowInformation> data, APIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, String url, final Activity mActivity, final RVAdapter adapter) {

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }
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

        if (cachedEntries!=null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude, 0, 0) < 151 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            mMap.clear();
            data.clear();
            adapter.notifyDataSetChanged();

            Log.i("SearchFor", "cached data is used");
            MarkerOptions mMarkerOptions = new MarkerOptions();
            AtmDataStructure firstEntry = cachedEntries.getFirst();

            Double locationToAtmDistance = Distance.distance1(firstEntry.mMarkerOptionLat, latitude, firstEntry.mMarkerOptionLng, longitude, 0, 0);
            if (locationToAtmDistance > 400) {

                //Move map camera
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            } else {
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            for (AtmDataStructure entry : cachedEntries) {

                if (entry.mMarkerOptionsTitle.toLowerCase().contains("commerzbank")) {
                    mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
                } else {
                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("sparkasse")) {
                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                    } else {
                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche")) {
                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                        } else {
                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("post")) {
                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                            } else {
                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("volks")
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("aachener"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("bopfing"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("brühl"))
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
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("rv"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("darlehenkasse"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("spaar & kredit"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("spaar&kredit"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("spreewald"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("vr"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("waldecker"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("team"))) {
                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                } else {
                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("bb")) {
                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                    } else {
                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("hypo")) {
                                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                        } else {
                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("psd")) {
                                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.psd_bank_logo_final));
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
                                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("apo")) {
                                                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.apotheker_und_aerztebank_logo_final));
                                                            } else {
                                                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("degussa")) {
                                                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                                                } else {
                                                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("lbbw") || entry.mMarkerOptionsTitle.toLowerCase().contains("wüttemb")) {
                                                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lb_bw_logo_final));
                                                                    } else {
                                                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("lbb") || entry.mMarkerOptionsTitle.toLowerCase().contains("landesbank berlin")) {
                                                                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lbb_logo_final));
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
                                                                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche")) {
                                                                                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                                                                        } else {
                                                                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("shell")) {
                                                                                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                                                            } else {
                                                                                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("ing")) {
                                                                                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ing_logo_final));
                                                                                                } else {
                                                                                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("pax")) {
                                                                                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                                                                    } else {
                                                                                                        mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3));
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
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
                adapter.notifyDataSetChanged();
                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
            }

            // adapter.notifyDataSetChanged();

            // The function to display the data from the cache memory is to fast to display a loading bar. There for a fake one will be showed for 1,5-2 seconds.
            loadingProgressBar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }, 1500);

        } else {

            Log.i("nearByAtm", "Request sent");

            mService.getNearByPoi(url)
                    .enqueue(new Callback<MyAtms>() {
                        @Override
                        public void onResponse(@NonNull Call<MyAtms> call, @NonNull Response<MyAtms> response) {
                            if (response.isSuccessful()) {

                                // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                                if (Objects.requireNonNull(response.body()).getResults().length != 0 && !data.isEmpty()) {

                                    int zaehler = Objects.requireNonNull(response.body()).getResults().length;

                                    if (zaehler > 10) {
                                        zaehler = 10;
                                    }

                                    for (int i = 0; i < zaehler; i++) {

                                        boolean toDisplay = true;
                                        Results googlePlace = Objects.requireNonNull(response.body()).getResults()[i];
                                        String[] checkType = googlePlace.getTypes();
                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check for doublettes that are also displayed under the banks search
                                        for (String aCheckType : checkType) {
                                            if (aCheckType.equals("bank")) {
                                                toDisplay = false;
                                                if (zaehler < 20) {
                                                    zaehler = zaehler + 1;
                                                }
                                            }
                                        }

                                        // Calculates air distance between location & atm
                                        Double locationToAtmDistance = Distance.distance1(Double.parseDouble(googlePlace.getGeometry().getLocation().getLat()), latitude, Double.parseDouble(googlePlace.getGeometry().getLocation().getLng()), longitude, 0, 0);

                                        // Check if distance is greater than 1500m, and hide all results that are farer than that.
                                        if (locationToAtmDistance > 1500) {
                                            toDisplay = false;
                                            if (zaehler < 20) {
                                                zaehler = zaehler + 1;
                                            }
                                        }


                                        String placeName = googlePlace.getName();

                                        if (!placeName.toLowerCase().contains("pax")
                                                && !placeName.toLowerCase().contains("diba")
                                                && !placeName.toLowerCase().contains("deutsche")) {
                                            toDisplay = false;
                                            if (zaehler < 20) {
                                                zaehler = zaehler + 1;
                                            }
                                        }


                                        if (toDisplay) {
                                            MarkerOptions markerOptions = new MarkerOptions();
                                            AtmDataStructure toCacheElement = new AtmDataStructure();
                                            double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                                            double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;

                                            LatLng latLng = new LatLng(lat, lng);
                                            markerOptions.position(latLng);
                                            markerOptions.title(placeName);


                                            if (placeName.toLowerCase().contains("deutsche")) {
                                                markerOptions.title("Deutsche Bank");
                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                thisRow.iconId = images[2];
                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                            } else {
                                                if (placeName.toLowerCase().contains("ing")) {
                                                    markerOptions.title("ING DiBa");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = images[5];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ing_logo_final));
                                                } else {
                                                    if (placeName.toLowerCase().contains("pax")) {
                                                        markerOptions.title("Pax Bank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = images[6];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                    } else {
                                                        thisRow.iconId = images[3];
                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3 ));
                                                    }
                                                }
                                            }

                                            toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                            toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();


                                            // In order to keep a logical display order, check if the distance to the atm is shorter than the last result from the bank search. If yes, add to list.
                                            if (Double.parseDouble(data.getLast().rowSubtitle) > locationToAtmDistance) {

                                                // Add Marker to map
                                                mMap.addMarker(markerOptions);
                                                thisRow.rowTitle = markerOptions.getTitle();
                                                thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                                toCacheElement.currentAtm = thisRow;

                                                data.add(thisRow);
                                                toCache.add(toCacheElement);
                                                Collections.sort(data, new CompareDistancesOnDisplayList());
                                                Collections.sort(toCache, new CompareDistanceOnCacheList());


                                            }
                                        }
                                    }

                                    // Check if the result Object is empty
                                    if (data.isEmpty()){

                                        // If empty show the user an alert
                                        CustomAlertDialog alert = new CustomAlertDialog();
                                        alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                                    } else try {

                                        // If not empty try and cache the data for later usage
                                        CacheData.writeAtmData(mContext, mContext.getString(R.string.KEY_for_atms), toCache);

                                        // Also save the time the data was cached in UNIX timestamp format
                                        long unixTime = System.currentTimeMillis() / 1000;
                                        CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_timestamp), unixTime);

                                        // Save the location where the data was chached
                                        CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_latitude), latitude);
                                        CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_longitude), longitude);

                                    } catch (IOException e) {
                                        Log.e(TAG, e.getMessage());
                                    }

                                } else {

                                    // Handle if no results were found
                                    loadingProgressBar.setVisibility(View.GONE);
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                                }
                            }
                            loadingProgressBar.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                        }
                    });
        }
    }


    private static BoundingBox calculateBoundingBox(double latitude, double longitude){

        double distanceFromCenterToCorner = 1500 * Math.sqrt(2.0);
        LatLng center = new LatLng(latitude, longitude);

        LatLng southwestCorner = SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 255.0);
        LatLng northestCorenr = SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);

        LatLngBounds bounds = new LatLngBounds(southwestCorner, northestCorenr);

        BoundingBox mBoundingBox = new BoundingBox();
        mBoundingBox.latMin = bounds.southwest.latitude;
        mBoundingBox.lngMin = bounds.southwest.longitude;
        mBoundingBox.latMax = bounds.northeast.latitude;
        mBoundingBox.lngMax = bounds.northeast.longitude;

        return mBoundingBox;

    }


    public static void osmNearByBanks(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final int[] images, final RVAdapter adapter, final LinkedList<RVRowInformation> data){

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

        if (cachedEntries==null || cachedEntries.isEmpty() || Distance.distance1(lat, latitude, lng, longitude, 0, 0) > 100 || ((System.currentTimeMillis() / 1000) - lastSaved) > 600) {

            // Clear map & data set. Avoids duplicates on map & on list
            mMap.clear();
            data.clear();
            toCache.clear();
            adapter.notifyDataSetChanged();

            Log.i("osmNearByBanks", "Request sent");

            BoundingBox coordinates = calculateBoundingBox(latitude, longitude);

            String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:25];(node[amenity=bank](" + coordinates.latMin + "," + coordinates.lngMin + "," + coordinates.latMax + "," + coordinates.lngMax + ");way[amenity=bank](" + coordinates.latMin + "," + coordinates.lngMin + "," + coordinates.latMax + "," + coordinates.lngMax + ");relation[amenity=bank](" + coordinates.latMin + "," + coordinates.lngMin + "," + coordinates.latMax + "," + coordinates.lngMax + "););out%20body;%3E;out%20skel%20qt;";

            Log.i("Gnerated URL", url);

            mService.getNearByBank(url)
                    .enqueue(new Callback<MyOsmAtms>() {
                        @Override
                        public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                            if (response.isSuccessful()) {

                                int zaehler = Objects.requireNonNull(response.body()).getElements().length;

                                if (zaehler > 10) {
                                    zaehler = 10;
                                }
                                for (Integer i = 0; i < zaehler; i++) {

                                    boolean toDisplay;
                                    Elements googlePlace = Objects.requireNonNull(response.body()).getElements()[i];
                                    RVRowInformation thisRow = new RVRowInformation();

                                    if (googlePlace.getLat() == null && googlePlace.getTags() != null && googlePlace.getTags().getName() != null) {

                                        String address = googlePlace.getTags().getAddrStreet() + " " + googlePlace.getTags().getAddrHousenumber() + " " + googlePlace.getTags().getAddrPostcode() + " " + googlePlace.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng coordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                        if (i == 0) {
                                            Double locationToAtmDistance = Distance.distance1(coordinates.latitude, latitude, coordinates.longitude, longitude, 0, 0);
                                            if (locationToAtmDistance > 400) {

                                                // Move map camera
                                                LatLng latLng = new LatLng(latitude, longitude);
                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                            } else {
                                                LatLng latLng = new LatLng(latitude, longitude);
                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                            }
                                        }

                                        String placeName = googlePlace.getTags().getName();
                                        toDisplay = BlackListFilter.isBlacklisted(placeName.toLowerCase());
                                        if (!toDisplay) {
                                            if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                zaehler = zaehler + 1;
                                            }
                                        }

                                        // Calculates air distance between location & atm
                                        Double locationToAtmDistance = Distance.distance1(coordinates.latitude, latitude, coordinates.longitude, longitude, 0, 0);

                                        if (locationToAtmDistance > 1500) {
                                            toDisplay = false;
                                        }

                                        if (!placeName.toLowerCase().contains("bank")
                                                && !placeName.toLowerCase().contains("kasse")
                                                && !placeName.toLowerCase().contains("diba")
                                                && !placeName.toLowerCase().contains("santander")
                                                && !placeName.toLowerCase().contains("seb")) {
                                            toDisplay = false;
                                            if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                zaehler = zaehler + 1;
                                            }
                                        }


                                        if (toDisplay) {
                                            MarkerOptions markerOptions = new MarkerOptions();
                                            AtmDataStructure toCacheElement = new AtmDataStructure();
                                            double lat = coordinates.latitude;
                                            double lng = coordinates.longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;

                                            LatLng latLng = new LatLng(lat, lng);
                                            markerOptions.position(latLng);
                                            if (placeName.toLowerCase().contains("commerzbank")) {
                                                markerOptions.title("Commerzbank");
                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                thisRow.iconId = R.drawable.ic_new_commerzbank_map_marker;
                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
                                            } else {
                                                if (placeName.toLowerCase().contains("sparkasse")) {
                                                    markerOptions.title(placeName);
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = images[11];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                                } else {
                                                    if (placeName.toLowerCase().contains("deutsche")) {
                                                        markerOptions.title("Deutsche Bank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = images[2];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("post")) {
                                                            markerOptions.title("Postbank");
                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                            thisRow.iconId = images[7];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("volks")
                                                                    || (placeName.toLowerCase().contains("aachener"))
                                                                    || (placeName.toLowerCase().contains("bopfing"))
                                                                    || (placeName.toLowerCase().contains("brühl"))
                                                                    || (placeName.toLowerCase().contains("donau"))
                                                                    || (placeName.toLowerCase().contains("erfurter"))
                                                                    || (placeName.toLowerCase().contains("federsee bank"))
                                                                    || (placeName.toLowerCase().contains("frankenberger bank"))
                                                                    || (placeName.toLowerCase().contains("geno"))
                                                                    || (placeName.toLowerCase().contains("genossenschafts bank münchen"))
                                                                    || (placeName.toLowerCase().contains("gls"))
                                                                    || (placeName.toLowerCase().contains("unterlegäu"))
                                                                    || (placeName.toLowerCase().contains("kölner"))
                                                                    || (placeName.toLowerCase().contains("ievo"))
                                                                    || (placeName.toLowerCase().contains("liga"))
                                                                    || (placeName.toLowerCase().contains("märki"))
                                                                    || (placeName.toLowerCase().contains("münchener bank"))
                                                                    || (placeName.toLowerCase().contains("reiffeisen"))
                                                                    || (placeName.toLowerCase().contains("rv"))
                                                                    || (placeName.toLowerCase().contains("darlehenkasse"))
                                                                    || (placeName.toLowerCase().contains("spaar & kredit"))
                                                                    || (placeName.toLowerCase().contains("spaar&kredit"))
                                                                    || (placeName.toLowerCase().contains("spreewald"))
                                                                    || (placeName.toLowerCase().contains("vr"))
                                                                    || (placeName.toLowerCase().contains("waldecker"))
                                                                    || (placeName.toLowerCase().contains("team"))) {
                                                                markerOptions.title("Volksbank Gruppe");
                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                thisRow.iconId = images[13];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("bb")) {
                                                                    markerOptions.title("BBBank");
                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                    thisRow.iconId = images[0];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("hypo")) {
                                                                        markerOptions.title("HypoVereinsbank");
                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                        thisRow.iconId = images[4];
                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                                                    } else {
                                                                        if (placeName.toLowerCase().contains("psd")) {
                                                                            markerOptions.title("PSD Bank");
                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                            thisRow.iconId = images[8];
                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.psd_bank_logo_final));
                                                                        } else {
                                                                            if (placeName.toLowerCase().contains("santander")) {
                                                                                markerOptions.title("Santander");
                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                thisRow.iconId = images[9];
                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.santander_logo_final));
                                                                            } else {
                                                                                if (placeName.toLowerCase().contains("sparda")) {
                                                                                    markerOptions.title("Sparda-Bank");
                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                    thisRow.iconId = images[10];
                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparda_bank_logo_final));
                                                                                } else {
                                                                                    if (placeName.toLowerCase().contains("targo")) {
                                                                                        markerOptions.title("TargoBank");
                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                        thisRow.iconId = images[12];
                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.targobank_logo_final));
                                                                                    } else {
                                                                                        if (placeName.toLowerCase().contains("apo")) {
                                                                                            markerOptions.title("Deutsche Apotheker und Ärzte Bank");
                                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                            thisRow.iconId = images[14];
                                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.apotheker_und_aerztebank_logo_final));
                                                                                        } else {
                                                                                            if (placeName.toLowerCase().contains("degussa")) {
                                                                                                markerOptions.title("Degussa Bank");
                                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                thisRow.iconId = images[15];
                                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                                                                            } else {
                                                                                                if (placeName.toLowerCase().contains("lbbw") || placeName.toLowerCase().contains("wüttemb")) {
                                                                                                    markerOptions.title("LBBW");
                                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                    thisRow.iconId = images[16];
                                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lb_bw_logo_final));
                                                                                                } else {
                                                                                                    if (placeName.toLowerCase().contains("lbb") || placeName.toLowerCase().contains("landesbank berlin")) {
                                                                                                        markerOptions.title("Landesbank Berlin");
                                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                        thisRow.iconId = images[17];
                                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lbb_logo_final));
                                                                                                    } else {
                                                                                                        if (placeName.toLowerCase().contains("oldenburgische landesbank") || placeName.toLowerCase().contains("olb")) {
                                                                                                            markerOptions.title("Oldenburgische Landesbank");
                                                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                            thisRow.iconId = images[18];
                                                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.oldenburgische_landesbank_logo_final));
                                                                                                        } else {
                                                                                                            if (placeName.toLowerCase().contains("südwest")) {
                                                                                                                markerOptions.title("Südwestbank");
                                                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                thisRow.iconId = images[19];
                                                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.suedwestbank_logo_final));
                                                                                                            } else {
                                                                                                                if (placeName.toLowerCase().contains("pax")) {
                                                                                                                    markerOptions.title("Pax-Bank");
                                                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                    thisRow.iconId = images[6];
                                                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                                                                                } else {
                                                                                                                    markerOptions.title(placeName);
                                                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                    thisRow.iconId = R.drawable.ic_new_general_map_marker3;
                                                                                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3));
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                            toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();

                                            // Add Marker to map
                                            mMap.addMarker(markerOptions);

                                            // Add to ListView
                                            thisRow.rowTitle = markerOptions.getTitle();
                                            thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                            toCacheElement.currentAtm = thisRow;
                                            data.add(thisRow);
                                            toCache.add(toCacheElement);
                                            Collections.sort(data, new CompareDistancesOnDisplayList());
                                            Collections.sort(toCache, new CompareDistanceOnCacheList());
                                            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                                        }
                                    } else {
                                        if (googlePlace.getLat() != null && googlePlace.getLon() != null && googlePlace.getTags() != null) {

                                            if (i == 0) {
                                                Double locationToAtmDistance = Distance.distance1(Double.parseDouble(googlePlace.getLat()), latitude, Double.parseDouble(googlePlace.getLon()), longitude, 0, 0);
                                                if (locationToAtmDistance > 400) {

                                                    // Move map camera
                                                    LatLng latLng = new LatLng(latitude, longitude);
                                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                                } else {
                                                    LatLng latLng = new LatLng(latitude, longitude);
                                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                                }
                                            }

                                            String placeName = googlePlace.getTags().getName();
                                            toDisplay = BlackListFilter.isBlacklisted(placeName.toLowerCase());
                                            if (!toDisplay) {
                                                if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                    zaehler = zaehler + 1;
                                                }
                                            }

                                            // Calculates air distance between location & atm
                                            Double locationToAtmDistance = Distance.distance1(Double.parseDouble(googlePlace.getLat()), latitude, Double.parseDouble(googlePlace.getLon()), longitude, 0, 0);

                                            if (locationToAtmDistance > 1500) {
                                                toDisplay = false;
                                            }

                                            if (!placeName.toLowerCase().contains("bank")
                                                    && !placeName.toLowerCase().contains("kasse")
                                                    && !placeName.toLowerCase().contains("diba")
                                                    && !placeName.toLowerCase().contains("santander")
                                                    && !placeName.toLowerCase().contains("seb")) {
                                                toDisplay = false;
                                                if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                    zaehler = zaehler + 1;
                                                }
                                            }


                                            if (toDisplay) {
                                                MarkerOptions markerOptions = new MarkerOptions();
                                                AtmDataStructure toCacheElement = new AtmDataStructure();
                                                double lat = Double.parseDouble(googlePlace.getLat());
                                                double lng = Double.parseDouble(googlePlace.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;

                                                LatLng latLng = new LatLng(lat, lng);
                                                markerOptions.position(latLng);
                                                if (placeName.toLowerCase().contains("commerzbank")) {
                                                    markerOptions.title("Commerzbank");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = R.drawable.ic_new_commerzbank_map_marker;
                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
                                                } else {
                                                    if (placeName.toLowerCase().contains("sparkasse")) {
                                                        markerOptions.title(placeName);
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = images[11];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("deutsche")) {
                                                            markerOptions.title("Deutsche Bank");
                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                            thisRow.iconId = images[2];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("post")) {
                                                                markerOptions.title("Postbank");
                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                thisRow.iconId = images[7];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("volks")
                                                                        || (placeName.toLowerCase().contains("aachener"))
                                                                        || (placeName.toLowerCase().contains("bopfing"))
                                                                        || (placeName.toLowerCase().contains("brühl"))
                                                                        || (placeName.toLowerCase().contains("donau"))
                                                                        || (placeName.toLowerCase().contains("erfurter"))
                                                                        || (placeName.toLowerCase().contains("federsee bank"))
                                                                        || (placeName.toLowerCase().contains("frankenberger bank"))
                                                                        || (placeName.toLowerCase().contains("geno"))
                                                                        || (placeName.toLowerCase().contains("genossenschafts bank münchen"))
                                                                        || (placeName.toLowerCase().contains("gls"))
                                                                        || (placeName.toLowerCase().contains("unterlegäu"))
                                                                        || (placeName.toLowerCase().contains("kölner"))
                                                                        || (placeName.toLowerCase().contains("ievo"))
                                                                        || (placeName.toLowerCase().contains("liga"))
                                                                        || (placeName.toLowerCase().contains("märki"))
                                                                        || (placeName.toLowerCase().contains("münchener bank"))
                                                                        || (placeName.toLowerCase().contains("reiffeisen"))
                                                                        || (placeName.toLowerCase().contains("rv bank"))
                                                                        || (placeName.toLowerCase().contains("darlehenkasse"))
                                                                        || (placeName.toLowerCase().contains("spaar & kredit"))
                                                                        || (placeName.toLowerCase().contains("spaar&kredit"))
                                                                        || (placeName.toLowerCase().contains("spreewald"))
                                                                        || (placeName.toLowerCase().contains("vr bank"))
                                                                        || (placeName.toLowerCase().contains("waldecker"))
                                                                        || (placeName.toLowerCase().contains("team"))) {
                                                                    markerOptions.title("Volksbank Gruppe");
                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                    thisRow.iconId = images[13];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("bb")) {
                                                                        markerOptions.title("BBBank");
                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                        thisRow.iconId = images[0];
                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                                                    } else {
                                                                        if (placeName.toLowerCase().contains("hypo")) {
                                                                            markerOptions.title("HypoVereinsbank");
                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                            thisRow.iconId = images[4];
                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                                                        } else {
                                                                            if (placeName.toLowerCase().contains("psd")) {
                                                                                markerOptions.title("PSD Bank");
                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                thisRow.iconId = images[8];
                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.psd_bank_logo_final));
                                                                            } else {
                                                                                if (placeName.toLowerCase().contains("santander")) {
                                                                                    markerOptions.title("Santander");
                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                    thisRow.iconId = images[9];
                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.santander_logo_final));
                                                                                } else {
                                                                                    if (placeName.toLowerCase().contains("sparda")) {
                                                                                        markerOptions.title("Sparda-Bank");
                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                        thisRow.iconId = images[10];
                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparda_bank_logo_final));
                                                                                    } else {
                                                                                        if (placeName.toLowerCase().contains("targo")) {
                                                                                            markerOptions.title("TargoBank");
                                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                            thisRow.iconId = images[12];
                                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.targobank_logo_final));
                                                                                        } else {
                                                                                            if (placeName.toLowerCase().contains("apo")) {
                                                                                                markerOptions.title("Deutsche Apotheker und Ärzte Bank");
                                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                thisRow.iconId = images[14];
                                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.apotheker_und_aerztebank_logo_final));
                                                                                            } else {
                                                                                                if (placeName.toLowerCase().contains("degussa")) {
                                                                                                    markerOptions.title("Degussa Bank");
                                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                    thisRow.iconId = images[15];
                                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                                                                                } else {
                                                                                                    if (placeName.toLowerCase().contains("lbbw") || placeName.toLowerCase().contains("wüttemb")) {
                                                                                                        markerOptions.title("LBBW");
                                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                        thisRow.iconId = images[16];
                                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lb_bw_logo_final));
                                                                                                    } else {
                                                                                                        if (placeName.toLowerCase().contains("lbb") || placeName.toLowerCase().contains("landesbank berlin")) {
                                                                                                            markerOptions.title("Landesbank Berlin");
                                                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                            thisRow.iconId = images[17];
                                                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lbb_logo_final));
                                                                                                        } else {
                                                                                                            if (placeName.toLowerCase().contains("oldenburgische landesbank") || placeName.toLowerCase().contains("olb")) {
                                                                                                                markerOptions.title("Oldenburgische Landesbank");
                                                                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                thisRow.iconId = images[18];
                                                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.oldenburgische_landesbank_logo_final));
                                                                                                            } else {
                                                                                                                if (placeName.toLowerCase().contains("südwest")) {
                                                                                                                    markerOptions.title("Südwestbank");
                                                                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                    thisRow.iconId = images[19];
                                                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.suedwestbank_logo_final));
                                                                                                                } else {
                                                                                                                    if (placeName.toLowerCase().contains("pax")) {
                                                                                                                        markerOptions.title("Pax-Bank");
                                                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                        thisRow.iconId = images[6];
                                                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                                                                                    } else {
                                                                                                                        markerOptions.title(placeName);
                                                                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                                                                        thisRow.iconId = R.drawable.ic_new_general_map_marker3;
                                                                                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3));
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                                toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();

                                                // Add Marker to map
                                                mMap.addMarker(markerOptions);

                                                // Add to ListView
                                                thisRow.rowTitle = markerOptions.getTitle();
                                                thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                                toCacheElement.currentAtm = thisRow;
                                                data.add(thisRow);
                                                toCache.add(toCacheElement);
                                                Collections.sort(data, new CompareDistancesOnDisplayList());
                                                Collections.sort(toCache, new CompareDistanceOnCacheList());
                                                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                                            }
                                        }
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                            loadingProgressBar.setVisibility(View.GONE);
                            osmNearByAtms(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
                        }

                        @Override
                        public void onFailure(Call<MyOsmAtms> call, Throwable t) {

                        }
                    });
        } else {
            osmNearByAtms(mService, latitude, longitude, mMap, mActivity, mContext, images, adapter, data);
        }
    }

    public static void osmNearByAtms(APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final int[] images, final RVAdapter adapter, final LinkedList<RVRowInformation> data) {

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }
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

        if (cachedEntries!=null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude, 0, 0) < 151 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            mMap.clear();
            data.clear();
            adapter.notifyDataSetChanged();

            Log.i("SearchFor", "cached data is used");
            MarkerOptions mMarkerOptions = new MarkerOptions();
            AtmDataStructure firstEntry = cachedEntries.getFirst();

            Double locationToAtmDistance = Distance.distance1(firstEntry.mMarkerOptionLat, latitude, firstEntry.mMarkerOptionLng, longitude, 0, 0);
            if (locationToAtmDistance > 400) {

                //Move map camera
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            } else {
                LatLng latLng = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }

            for (AtmDataStructure entry : cachedEntries) {

                if (entry.mMarkerOptionsTitle.toLowerCase().contains("commerzbank")) {
                    mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
                } else {
                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("sparkasse")) {
                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                    } else {
                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche")) {
                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                        } else {
                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("post")) {
                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                            } else {
                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("volks")
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("aachener"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("bopfing"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("brühl"))
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
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("vr bank"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("waldecker"))
                                        || (entry.mMarkerOptionsTitle.toLowerCase().contains("team"))) {
                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                } else {
                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("bb")) {
                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                    } else {
                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("hypo")) {
                                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                        } else {
                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("psd")) {
                                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.psd_bank_logo_final));
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
                                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("apo")) {
                                                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.apotheker_und_aerztebank_logo_final));
                                                            } else {
                                                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("degussa")) {
                                                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                                                } else {
                                                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("lbbw") || entry.mMarkerOptionsTitle.toLowerCase().contains("wüttemb")) {
                                                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lb_bw_logo_final));
                                                                    } else {
                                                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("lbb") || entry.mMarkerOptionsTitle.toLowerCase().contains("landesbank berlin")) {
                                                                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lbb_logo_final));
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
                                                                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche")) {
                                                                                            mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                                                                        } else {
                                                                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("shell")) {
                                                                                                mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                                                            } else {
                                                                                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("ing")) {
                                                                                                    mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ing_logo_final));
                                                                                                } else {
                                                                                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("pax")) {
                                                                                                        mMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                                                                    } else {
                                                                                                        mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3));
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
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
                adapter.notifyDataSetChanged();
                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
            }

            // adapter.notifyDataSetChanged();

            // The function to display the data from the cache memory is to fast to display a loading bar. There for a fake one will be showed for 1,5-2 seconds.
            loadingProgressBar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }, 1000);

        } else {

            Log.i("osmNearByAtm", "Request sent");

            BoundingBox coordinates = calculateBoundingBox(latitude, longitude);

            String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:25];(node[amenity=atm](" + coordinates.latMin + "," + coordinates.lngMin + "," + coordinates.latMax + "," + coordinates.lngMax + ");way[amenity=atm](" + coordinates.latMin + "," + coordinates.lngMin + "," + coordinates.latMax + "," + coordinates.lngMax + ");relation[amenity=atm](" + coordinates.latMin + "," + coordinates.lngMin + "," + coordinates.latMax + "," + coordinates.lngMax + "););out%20body;%3E;out%20skel%20qt;";

            Log.i("Gnerated URL", url);

            mService.getNearByAtm(url)
                    .enqueue(new Callback<MyOsmAtms>() {
                        @Override
                        public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                            if (response.isSuccessful()) {

                                // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                                if (Objects.requireNonNull(response.body()).getElements().length != 0 && !data.isEmpty()) {

                                    int zaehler = Objects.requireNonNull(response.body()).getElements().length;

                                    if (zaehler > 10) {
                                        zaehler = 10;
                                    }


                                    for (int i = 0; i < zaehler; i++) {

                                        boolean toDisplay = true;
                                        Elements googlePlace = Objects.requireNonNull(response.body()).getElements()[i];
                                        RVRowInformation thisRow = new RVRowInformation();

                                        if (googlePlace.getLat() == null && googlePlace.getTags() != null && googlePlace.getTags().getOperator() != null) {

                                            String address = googlePlace.getTags().getAddrStreet() + " " + googlePlace.getTags().getAddrHousenumber() + " " + googlePlace.getTags().getAddrPostcode() + " " + googlePlace.getTags().getAddrCity();
                                            Log.i("Address", address);
                                            LatLng coordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            // Calculates air distance between location & atm
                                            Double locationToAtmDistance = Distance.distance1(coordinates.latitude, latitude, coordinates.longitude, longitude, 0, 0);

                                            // Check if distance is greater than 1500m, and hide all results that are farer than that.
                                            if (locationToAtmDistance > 1500) {
                                                toDisplay = false;
                                                if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                    zaehler = zaehler + 1;
                                                }
                                            }


                                            String placeName = googlePlace.getTags().getOperator();

                                            if (!placeName.toLowerCase().contains("pax")
                                                    && !placeName.toLowerCase().contains("diba")
                                                    && !placeName.toLowerCase().contains("deutsche")
                                                    && !placeName.toLowerCase().contains("post")
                                                    && !placeName.toLowerCase().contains("sparkasse")
                                                    && !placeName.toLowerCase().contains("sparkasse")
                                                    && !placeName.toLowerCase().contains("er bank")
                                                    && !placeName.toLowerCase().contains("bopfing")
                                                    && !placeName.toLowerCase().contains("donau")
                                                    && !placeName.toLowerCase().contains("federsee bank")
                                                    && !placeName.toLowerCase().contains("geno")
                                                    && !placeName.toLowerCase().contains("genossenschafts bank münchen")
                                                    && !placeName.toLowerCase().contains("gls")
                                                    && !placeName.toLowerCase().contains("unterlegäu")
                                                    && !placeName.toLowerCase().contains("ievo")
                                                    && !placeName.toLowerCase().contains("liga")
                                                    && !placeName.toLowerCase().contains("märki")
                                                    && !placeName.toLowerCase().contains("münchener bank")
                                                    && !placeName.toLowerCase().contains("reiffeisen")
                                                    && !placeName.toLowerCase().contains("rv bank")
                                                    && !placeName.toLowerCase().contains("darlehenkasse")
                                                    && !placeName.toLowerCase().contains("spaar & kredit")
                                                    && !placeName.toLowerCase().contains("spaar&kredit")
                                                    && !placeName.toLowerCase().contains("spreewald")
                                                    && !placeName.toLowerCase().contains("vr bank")
                                                    && !placeName.toLowerCase().contains("team")){
                                                toDisplay = false;
                                                if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                    zaehler = zaehler + 1;
                                                }
                                            }


                                            if (toDisplay) {
                                                MarkerOptions markerOptions = new MarkerOptions();
                                                AtmDataStructure toCacheElement = new AtmDataStructure();
                                                double lat = coordinates.latitude;
                                                double lng = coordinates.longitude;

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;

                                                LatLng latLng = new LatLng(lat, lng);
                                                markerOptions.position(latLng);
                                                markerOptions.title(placeName);


                                                if (placeName.toLowerCase().contains("deutsche")) {
                                                    markerOptions.title("Deutsche Bank");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = images[2];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                                } else {
                                                    if (placeName.toLowerCase().contains("ing")) {
                                                        markerOptions.title("ING DiBa");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = images[5];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ing_logo_final));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("pax")) {
                                                            markerOptions.title("Pax Bank");
                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                            thisRow.iconId = images[6];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("post")) {
                                                                markerOptions.title("Postbank");
                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                thisRow.iconId = images[7];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("sparkasse")) {
                                                                    markerOptions.title(placeName);
                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                    thisRow.iconId = images[11];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("volks")
                                                                            || (placeName.toLowerCase().contains("aachener"))
                                                                            || (placeName.toLowerCase().contains("bopfing"))
                                                                            || (placeName.toLowerCase().contains("brühl"))
                                                                            || (placeName.toLowerCase().contains("donau"))
                                                                            || (placeName.toLowerCase().contains("erfurter"))
                                                                            || (placeName.toLowerCase().contains("federsee bank"))
                                                                            || (placeName.toLowerCase().contains("frankenberger bank"))
                                                                            || (placeName.toLowerCase().contains("geno"))
                                                                            || (placeName.toLowerCase().contains("genossenschafts bank münchen"))
                                                                            || (placeName.toLowerCase().contains("gls"))
                                                                            || (placeName.toLowerCase().contains("unterlegäu"))
                                                                            || (placeName.toLowerCase().contains("kölner"))
                                                                            || (placeName.toLowerCase().contains("ievo"))
                                                                            || (placeName.toLowerCase().contains("liga"))
                                                                            || (placeName.toLowerCase().contains("märki"))
                                                                            || (placeName.toLowerCase().contains("münchener bank"))
                                                                            || (placeName.toLowerCase().contains("reiffeisen"))
                                                                            || (placeName.toLowerCase().contains("rv bank"))
                                                                            || (placeName.toLowerCase().contains("darlehenkasse"))
                                                                            || (placeName.toLowerCase().contains("spaar & kredit"))
                                                                            || (placeName.toLowerCase().contains("spaar&kredit"))
                                                                            || (placeName.toLowerCase().contains("spreewald"))
                                                                            || (placeName.toLowerCase().contains("vr bank"))
                                                                            || (placeName.toLowerCase().contains("waldecker"))
                                                                            || (placeName.toLowerCase().contains("team"))) {
                                                                        markerOptions.title("Volksbank Gruppe");
                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                        thisRow.iconId = images[13];
                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                                                    } else {
                                                                        thisRow.iconId = images[3];
                                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3));
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }


                                                toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                                toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();


                                                // In order to keep a logical display order, check if the distance to the atm is shorter than the last result from the bank search. If yes, add to list.
                                                if (Double.parseDouble(data.getLast().rowSubtitle) > locationToAtmDistance) {

                                                    // Add Marker to map
                                                    mMap.addMarker(markerOptions);
                                                    thisRow.rowTitle = markerOptions.getTitle();
                                                    thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                                    toCacheElement.currentAtm = thisRow;

                                                    data.add(thisRow);
                                                    toCache.add(toCacheElement);
                                                    Collections.sort(data, new CompareDistancesOnDisplayList());
                                                    Collections.sort(toCache, new CompareDistanceOnCacheList());


                                                }
                                            }
                                        } else {
                                            if (googlePlace.getLat() != null && googlePlace.getLon() != null && googlePlace.getTags() != null) {

                                                // Calculates air distance between location & atm
                                                Double locationToAtmDistance = Distance.distance1(Double.parseDouble(googlePlace.getLat()), latitude, Double.parseDouble(googlePlace.getLon()), longitude, 0, 0);

                                                // Check if distance is greater than 1500m, and hide all results that are farer than that.
                                                if (locationToAtmDistance > 1500) {
                                                    toDisplay = false;
                                                    if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                        zaehler = zaehler + 1;
                                                    }
                                                }


                                                String placeName = googlePlace.getTags().getOperator();

                                                if (!placeName.toLowerCase().contains("pax")
                                                        && !placeName.toLowerCase().contains("diba")
                                                        && !placeName.toLowerCase().contains("deutsche")
                                                        && !placeName.toLowerCase().contains("post")
                                                        && !placeName.toLowerCase().contains("sparkasse")
                                                        && !placeName.toLowerCase().contains("sparkasse")
                                                        && !placeName.toLowerCase().contains("er bank")
                                                        && !placeName.toLowerCase().contains("bopfing")
                                                        && !placeName.toLowerCase().contains("donau")
                                                        && !placeName.toLowerCase().contains("federsee bank")
                                                        && !placeName.toLowerCase().contains("geno")
                                                        && !placeName.toLowerCase().contains("genossenschafts bank münchen")
                                                        && !placeName.toLowerCase().contains("gls")
                                                        && !placeName.toLowerCase().contains("unterlegäu")
                                                        && !placeName.toLowerCase().contains("ievo")
                                                        && !placeName.toLowerCase().contains("liga")
                                                        && !placeName.toLowerCase().contains("märki")
                                                        && !placeName.toLowerCase().contains("münchener bank")
                                                        && !placeName.toLowerCase().contains("reiffeisen")
                                                        && !placeName.toLowerCase().contains("rv bank")
                                                        && !placeName.toLowerCase().contains("darlehenkasse")
                                                        && !placeName.toLowerCase().contains("spaar & kredit")
                                                        && !placeName.toLowerCase().contains("spaar&kredit")
                                                        && !placeName.toLowerCase().contains("spreewald")
                                                        && !placeName.toLowerCase().contains("vr bank")
                                                        && !placeName.toLowerCase().contains("team")){
                                                    toDisplay = false;
                                                    if (Objects.requireNonNull(response.body().getElements().length) > 10 && zaehler < 20) {
                                                        zaehler = zaehler + 1;
                                                    }
                                                }


                                                if (toDisplay) {
                                                    MarkerOptions markerOptions = new MarkerOptions();
                                                    AtmDataStructure toCacheElement = new AtmDataStructure();
                                                    double lat = Double.parseDouble(googlePlace.getLat());
                                                    double lng = Double.parseDouble(googlePlace.getLon());

                                                    toCacheElement.mMarkerOptionLat = lat;
                                                    toCacheElement.mMarkerOptionLng = lng;

                                                    LatLng latLng = new LatLng(lat, lng);
                                                    markerOptions.position(latLng);
                                                    markerOptions.title(placeName);


                                                    if (placeName.toLowerCase().contains("deutsche")) {
                                                        markerOptions.title("Deutsche Bank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = images[2];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("ing")) {
                                                            markerOptions.title("ING DiBa");
                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                            thisRow.iconId = images[5];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ing_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("pax")) {
                                                                markerOptions.title("Pax Bank");
                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                thisRow.iconId = images[6];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("post")) {
                                                                    markerOptions.title("Postbank");
                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                    thisRow.iconId = images[7];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("sparkasse")) {
                                                                        markerOptions.title(placeName);
                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                        thisRow.iconId = images[11];
                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                                                    } else {
                                                                        if (placeName.toLowerCase().contains("volks")
                                                                                || (placeName.toLowerCase().contains("aachener"))
                                                                                || (placeName.toLowerCase().contains("bopfing"))
                                                                                || (placeName.toLowerCase().contains("brühl"))
                                                                                || (placeName.toLowerCase().contains("donau"))
                                                                                || (placeName.toLowerCase().contains("erfurter"))
                                                                                || (placeName.toLowerCase().contains("federsee bank"))
                                                                                || (placeName.toLowerCase().contains("frankenberger bank"))
                                                                                || (placeName.toLowerCase().contains("geno"))
                                                                                || (placeName.toLowerCase().contains("genossenschafts bank münchen"))
                                                                                || (placeName.toLowerCase().contains("gls"))
                                                                                || (placeName.toLowerCase().contains("unterlegäu"))
                                                                                || (placeName.toLowerCase().contains("kölner"))
                                                                                || (placeName.toLowerCase().contains("ievo"))
                                                                                || (placeName.toLowerCase().contains("liga"))
                                                                                || (placeName.toLowerCase().contains("märki"))
                                                                                || (placeName.toLowerCase().contains("münchener bank"))
                                                                                || (placeName.toLowerCase().contains("reiffeisen"))
                                                                                || (placeName.toLowerCase().contains("rv bank"))
                                                                                || (placeName.toLowerCase().contains("darlehenkasse"))
                                                                                || (placeName.toLowerCase().contains("spaar & kredit"))
                                                                                || (placeName.toLowerCase().contains("spaar&kredit"))
                                                                                || (placeName.toLowerCase().contains("spreewald"))
                                                                                || (placeName.toLowerCase().contains("vr bank"))
                                                                                || (placeName.toLowerCase().contains("waldecker"))
                                                                                || (placeName.toLowerCase().contains("team"))) {
                                                                            markerOptions.title("Volksbank Gruppe");
                                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                            thisRow.iconId = images[13];
                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                                                        } else {
                                                                            thisRow.iconId = images[3];
                                                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_general_map_marker3));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                                    toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();


                                                    // In order to keep a logical display order, check if the distance to the atm is shorter than the last result from the bank search. If yes, add to list.
                                                    if (Double.parseDouble(data.getLast().rowSubtitle) > locationToAtmDistance) {

                                                    // Add Marker to map
                                                    mMap.addMarker(markerOptions);
                                                    thisRow.rowTitle = markerOptions.getTitle();
                                                    thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                                    toCacheElement.currentAtm = thisRow;

                                                    data.add(thisRow);
                                                    toCache.add(toCacheElement);
                                                    Collections.sort(data, new CompareDistancesOnDisplayList());
                                                    Collections.sort(toCache, new CompareDistanceOnCacheList());
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Check if the result Object is empty
                                    if (data.isEmpty()) {

                                        // If empty show the user an alert
                                        CustomAlertDialog alert = new CustomAlertDialog();
                                        alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                                    } else try {

                                        // If not empty try and cache the data for later usage
                                        CacheData.writeAtmData(mContext, mContext.getString(R.string.KEY_for_atms), toCache);

                                        // Also save the time the data was cached in UNIX timestamp format
                                        long unixTime = System.currentTimeMillis() / 1000;
                                        CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_timestamp), unixTime);

                                        // Save the location where the data was chached
                                        CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_latitude), latitude);
                                        CacheData.writeObject(mContext, mContext.getString(R.string.KEY_for_longitude), longitude);

                                    } catch (IOException e) {
                                        Log.e(TAG, e.getMessage());
                                    }
                                } else {

                                    // Handle if no results were found
                                    loadingProgressBar.setVisibility(View.GONE);
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
                                }
                            }
                            loadingProgressBar.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {

                        }
                    });
        }
    }
}