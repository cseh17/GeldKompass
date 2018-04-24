package com.atm_search.cseh_17.atm_search;

import android.Manifest;
import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.atm_search.cseh_17.atm_search.Model.MyAtms;
import com.atm_search.cseh_17.atm_search.Model.Results;
import com.atm_search.cseh_17.atm_search.Remote.GoogleAPIService;
import com.atm_search.cseh_17.atm_search.BlackListFilter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


// Activity that displays a map showing the place at the device's current location
public class MainMapAtmDisplay extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    //private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private double latitude, longitude;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Marker mCurrentLocationMarker;
    SupportMapFragment mapFragment;
    FusedLocationProviderClient mFusedLocationClient;
    GoogleAPIService mService;
    private LatLngBounds allowedBoundsGermany = new LatLngBounds(new LatLng( 47.2701115, 5.8663425), new LatLng(55.0815,15.0418962));
    private RecyclerView recyclerView;
    LinkedList<RVRowInformation> data = new LinkedList<>(Arrays.asList(new RVRowInformation()));
    int[] images = {R.drawable.bbbank_logo_final, R.drawable.commerzbank_logo_final, R.drawable.deutschebank_logo_final, R.drawable.generic_logo_final, R.drawable.hypo_logo_final, R.drawable.ing_logo_final, R.drawable.paxbank_logo_final, R.drawable.postbank_logo_final, R.drawable.psd_bank_logo_final, R.drawable.santander_logo_final, R.drawable.sparda_bank_logo_final, R.drawable.sparkasse_logo_final, R.drawable.targobank_logo_final, R.drawable.volksbank_logo_final};
    RVAdapter adapter;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data.remove(0);
        // Retrieve the content view that renders the map
        setContentView(R.layout.activity_main_map_atm_display);

        // Initialise RecyclerView
        recyclerView = findViewById(R.id.rv_list_items);
        adapter = new RVAdapter(this, data);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.GONE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        TabLayout tabLayout = findViewById(R.id.tabs);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialise Service
        mService = Common.getGooglePIService();


        // Request Runtime permission
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //checkLocationPermission();
        //}

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0:
                        recyclerView.setVisibility(View.GONE);
                        mapFragment.getView().setVisibility(View.VISIBLE);
                        //Log.i("Ausgeführt in: ", "map-tab");
                        //if (mMap != null){
                            //nearByAtms("atm");
                            //nearByBanks();
                        //}
                        break;
                    case 1:
                        mapFragment.getView().setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        break;

                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        View navToolbarButtons = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("4"));

        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) navToolbarButtons.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        rlp.setMargins(0,30,30,0);

        FloatingActionButton floatingmyLocationButton = findViewById(R.id.myLocationButton);
        floatingmyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMapReady(mMap);

            }
        });

    }

    private String getUrlBank(double latitude, double longitude, String atm) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location="+latitude+","+longitude).append("&rankby=distance").append("&type="+atm).append("&sensor=true").append("&key="+getResources().getString(R.string.browser_key));
        Log.i("getUrl", googlePlacesUrl.toString());

        return googlePlacesUrl.toString();
    }

    private String getUrlAtm(double latitude, double longitude, String atm) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location="+latitude+","+longitude).append("&rankby=distance").append("&keyword="+atm).append("&sensor=true").append("&key="+getResources().getString(R.string.browser_key));
        //googlePlacesUrl.append("&type=atm");
        Log.i("getUrl", googlePlacesUrl.toString());

        return googlePlacesUrl.toString();
    }

    // Function to search and display atms.
    private void nearByAtms(final String atmName) {
        final ProgressBar loadingProgressBar = findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        String url = getUrlAtm(latitude, longitude, atmName);

        mService.getNearByPoi(url)
                .enqueue(new Callback<MyAtms>() {
                    @Override
                    public void onResponse(Call<MyAtms> call, Response<MyAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (response.body().getResults().length != 0) {

                                int zaehler = response.body().getResults().length;

                                if (zaehler > 10) {
                                    zaehler = 10;
                                }

                                for (int i = 0; i < zaehler; i++) {

                                    boolean toDisplay = true;
                                    Results googlePlace = response.body().getResults()[i];
                                    String[] checkType = googlePlace.getTypes();
                                    RVRowInformation thisRow = new RVRowInformation();

                                    // Check for doublettes that are also displayed under the banks search
                                    for (int j = 0; j < checkType.length; j++) {
                                        if (checkType[j].equals("bank")) {
                                            toDisplay = false;
                                            if (zaehler < 20) {
                                                zaehler = zaehler + 1;
                                            }
                                        }
                                    }

                                    // Calculates air distance between location & atm
                                    Double locationToAtmDistance = Distance.distance1(Double.parseDouble(googlePlace.getGeometry().getLocation().getLat()), latitude, Double.parseDouble(googlePlace.getGeometry().getLocation().getLng()), longitude, 0, 0);
                                    Log.i("Distance to ATM: ", locationToAtmDistance.toString());

                                    // Check if distance is greater than 1500m, and hid all results that are farer than that.
                                    if (locationToAtmDistance > 1500) {
                                        toDisplay = false;
                                        if (zaehler < 20) {
                                            zaehler = zaehler + 1;
                                        }
                                    }


                                    String placeName = googlePlace.getName();
                                    if (!placeName.toLowerCase().contains("shell")
                                            && !placeName.toLowerCase().contains("pax")
                                            && !placeName.toLowerCase().contains("diba")
                                            && !placeName.toLowerCase().contains("deutsche")) {
                                        toDisplay = false;
                                        if (zaehler < 20) {
                                            zaehler = zaehler + 1;
                                        }
                                    }


                                    if (toDisplay) {
                                        MarkerOptions markerOptions = new MarkerOptions();

                                        double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                                        double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());

                                        // Because of the collaboration between Shell and Postank, one can pick up cash from all Shell stations as it was a Postbank ATM
                                        // Check if it is a Shell station, but display "Postbank" instead
                                        if (placeName.toLowerCase().contains("shell")) {
                                            placeName = "Postbank";
                                        }
                                        LatLng latLng = new LatLng(lat, lng);
                                        markerOptions.position(latLng);
                                        markerOptions.title(placeName);


                                        if (placeName.toLowerCase().contains("deutsche")) {
                                            markerOptions.title("Deutsche Bank");
                                            thisRow.iconId = images[2];
                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                        } else {
                                            if (placeName.toLowerCase().contains("shell")) {
                                                markerOptions.title("Postbank");
                                                thisRow.iconId = images[7];
                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                            } else {
                                                if (placeName.toLowerCase().contains("ing")) {
                                                    markerOptions.title("ING DiBa");
                                                    thisRow.iconId = images[5];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ing_logo_final));
                                                } else {
                                                    if (placeName.toLowerCase().contains("pax")) {
                                                        markerOptions.title("Pax Bank");
                                                        thisRow.iconId = images[6];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.paxbank_logo_final));
                                                    } else {
                                                        thisRow.iconId = images[3];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.generic_logo_final));
                                                    }
                                                }
                                            }
                                        }

                                        // Add Marker to map
                                        mMap.addMarker(markerOptions);
                                        thisRow.rowTitle = markerOptions.getTitle();
                                        thisRow.rowSubtitle = String.format(Locale.GERMAN, "%.0f", locationToAtmDistance);
                                        data.add(thisRow);
                                        Collections.sort(data, new DataCompare());
                                    }
                                }
                            } else {
                                // Handle if no results were found
                                loadingProgressBar.setVisibility(View.GONE);
                                CustomAlertDialog alert = new CustomAlertDialog();
                                alert.showDialog(MainMapAtmDisplay.this, "No results");
                            }
                        }
                        loadingProgressBar.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(Call<MyAtms> call, Throwable t) {

                    }
                });
    }

    // Function to search and display Bank branches.
    private void nearByBanks() {
        mMap.clear();
        data.clear();
        final ProgressBar loadingProgressBar = findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);
        Log.i("Ausgeführt", "now");
        String url = getUrlBank(latitude, longitude, "bank");

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
                                    //Log.d("TYPE", checkType[j]);
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
                                    double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                                    double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());


                                    LatLng latLng = new LatLng(lat, lng);
                                    markerOptions.position(latLng);
                                    if (placeName.toLowerCase().contains("commerzbank")){
                                        markerOptions.title("Commerzbank");
                                        thisRow.iconId = images[1];
                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.commerzbank_logo_final));
                                    } else {
                                        if (placeName.toLowerCase().contains("sparkasse")){
                                            markerOptions.title(placeName);
                                            thisRow.iconId = images[11];
                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                        } else {
                                            if (placeName.toLowerCase().contains("deutsche")){
                                                markerOptions.title("Deutsche Bank");
                                                thisRow.iconId = images[2];
                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                        } else {
                                                if (placeName.toLowerCase().contains("post")){
                                                    markerOptions.title("Postbank");
                                                    thisRow.iconId = images[7];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                            } else {
                                                    if (placeName.toLowerCase().contains("volks")){
                                                        markerOptions.title("Volksbank");
                                                        thisRow.iconId = images[13];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                                } else {
                                                        if (placeName.toLowerCase().contains("bb")){
                                                            markerOptions.title("BBBank");
                                                            thisRow.iconId = images[0];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("hypo")){
                                                                markerOptions.title("HypoVereinsbank");
                                                                thisRow.iconId = images[4];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("psd")){
                                                                    markerOptions.title("PSD Bank");
                                                                    thisRow.iconId = images[8];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.psd_bank_logo_final));
                                                                } else {
                                                                    if (placeName.toLowerCase().contains("santander")){
                                                                        markerOptions.title("Santander");
                                                                        thisRow.iconId = images[9];
                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.santander_logo_final));
                                                                    } else {
                                                                        if (placeName.toLowerCase().contains("sparda")){
                                                                            markerOptions.title("Sparda-Bank");
                                                                            thisRow.iconId = images[10];
                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparda_bank_logo_final));
                                                                        } else {
                                                                            if (placeName.toLowerCase().contains("targo")){
                                                                                markerOptions.title("TargoBank");
                                                                                thisRow.iconId = images[12];
                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.targobank_logo_final));
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
                                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MainMapAtmDisplay.this));

                                }
                            }
                        }
                        loadingProgressBar.setVisibility(View.GONE);
                    }


                    @Override
                    public void onFailure(Call<MyAtms> call, Throwable t) {

                    }
                });
    }

    public void onPause(){
        super.onPause();

        // Stop location updates when Activity is no longer active
        if (mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000);
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // If permissions already granted
                buildGoogleApiClient();
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {

                //Request permissions
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }


        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setMinZoomPreference(12);
        mMap.setLatLngBoundsForCameraTarget(allowedBoundsGermany);
    }

    private  synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    LocationCallback mLocationCallback = new LocationCallback() {

            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                    mLastLocation = location;


                    if (mCurrentLocationMarker != null) {
                        mCurrentLocationMarker.remove();
                    }

                    // Place current location marker
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    //Move map camera
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            }
        };

        private void checkLocationPermission() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Check if the explanation should be showed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an explanation to the user *ASYNCHRONUSLY*, don't block this thread waiting for the users response
                    new AlertDialog.Builder(this)
                            .setTitle("Location Permissions needed")
                            .setMessage("This app needs the Location Permisions, please accept to use location functionality")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Promt the user once explanation has been shown
                                    ActivityCompat.requestPermissions(MainMapAtmDisplay.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                                }
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed - request the permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                }
            }
        }

        public void onRequestPermissionsResult(int reqestCode, String permissions[], int[] grantResults){

            switch (reqestCode){
                case MY_PERMISSIONS_REQUEST_LOCATION:{

                    // If request is cancelled, the result array is empty
                    if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){

                        // Request was granted. Perform the location related tasks
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                            // Permissions granted, actiate the functionality that depends on the permission.
                            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
                            if (mGoogleApiClient == null) {
                                buildGoogleApiClient();
                            }
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                            mMap.setMyLocationEnabled(true);
                        }
                    } else {

                        // Permissions denied, disable the functionality that depends on the permission.
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                        final ProgressBar loadingProgressBar = findViewById(R.id.progresLoader);
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                }
            }
        }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000);
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrentLocationMarker != null) {
            mCurrentLocationMarker.remove();
        }
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        LatLng latLng = new LatLng(latitude, longitude);

        try {
            Geocoder geocoder = new Geocoder(this.getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.isEmpty()){
                getSupportActionBar().setSubtitle("Standort wird ermittlet");
            } else {
                if (addresses.size() > 0){
                    getSupportActionBar().setSubtitle("In der Nähe von " + addresses.get(0).getLocality());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check if the location is inside DE
        if (allowedBoundsGermany.contains(latLng)) {

            // Move Camera
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            Log.i("Ausgeführt in: ", "onLocationChanged");
            nearByBanks();
            nearByAtms("atm");
            //nearByAtms("ing diba");
            //nearByAtms("deutsche bank");
            //nearByAtms("pax bank");
        } else {

            CustomAlertDialog alert = new CustomAlertDialog();
            alert.showDialog(this, "This app only works inside the borders of Germany");



            //new AlertDialog.Builder(this)
                    //.setTitle("Location Outside of Germany")
                    //.setMessage("This app only works inside the borders of Germany")
                    //.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        //public void onClick(DialogInterface dialogInterface, int i) {
                        //}
                    //})
                    //.create()
                    //.show();
        }

        if (mGoogleApiClient != null){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    class DataCompare implements Comparator<RVRowInformation>{

        @Override
        public int compare(RVRowInformation rvri1, RVRowInformation rvri2) {
            Double comp1, comp2;
            comp1 = Double.parseDouble(rvri1.rowSubtitle);
            comp2 = Double.parseDouble(rvri2.rowSubtitle);
            return comp1.compareTo(comp2);
    }
    }
}
