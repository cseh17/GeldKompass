package com.atm_search.cseh_17.geld_kompass;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.atm_search.cseh_17.geld_kompass.ModelOSM.Elements;
import com.atm_search.cseh_17.geld_kompass.ModelOSM.MyOsmAtms;
import com.atm_search.cseh_17.geld_kompass.Remote.APIService;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.atm_search.cseh_17.geld_kompass.BitmapDescriptorFromVector.bitmapDescriptorFromVector;

class Filters {

    private static LinkedList<AtmDataStructure> cachedEntries;
    private static long lastSaved;
    private static double lat, lng;
    private static boolean showGeneralError = true;

    static boolean nearByBanksFilteredCashGroup(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        // Clear data and map in order to avoid doubles on list.
        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        Trace myTrace = FirebasePerformance.getInstance().newTrace("cashGroup_filter_cache");
        myTrace.start();

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

        if (cachedEntries != null && !cachedEntries.isEmpty()){
            myTrace.incrementMetric("cash_group_cache_hit", 1);
        } else {
            myTrace.incrementMetric("cash_group_cache_miss", 1);
        }
        myTrace.stop();

        if (cachedEntries != null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude) < 101 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            myTrace.incrementMetric("cash_group_cache_hit", 1);
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
                    if (isFirst) {

                        isFirst = false;
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude);
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
                            mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_hypo_marker));
                        } else {
                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("deutsche")) {
                                mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_deutsche_bank_marker));
                            } else {
                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("post")) {
                                    mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_postbank_marker));
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
            CustomSearchDistanceDialog alert_distance = new CustomSearchDistanceDialog();
            Dialog dialog = alert_distance.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
            osmFirstBankCashGroup(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog);
            loadingProgressBar.setVisibility(View.GONE);
            return true;
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

    static boolean nearByBanksFilteredCashPool(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        Trace myTrace = FirebasePerformance.getInstance().newTrace("cashPool_filter_cache");
        myTrace.start();

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

        if (cachedEntries != null && !cachedEntries.isEmpty()){
            myTrace.incrementMetric("cash_pool_cache_hit", 1);
        } else {
            myTrace.incrementMetric("cash_pool_cache_miss", 1);
        }
        myTrace.stop();

        if (cachedEntries!=null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude) < 101 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

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
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude);
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
                                    mMarkerOptions.icon(bitmapDescriptorFromVector(mContext, R.drawable.ic_new_targobank_map_marker));
                                } else {
                                    if (entry.mMarkerOptionsTitle.toLowerCase().contains("degussa")) {
                                        mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_degussa_bank_marker));
                                    } else {
                                        if (entry.mMarkerOptionsTitle.toLowerCase().contains("oldenburgische landesbank") || entry.mMarkerOptionsTitle.toLowerCase().contains("olb")) {
                                            mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_olb_marker3));
                                        } else {
                                            if (entry.mMarkerOptionsTitle.toLowerCase().contains("südwest")) {
                                                mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_suedwestbank_marker2));
                                            } else {
                                                if (entry.mMarkerOptionsTitle.toLowerCase().contains("pax")) {
                                                    mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_pax_bank_marker2));
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

            //CustomAlertDialog alert = new CustomAlertDialog();
            //alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_cash_pool_DE));
            CustomSearchDistanceDialog alert_distance = new CustomSearchDistanceDialog();
            Dialog dialog = alert_distance.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
            osmFirstBankCashPool(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog);
            loadingProgressBar.setVisibility(View.GONE);
            return true;
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

    static boolean nearByBanksFilteredSparkasse(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter) {

        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();

        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        Trace myTrace = FirebasePerformance.getInstance().newTrace("sparkasse_filter_cache");
        myTrace.start();

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

        if (cachedEntries != null && !cachedEntries.isEmpty()){
            myTrace.incrementMetric("sparkasse_cache_hit", 1);
        } else {
            myTrace.incrementMetric("sparkasse_cache_miss", 1);
        }
        myTrace.stop();

        if (cachedEntries != null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude) < 101 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            Log.i("Sparkasse filter", "deployed");
            MarkerOptions mMarkerOptions = new MarkerOptions();

            boolean toDisplay;
            boolean isFirst = true;
            for (AtmDataStructure entry : cachedEntries) {

                toDisplay = entry.mMarkerOptionsTitle.toLowerCase().contains("sparkasse");

                if (toDisplay) {

                    if (isFirst){

                        isFirst = false;
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude);
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
                        mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_sparkasse_marker5));
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
            //CustomAlertDialog alert = new CustomAlertDialog();
            //alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_sparkasse_DE));
            CustomSearchDistanceDialog alert_distance = new CustomSearchDistanceDialog();
            Dialog dialog = alert_distance.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
            osmFirstBankSparkasse(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog);
            loadingProgressBar.setVisibility(View.GONE);
            return true;
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

    static boolean nearByBanksFilteredVolksbank(final GoogleMap mMap, final LinkedList<RVRowInformation> data, final APIService mService, final double latitude, final double longitude, final Context mContext, final Activity mActivity, final RVAdapter adapter)  {

        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.main_progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        Trace myTrace = FirebasePerformance.getInstance().newTrace("volksbankGroup_filter_cache");
        myTrace.start();

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

        if (cachedEntries != null && !cachedEntries.isEmpty()){
            myTrace.incrementMetric("volksbank_group_cache_hit", 1);
        } else {
            myTrace.incrementMetric("volksbank_group_cache_miss", 1);
        }
        myTrace.stop();

        if (cachedEntries != null && !cachedEntries.isEmpty() && Distance.distance1(lat, latitude, lng, longitude) < 101 && ((System.currentTimeMillis() / 1000) - lastSaved) < 600) {

            Log.i("Volksbank filter", "deployed");
            MarkerOptions mMarkerOptions = new MarkerOptions();

            boolean toDisplay;
            boolean isFirst = true;
            for (AtmDataStructure entry : cachedEntries) {

                toDisplay = CheckIfVolksbank.checkIfVolksbank(entry.mMarkerOptionsTitle);

                if (toDisplay) {

                    if (isFirst){

                        isFirst = false;
                        Double locationToAtmDistance = Distance.distance1(entry.mMarkerOptionLat, latitude, entry.mMarkerOptionLng, longitude);
                        if (locationToAtmDistance > 400) {

                            //Move map camera
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                        } else {
                            LatLng latLng = new LatLng(latitude, longitude);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }

                    }

                    mMarkerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_volksbank_gruppe_marker2));

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
            //CustomAlertDialog alert = new CustomAlertDialog();
            //alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_volksbank_DE));
            CustomSearchDistanceDialog alert_distance = new CustomSearchDistanceDialog();
            Dialog dialog = alert_distance.showDialog(mActivity, mContext.getString(R.string.no_result_alert_DE));
            osmFirstBankVolksbank(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog);
            loadingProgressBar.setVisibility(View.GONE);
            return true;
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

    @AddTrace(name = "osmFirstBankCashGroup")
    private static void osmFirstBankCashGroup(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog) {

        // Clear data and map in order to avoid doubles on list.
        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstBankCashGroup", "Request sent");

        // Check if the location is inside a big city. If yes, set the search-radius to 5000m, otherwise to 30000m.
        final int distance;

        if (CheckIfGrossstadt.checkIfGrossstadt(GetCityNameFromCoordinates.getCityName(latitude, longitude, mContext))){
            distance = 2500;
        } else {
            if (CheckIfNearGrossstadt.checkIfNearGrossstadt(latitude, longitude)) {
                distance = 5000;
            } else {
                distance = 20000;
            }
        }

        final LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;

        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude+ ");way[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Gnerated URL", url);

        mService.getNearByBank(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {
                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }


                                // Access the progressBar from the custom dialog, and configure it
                                ProgressBar mProgressBar = dialog.findViewById(R.id.dialog_progresLoader);
                                mProgressBar.setVisibility(View.VISIBLE);
                                mProgressBar.setMax(editedResponse.size());
                                int progressStatus = 0;

                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getName() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String houseNumber;
                                        if (item.getTags().getAddrHousenumber().contains("-")) {
                                            houseNumber = item.getTags().getAddrHousenumber().substring(0, item.getTags().getAddrHousenumber().indexOf("-"));
                                        } else {
                                            houseNumber = item.getTags().getAddrHousenumber();
                                        }

                                        String address = item.getTags().getAddrStreet() + " " + houseNumber + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {
                                        if (item.getTags().getName() == null) {
                                            item.setIsValid(false);
                                        } else {

                                            // Check if the elements are backlisted, or part of the Cashroup
                                            item.setIsValid(BlackListFilter.isBlacklisted(item.getTags().getName()));

                                            if (!item.getTags().getName().toLowerCase().contains("commerz")
                                                    && !item.getTags().getName().toLowerCase().contains("deutsche bank")
                                                    && !item.getTags().getName().toLowerCase().contains("hypo")
                                                    && !item.getTags().getName().toLowerCase().contains("post")) {
                                                item.setIsValid(false);
                                            }
                                        }
                                    }
                                    progressStatus++;
                                    mProgressBar.setProgress(progressStatus);
                                }

                                // Sort the edited response list by distance from actual location
                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse) {
                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Log.i("First element", filteredResponse.getFirst().getTags().getName());

                                    Elements theFirst = filteredResponse.getFirst();
                                    if (theFirst.getIsValid()) {
                                        String placeName = theFirst.getTags().getName();
                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 400 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }


                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null && theFirst.getTags() != null) {


                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);
                                        if (placeName.toLowerCase().contains("commerzbank")) {
                                            markerOptions.title("Commerzbank");
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_commerzbank_map_marker;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
                                        } else {
                                            if (placeName.toLowerCase().contains("deutsche")) {
                                                markerOptions.title("Deutsche Bank");
                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                thisRow.iconId = R.drawable.ic_new_deutsche_bank_marker;
                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_deutsche_bank_marker));
                                            } else {
                                                if (placeName.toLowerCase().contains("post")) {
                                                    markerOptions.title("Postbank");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = R.drawable.ic_new_postbank_marker;
                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_postbank_marker));
                                                } else {
                                                    if (placeName.toLowerCase().contains("hypo")) {
                                                        markerOptions.title("HypoVereinsbank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = R.drawable.ic_new_hypo_marker;
                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_hypo_marker));
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
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;
                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        osmFirstAtmCashGroup(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstBankCashGroup", "timeout");
                        if (t instanceof SocketTimeoutException){
                            adapter.notifyDataSetChanged();
                            osmFirstAtmCashGroup(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstBankCashGroupDistance-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstBankCashGroupDistance_load_data_miss", 1);
                    }
                });
    }

    @AddTrace(name = "osmFirstAtmCashGroup")
    private static void osmFirstAtmCashGroup(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog, int distance) {

        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstAtmCashGroup", "Request sent");
        LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;
        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");way[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Gnerated URL", url);

        mService.getNearByAtm(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {

                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }

                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getOperator() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String address = item.getTags().getAddrStreet() + " " + item.getTags().getAddrHousenumber() + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {

                                        if (item.getTags().getOperator() == null) {

                                            item.setIsValid(false);
                                        } else {

                                            String placeName = item.getTags().getOperator();

                                            if (!placeName.toLowerCase().contains("deutsche bank")
                                                    && !placeName.toLowerCase().contains("post")
                                                    && !placeName.toLowerCase().contains("commerz")
                                                    && !placeName.toLowerCase().contains("hypo")) {
                                                item.setIsValid(false);
                                            }
                                        }
                                    }
                                }

                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse) {

                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Elements theFirst = filteredResponse.getFirst();

                                    // Get the first result from the checked List and do other jobs
                                    if (theFirst.getIsValid()) {

                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 2000 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }

                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null && theFirst.getTags().getAddrStreet() != null && theFirst.getTags().getAddrHousenumber() != null && theFirst.getTags().getAddrPostcode() != null && theFirst.getTags().getAddrCity() != null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null) {

                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        String placeName = theFirst.getTags().getOperator();
                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);
                                        markerOptions.title(placeName);

                                        if (placeName.toLowerCase().contains("deutsche")) {
                                            markerOptions.title("Deutsche Bank");
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_deutsche_bank_marker;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_deutsche_bank_marker));
                                        } else {
                                            if (placeName.toLowerCase().contains("post")) {
                                                markerOptions.title("Postbank");
                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                thisRow.iconId = R.drawable.ic_new_postbank_marker;
                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_postbank_marker));
                                            } else {
                                                if (placeName.toLowerCase().contains("commerzbank")) {
                                                    markerOptions.title("Commerzbank");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = R.drawable.ic_new_commerzbank_map_marker;
                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_commerzbank_map_marker));
                                                } else {
                                                    if (placeName.toLowerCase().contains("hypo")) {
                                                        markerOptions.title("HypoVereinsbank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = R.drawable.ic_new_hypo_marker;
                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_hypo_marker));
                                                    }
                                                }
                                            }
                                        }

                                        toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                        toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();


                                        // Add Marker to map
                                        mMap.addMarker(markerOptions);
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;

                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                    }

                                    adapter.notifyDataSetChanged();
                                } else {
                                    if (data.isEmpty()){

                                        // Handle if no results were found
                                        CustomAlertDialog alert = new CustomAlertDialog();
                                        alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_cash_group_DE));
                                    }
                                }
                            } else {

                                if (data.isEmpty()) {

                                    // Handle if no results were found
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_cash_group_DE));
                                }
                            }

                            Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor_save_cache");
                            myTrace.start();

                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstAtmCashGroup", "timeout");
                        if (t instanceof SocketTimeoutException){
                            dialog.dismiss();
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstAtmCashGroupDistance-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstAtmCashGroupDistance_load_data_miss", 1);

                    }
                });
    }

    @AddTrace(name = "osmFirstBankCashPool")
    private static void osmFirstBankCashPool(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog){

        // Clear data and map in order to avoid doubles on list.
        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstBankCashPool", "Request sent");

        // Check if the location is inside a big city. If yes, set the search-radius to 5000m, otherwise to 30000m.
        final int distance;

        if (CheckIfGrossstadt.checkIfGrossstadt(GetCityNameFromCoordinates.getCityName(latitude, longitude, mContext))){
            distance = 3000;
        } else {
            if (CheckIfNearGrossstadt.checkIfNearGrossstadt(latitude, longitude)) {
                distance = 5000;
            } else {
                distance = 25000;
            }
        }

        final LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;

        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude+ ");way[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Generated URL", url);

        mService.getNearByBank(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                final LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {
                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }

                                // Access the progressBar from the custom dialog, and configure it


                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getName() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String houseNumber;
                                        if (item.getTags().getAddrHousenumber().contains("-")) {
                                            houseNumber = item.getTags().getAddrHousenumber().substring(0, item.getTags().getAddrHousenumber().indexOf("-"));
                                        } else {
                                            houseNumber = item.getTags().getAddrHousenumber();
                                        }

                                        String address = item.getTags().getAddrStreet() + " " + houseNumber + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {
                                        if (item.getTags().getName() == null) {
                                            item.setIsValid(false);
                                        } else {
                                            item.setIsValid(BlackListFilter.isBlacklisted(item.getTags().getName()));

                                            if (!item.getTags().getName().toLowerCase().contains("bbb")
                                                    && !item.getTags().getName().toLowerCase().contains("degussa")
                                                    && !item.getTags().getName().toLowerCase().contains("national")
                                                    && !item.getTags().getName().toLowerCase().contains("pax")
                                                    && !item.getTags().getName().toLowerCase().contains("santan")
                                                    && !item.getTags().getName().toLowerCase().contains("sparda")
                                                    && !item.getTags().getName().toLowerCase().contains("südwest")
                                                    && !item.getTags().getName().toLowerCase().contains("targo")
                                                    && !item.getTags().getName().toLowerCase().contains("oldenburgische landesbank")
                                                    && !item.getTags().getName().toLowerCase().contains("olb")) {
                                                item.setIsValid(false);
                                            }
                                        }
                                    }
                                }

                                // Sort the edited response list by distance from actual location
                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse) {

                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Log.i("First element", filteredResponse.getFirst().getTags().getName());

                                    Elements theFirst = filteredResponse.getFirst();
                                    if (theFirst.getIsValid()) {
                                        String placeName = theFirst.getTags().getName();
                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 400 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }


                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null && theFirst.getTags() != null) {


                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);

                                        if (placeName.toLowerCase().contains("bb")) {
                                            markerOptions.title("BBBank");
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_bbbank_marker;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_bbbank_marker));
                                        } else {
                                            if (placeName.toLowerCase().contains("santander")) {
                                                markerOptions.title("Santander");
                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                thisRow.iconId = R.drawable.ic_new_santander_marker;
                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_santander_marker));
                                            } else {
                                                if (placeName.toLowerCase().contains("sparda")) {
                                                    markerOptions.title("Sparda-Bank");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = R.drawable.ic_new_sparda_bank_marker5;
                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_sparda_bank_marker5));
                                                } else {
                                                    if (placeName.toLowerCase().contains("targo")) {
                                                        markerOptions.title("TargoBank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = R.drawable.ic_new_targobank_map_marker;
                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_targobank_map_marker));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("degussa")) {
                                                            markerOptions.title("Degussa Bank");
                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                            thisRow.iconId = R.drawable.ic_new_degussa_bank_marker;
                                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_degussa_bank_marker));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("oldenburgische landesbank") || placeName.toLowerCase().contains("olb")) {
                                                                markerOptions.title("Oldenburgische Landesbank");
                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                thisRow.iconId = R.drawable.ic_new_olb_marker3;
                                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_olb_marker3));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("südwest")) {
                                                                    markerOptions.title("Südwestbank");
                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                    thisRow.iconId = R.drawable.ic_new_suedwestbank_marker2;
                                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_suedwestbank_marker2));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("pax")) {
                                                                        markerOptions.title("Pax-Bank");
                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                        thisRow.iconId = R.drawable.ic_new_pax_bank_marker2;
                                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_pax_bank_marker2));
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
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;
                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // If no result was found, aka data is empty, do search for the category atm.
                        if (data.isEmpty()) {
                            osmFirstAtmCashPool(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                        } else {
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstBankCashPool", "timeout");
                        if (t instanceof SocketTimeoutException){
                            adapter.notifyDataSetChanged();

                            // If no result was found, aka data is empty, do search for the category atm.
                            if (data.isEmpty()) {
                                osmFirstAtmCashPool(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                            } else {
                                dialog.dismiss();
                            }
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstBankCashPoolDistance-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstBankCashPoolDistance_load_data_miss", 1);
                    }
                });
    }

    @AddTrace(name = "osmFirstAtmCashPool")
    private static void osmFirstAtmCashPool(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog, int distance) {

        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstAtmCashPool", "Request sent");
        LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;
        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");way[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Gnerated URL", url);

        mService.getNearByAtm(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {

                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }

                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getOperator() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String address = item.getTags().getAddrStreet() + " " + item.getTags().getAddrHousenumber() + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {

                                        if (item.getTags().getOperator() == null) {

                                            item.setIsValid(false);
                                        } else {

                                            String placeName = item.getTags().getOperator();

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
                                                item.setIsValid(false);
                                            }
                                        }
                                    }
                                }

                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse){

                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Elements theFirst = filteredResponse.getFirst();

                                    // Get the first result from the checked List and do other jobs
                                    if (theFirst.getIsValid()) {

                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 2000 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }

                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null && theFirst.getTags().getAddrStreet() != null && theFirst.getTags().getAddrHousenumber() != null && theFirst.getTags().getAddrPostcode() != null && theFirst.getTags().getAddrCity() != null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null) {

                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        String placeName = theFirst.getTags().getOperator();
                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);
                                        markerOptions.title(placeName);

                                        if (placeName.toLowerCase().contains("bb")) {
                                            markerOptions.title("BBBank");
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_bbbank_marker;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_bbbank_marker));
                                        } else {
                                            if (placeName.toLowerCase().contains("santander")) {
                                                markerOptions.title("Santander");
                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                thisRow.iconId = R.drawable.ic_new_santander_marker;
                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_santander_marker));
                                            } else {
                                                if (placeName.toLowerCase().contains("sparda")) {
                                                    markerOptions.title("Sparda-Bank");
                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                    thisRow.iconId = R.drawable.ic_new_sparda_bank_marker5;
                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_sparda_bank_marker5));
                                                } else {
                                                    if (placeName.toLowerCase().contains("targo")) {
                                                        markerOptions.title("TargoBank");
                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                        thisRow.iconId = R.drawable.ic_new_targobank_map_marker;
                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_targobank_map_marker));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("degussa")) {
                                                            markerOptions.title("Degussa Bank");
                                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                            thisRow.iconId = R.drawable.ic_new_degussa_bank_marker;
                                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_degussa_bank_marker));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("oldenburgische landesbank") || placeName.toLowerCase().contains("olb")) {
                                                                markerOptions.title("Oldenburgische Landesbank");
                                                                markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                thisRow.iconId = R.drawable.ic_new_olb_marker3;
                                                                markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_olb_marker3));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("südwest")) {
                                                                    markerOptions.title("Südwestbank");
                                                                    markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                    thisRow.iconId = R.drawable.ic_new_suedwestbank_marker2;
                                                                    markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_suedwestbank_marker2));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("pax")) {
                                                                        markerOptions.title("Pax-Bank");
                                                                        markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                                                        thisRow.iconId = R.drawable.ic_new_pax_bank_marker2;
                                                                        markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_pax_bank_marker2));
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
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;

                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                    }

                                    adapter.notifyDataSetChanged();
                                } else {

                                    // Handle if no results were found
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_cash_pool_DE));
                                }
                            } else {

                                if (data.isEmpty()) {

                                    // Handle if no results were found
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_cash_pool_DE));
                                }
                            }

                            Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor_save_cache");
                            myTrace.start();

                        }

                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstAtmCashPool", "timeout");
                        if (t instanceof SocketTimeoutException){
                            dialog.dismiss();
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstAtmCashPool-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstAtmCashPoolDistance_load_data_miss", 1);

                    }
                });
    }

    @AddTrace(name = "osmFirstBankSparkasse")
    private static void osmFirstBankSparkasse(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog){

        // Clear data and map in order to avoid doubles on list.
        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstBankSparkasse", "Request sent");

        // Check if the location is inside a big city. If yes, set the search-radius to 5000m, otherwise to 30000m.
        final int distance;

        if (CheckIfGrossstadt.checkIfGrossstadt(GetCityNameFromCoordinates.getCityName(latitude, longitude, mContext))){
            distance = 2000;
        } else {
            if (CheckIfNearGrossstadt.checkIfNearGrossstadt(latitude, longitude)) {
                distance = 4000;
            } else {
                distance = 10000;
            }
        }

        final LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;

        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude+ ");way[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Gnerated URL", url);

        mService.getNearByBank(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {
                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }

                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getName() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String houseNumber;
                                        if (item.getTags().getAddrHousenumber().contains("-")) {
                                            houseNumber = item.getTags().getAddrHousenumber().substring(0, item.getTags().getAddrHousenumber().indexOf("-"));
                                        } else {
                                            houseNumber = item.getTags().getAddrHousenumber();
                                        }

                                        String address = item.getTags().getAddrStreet() + " " + houseNumber + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {
                                        if (item.getTags().getName() == null) {
                                            item.setIsValid(false);
                                        } else {
                                            item.setIsValid(BlackListFilter.isBlacklisted(item.getTags().getName()));

                                            if (!item.getTags().getName().toLowerCase().contains("kasse")) {
                                                item.setIsValid(false);
                                            }
                                        }
                                    }
                                }

                                // Sort the edited response list by distance from actual location
                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse) {

                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Log.i("First element", filteredResponse.getFirst().getTags().getName());

                                    Elements theFirst = filteredResponse.getFirst();
                                    if (theFirst.getIsValid()) {
                                        String placeName = theFirst.getTags().getName();
                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 400 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }


                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null && theFirst.getTags() != null) {


                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);

                                        if (placeName.toLowerCase().contains("sparkasse")) {
                                            markerOptions.title(placeName);
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_sparkasse_marker5;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_sparkasse_marker5));
                                        }
                                        toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                        toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();

                                        // Add Marker to map
                                        mMap.addMarker(markerOptions);

                                        // Add to ListView
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;
                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        osmFirstAtmSparkasse(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstBankSparkasse", "timeout");
                        if (t instanceof SocketTimeoutException){
                            adapter.notifyDataSetChanged();
                            osmFirstAtmSparkasse(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstBankSparkasse-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstBankSparkasseDistance_load_data_miss", 1);
                    }
                });
    }

    @AddTrace(name = "osmFirstAtmSparkasse")
    private static void osmFirstAtmSparkasse(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog, int distance) {

        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstAtmSparkasse", "Request sent");
        LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;
        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");way[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Gnerated URL", url);

        mService.getNearByAtm(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {

                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }

                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getOperator() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String address = item.getTags().getAddrStreet() + " " + item.getTags().getAddrHousenumber() + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {

                                        if (item.getTags().getOperator() == null) {

                                            item.setIsValid(false);
                                        } else {

                                            String placeName = item.getTags().getOperator();

                                            if (!placeName.toLowerCase().contains("sparkasse")){
                                                item.setIsValid(false);
                                            }
                                        }
                                    }
                                }

                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse){

                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Elements theFirst = filteredResponse.getFirst();

                                    // Get the first result from the checked List and do other jobs
                                    if (theFirst.getIsValid()) {

                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 2000 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }

                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null && theFirst.getTags().getAddrStreet() != null && theFirst.getTags().getAddrHousenumber() != null && theFirst.getTags().getAddrPostcode() != null && theFirst.getTags().getAddrCity() != null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null) {

                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        String placeName = theFirst.getTags().getOperator();
                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);
                                        markerOptions.title(placeName);

                                        if (placeName.toLowerCase().contains("sparkasse")) {
                                            markerOptions.title(placeName);
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_sparkasse_marker5;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_sparkasse_marker5));
                                        }

                                        toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                        toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();


                                        // Add Marker to map
                                        mMap.addMarker(markerOptions);
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;

                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                    }

                                    adapter.notifyDataSetChanged();
                                } else {
                                    if (data.isEmpty()){

                                        // Handle if no results were found
                                        CustomAlertDialog alert = new CustomAlertDialog();
                                        alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_sparkasse_DE));
                                    }
                                }
                            } else {

                                if (data.isEmpty()) {

                                    // Handle if no results were found
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_sparkasse_DE));
                                }
                            }

                            Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor_save_cache");
                            myTrace.start();

                        }

                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstAtmSparkasse", "timeout");
                        if (t instanceof SocketTimeoutException){
                            dialog.dismiss();
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstAtmSparkasse-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstAtmSparkasseDistance_load_data_miss", 1);

                    }
                });
    }

    @AddTrace(name = "osmFirstBankVolksbank")
    private static void osmFirstBankVolksbank(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog){

        // Clear data and map in order to avoid doubles on list.
        mMap.clear();
        data.clear();
        adapter.notifyDataSetChanged();
        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstBankVolksbank", "Request sent");

        // Check if the location is inside a big city. If yes, set the search-radius to 5000m, otherwise to 30000m.
        final int distance;

        if (CheckIfGrossstadt.checkIfGrossstadt(GetCityNameFromCoordinates.getCityName(latitude, longitude, mContext))){
            distance = 2500;
        } else {
            if (CheckIfNearGrossstadt.checkIfNearGrossstadt(latitude, longitude)) {
                distance = 3500;
            } else {
                distance = 10000;
            }
        }

        final LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;

        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude+ ");way[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=bank](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Gnerated URL", url);

        mService.getNearByBank(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {
                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }

                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getName() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String houseNumber;
                                        if (item.getTags().getAddrHousenumber().contains("-")) {
                                            houseNumber = item.getTags().getAddrHousenumber().substring(0, item.getTags().getAddrHousenumber().indexOf("-"));
                                        } else {
                                            houseNumber = item.getTags().getAddrHousenumber();
                                        }

                                        String address = item.getTags().getAddrStreet() + " " + houseNumber + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {
                                        if (item.getTags().getName() == null) {
                                            item.setIsValid(false);
                                        } else {
                                            item.setIsValid(BlackListFilter.isBlacklisted(item.getTags().getName()));
                                            item.setIsValid(CheckIfVolksbank.checkIfVolksbank(item.getTags().getName()));
                                        }
                                    }
                                }

                                // Sort the edited response list by distance from actual location
                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse) {

                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Log.i("First element", filteredResponse.getFirst().getTags().getName());

                                    Elements theFirst = filteredResponse.getFirst();
                                    if (theFirst.getIsValid()) {
                                        String placeName = theFirst.getTags().getName();
                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 400 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }


                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null && theFirst.getTags() != null) {


                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);

                                        if (CheckIfVolksbank.checkIfVolksbank(placeName.toLowerCase())) {
                                            markerOptions.title("Volksbank Gruppe");
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_volksbank_gruppe_marker2;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_volksbank_gruppe_marker2));
                                        }

                                        toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                        toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();

                                        // Add Marker to map
                                        mMap.addMarker(markerOptions);

                                        // Add to ListView
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;
                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        osmFirstAtmVolksbank(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstBankVolksbank", "timeout");
                        if (t instanceof SocketTimeoutException){
                            adapter.notifyDataSetChanged();
                            osmFirstAtmVolksbank(mService, latitude, longitude, mMap, mActivity, mContext, adapter, data, dialog, distance);
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstBankVolksbankDistance-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstBankVolksbankDistance_load_data_miss", 1);
                    }
                });
    }

    @AddTrace(name = "osmFirstAtmVolksbank")
    private static void osmFirstAtmVolksbank(final APIService mService, final double latitude, final double longitude, final GoogleMap mMap, final Activity mActivity, final Context mContext, final RVAdapter adapter, final LinkedList<RVRowInformation> data, final Dialog dialog, int distance) {

        if (cachedEntries !=null) {
            cachedEntries.clear();
        }

        Log.i("osmFirstAtmVolksbank", "Request sent");
        LatLngBounds coordinates = getBoundingBox(distance, latitude, longitude);
        showGeneralError = true;
        String url = "http://overpass-api.de/api/interpreter?data=[out:json][timeout:10];(node[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");way[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + ");relation[amenity=atm](" + coordinates.southwest.latitude + "," + coordinates.southwest.longitude + "," + coordinates.northeast.latitude + "," + coordinates.northeast.longitude + "););out%20body;%3E;out%20skel%20qt;";
        Log.i("Gnerated URL", url);

        mService.getNearByAtm(url)
                .enqueue(new Callback<MyOsmAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyOsmAtms> call, @NonNull Response<MyOsmAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getElements().length != 0) {

                                // Create new list, calculate distance to each point, and add them to the list to be sorted.
                                LinkedList<Elements> editedResponse = new LinkedList<>();
                                LinkedList<Elements> filteredResponse = new LinkedList<>();

                                for (Elements item : Objects.requireNonNull(response.body()).getElements()) {

                                    if (item.getTags() != null) {
                                        editedResponse.add(item);
                                    }
                                }

                                for (Elements item : editedResponse) {

                                    item.setIsValid(true);
                                    if (item.getLat() == null && item.getTags().getOperator() != null && item.getTags().getAddrStreet() != null && item.getTags().getAddrHousenumber() != null && item.getTags().getAddrPostcode() != null && item.getTags().getAddrCity() != null) {

                                        String address = item.getTags().getAddrStreet() + " " + item.getTags().getAddrHousenumber() + " " + item.getTags().getAddrPostcode() + " " + item.getTags().getAddrCity();
                                        Log.i("Address", address);
                                        LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);
                                        if (addressCoordinates != null) {
                                            item.setDistance(Distance.distance1(addressCoordinates.latitude, latitude, addressCoordinates.longitude, longitude));
                                        } else {
                                            if (showGeneralError) {
                                                CustomAlertDialog alert = new CustomAlertDialog();
                                                alert.showDialog(mActivity, mContext.getString(R.string.general_error));
                                                showGeneralError = false;
                                            }
                                        }
                                    } else {
                                        if (item.getLat() != null && item.getLon() != null) {

                                            item.setDistance(Distance.distance1(Double.parseDouble(item.getLat()), latitude, Double.parseDouble(item.getLon()), longitude));
                                        }
                                    }

                                    if (item.getDistance() == 0) {
                                        item.setIsValid(false);
                                    } else {

                                        if (item.getTags().getOperator() == null) {

                                            item.setIsValid(false);
                                        } else {

                                            String placeName = item.getTags().getOperator();
                                            item.setIsValid(CheckIfVolksbank.checkIfVolksbank(placeName));
                                        }
                                    }
                                }

                                Collections.sort(editedResponse, new CompareDistanceOnEditedList());

                                for (Elements item : editedResponse){

                                    if (item.getIsValid() && !item.getType().equals("way")) {
                                        filteredResponse.add(item);
                                    }
                                }

                                // Check if the filteredResponse list is not empty
                                if (filteredResponse.size() > 0) {
                                    Elements theFirst = filteredResponse.getFirst();

                                    // Get the first result from the checked List and do other jobs
                                    if (theFirst.getIsValid()) {

                                        RVRowInformation thisRow = new RVRowInformation();

                                        // Check if the first element is closer than 2000 or further, and adjust the map zoom accordingly
                                        if (theFirst.getDistance() > 2000) {

                                            // Move map camera
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                                        } else {
                                            LatLng latLng = new LatLng(latitude, longitude);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                                        }

                                        double lat = 0;
                                        double lng = 0;
                                        MarkerOptions markerOptions = new MarkerOptions();
                                        AtmDataStructure toCacheElement = new AtmDataStructure();

                                        if (theFirst.getLat() == null && theFirst.getTags().getAddrStreet() != null && theFirst.getTags().getAddrHousenumber() != null && theFirst.getTags().getAddrPostcode() != null && theFirst.getTags().getAddrCity() != null) {

                                            String address = theFirst.getTags().getAddrStreet() + " " + theFirst.getTags().getAddrHousenumber() + " " + theFirst.getTags().getAddrPostcode() + " " + theFirst.getTags().getAddrCity();
                                            LatLng addressCoordinates = AddressDecoder.getLocationFromAddress(mContext, address);

                                            lat = Objects.requireNonNull(addressCoordinates).latitude;
                                            lng = Objects.requireNonNull(addressCoordinates).longitude;

                                            toCacheElement.mMarkerOptionLat = lat;
                                            toCacheElement.mMarkerOptionLng = lng;
                                        } else {
                                            if (theFirst.getLat() != null && theFirst.getLon() != null) {

                                                lat = Double.parseDouble(theFirst.getLat());
                                                lng = Double.parseDouble(theFirst.getLon());

                                                toCacheElement.mMarkerOptionLat = lat;
                                                toCacheElement.mMarkerOptionLng = lng;
                                            }
                                        }

                                        String placeName = theFirst.getTags().getOperator();
                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);
                                        markerOptions.title(placeName);

                                        if (CheckIfVolksbank.checkIfVolksbank(placeName.toLowerCase())) {
                                            markerOptions.title("Volksbank Gruppe");
                                            markerOptions.snippet(CoordinatesDecoder.getCompleteAddress(mContext, lat, lng));
                                            thisRow.iconId = R.drawable.ic_new_volksbank_gruppe_marker2;
                                            markerOptions.icon(bitmapDescriptorFromVector(mActivity, R.drawable.ic_new_volksbank_gruppe_marker2));
                                        }

                                        toCacheElement.mMarkerOptionsTitle = markerOptions.getTitle();
                                        toCacheElement.mMarkerOptionSnippet = markerOptions.getSnippet();


                                        // Add Marker to map
                                        mMap.addMarker(markerOptions);
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", theFirst.getDistance());
                                        toCacheElement.currentAtm = thisRow;

                                        data.add(thisRow);
                                        Collections.sort(data, new CompareDistancesOnDisplayList());
                                    }

                                    adapter.notifyDataSetChanged();
                                } else {
                                    if (data.isEmpty()){

                                        // Handle if no results were found
                                        CustomAlertDialog alert = new CustomAlertDialog();
                                        alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_volksbank_DE));
                                    }
                                }
                            } else {

                                if (data.isEmpty()) {

                                    // Handle if no results were found
                                    CustomAlertDialog alert = new CustomAlertDialog();
                                    alert.showDialog(mActivity, mContext.getString(R.string.no_result_alert_volksbank_DE));
                                }
                            }

                            Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor_save_cache");
                            myTrace.start();

                        }

                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(@NonNull Call<MyOsmAtms> call, @NonNull Throwable t) {
                        Log.i("osmFirstAtmVolksbank", "timeout");
                        if (t instanceof SocketTimeoutException){
                            dialog.dismiss();
                        }
                        Trace myTrace = FirebasePerformance.getInstance().newTrace("SearchFor-osmFirstAtmVolksbankDistance-fails");
                        myTrace.start();
                        myTrace.incrementMetric("osmFirstAtmVolksbankDistance_load_data_miss", 1);

                    }
                });
    }

    private static LatLngBounds getBoundingBox( double distance, double latitude, double longitude){

        LatLng center = new LatLng(latitude, longitude);

        return new LatLngBounds.Builder().
                include(SphericalUtil.computeOffset(center, distance, 0)).
                include(SphericalUtil.computeOffset(center, distance, 90)).
                include(SphericalUtil.computeOffset(center, distance, 180)).
                include(SphericalUtil.computeOffset(center,distance, 270)).build();
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