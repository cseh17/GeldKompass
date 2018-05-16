package com.atm_search.cseh_17.geld_kompass;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
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

import com.atm_search.cseh_17.geld_kompass.Model.MyAtms;
import com.atm_search.cseh_17.geld_kompass.Model.Results;
import com.atm_search.cseh_17.geld_kompass.Remote.GoogleAPIService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


// Activity that displays a map showing the place at the device's current location
public class MainMapAtmDisplay extends AppCompatActivity implements
        OnMapReadyCallback{

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    private double latitude, longitude;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Marker mCurrentLocationMarker;
    SupportMapFragment mapFragment;
    private FusedLocationProviderClient mFusedLocationClient;
    GoogleAPIService mService;
    private LatLngBounds allowedBoundsGermany = new LatLngBounds(new LatLng( 47.2701115, 5.8663425), new LatLng(55.0815,15.0418962));
    private RecyclerView recyclerView;
    LinkedList<RVRowInformation> data = new LinkedList<>(Collections.<RVRowInformation>emptyList());
    int[] images = {R.drawable.bbbank_logo_final, R.drawable.commerzbank_logo_final, R.drawable.deutschebank_logo_final, R.drawable.generic_logo_final, R.drawable.hypo_logo_final, R.drawable.ing_logo_final, R.drawable.paxbank_logo_final, R.drawable.postbank_logo_final, R.drawable.psd_bank_logo_final, R.drawable.santander_logo_final, R.drawable.sparda_bank_logo_final, R.drawable.sparkasse_logo_final, R.drawable.targobank_logo_final, R.drawable.volksbank_logo_final, R.drawable.apotheker_und_aerztebank_logo_final, R.drawable.degussa_bank_logo_final, R.drawable.lb_bw_logo_final, R.drawable.lbb_logo_final,R.drawable.oldenburgische_landesbank_logo_final, R.drawable.suedwestbank_logo_final};
    RVAdapter adapter;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the content view that renders the map
        setContentView(R.layout.activity_main_map_atm_display);

        // Initialise RecyclerView
        recyclerView = findViewById(R.id.rv_list_items);
        adapter = new RVAdapter(this, data);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.GONE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialise Service
        mService = Common.getGooglePIService();

        // Request Runtime permission
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //checkLocationPermission();
        //}

        // Initialise TabView
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        recyclerView.setVisibility(View.GONE);
                        Objects.requireNonNull(mapFragment.getView()).setVisibility(View.VISIBLE);
                        //Log.i("Ausgeführt in: ", "map-tab");
                        //if (mMap != null){
                        //nearByAtms("atm");
                        //nearByBanks();
                        //}
                        break;
                    case 1:
                        Objects.requireNonNull(mapFragment.getView()).setVisibility(View.GONE);
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

        // Get the MapNavigationButtons set
        View navToolbarButtons = ((View) Objects.requireNonNull(mapFragment.getView()).findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("4"));

        // Move them to the right side of the mapView
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) navToolbarButtons.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        rlp.setMargins(0, 30, 30, 0);

        // Create floatin MyLocationButton, and set ClickListeners
        FloatingActionButton floatingmyLocationButton = findViewById(R.id.myLocationButton);
        floatingmyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMapReady(mMap);

            }
        });

        //Create 3 filterButons and add ClickListeners
        FloatingActionButton floatingCashGroupFilterButton = findViewById(R.id.filterCashGroupButton);
        floatingCashGroupFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check for connectivity
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                // If there is a connection, do the search
                if (!CheckConnection.isConnected(cm)) {

                    // if there is no connection, show an alert dialog
                    CustomAlertDialog dialog = new CustomAlertDialog();
                    dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                } else {

                    // if there is a connection, do job
                    String browserKey = getResources().getString(R.string.browser_key);
                    Filters.nearByBanksFilteredCashGroup(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this, adapter);
                }
            }
        });

        FloatingActionButton floatingCashPoolFilterButton = findViewById(R.id.filterCashPoolButton);
        floatingCashPoolFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check for connectivity
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                // If there is a connection, do the search
                if (!CheckConnection.isConnected(cm)) {

                    // if there is no connection, show an alert dialog
                    CustomAlertDialog dialog = new CustomAlertDialog();
                    dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                } else {

                    // if there is a connection, do job
                    String browserKey = getResources().getString(R.string.browser_key);
                    Filters.nearByBanksFilteredCashPool(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this, adapter);
                }
            }
        });

        FloatingActionButton floatingSparkasseFilterButton = findViewById(R.id.filterSparkasseButton);
        floatingSparkasseFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check for connectivity
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                // If there is a connection, do the search
                if (!CheckConnection.isConnected(cm)) {

                    // if there is no connection, show an alert dialog
                    CustomAlertDialog dialog = new CustomAlertDialog();
                    dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                } else {

                    // if there is a connection, do job
                    String browserKey = getResources().getString(R.string.browser_key);
                    Filters.nearByBanksFilteredSparkasse(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this, adapter);
                }
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

        mLocationRequest = LocationRequest.create();

        //Automatic location update set to 2 min.
        mLocationRequest.setInterval(120000);
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check if the map has been loaded, and the View is not empty
        if (mMap != null &&
                Objects.requireNonNull(mapFragment.getView()).findViewById(Integer.parseInt("1")) != null) {
            // Get the View
            View locationCompass = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("5"));
            // Position the CompassButton
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationCompass.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            layoutParams.setMargins(30, 280,0, 0);
        }

        // Check for Android(SDK) version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // If permissions already granted
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

    LocationCallback mLocationCallback = new LocationCallback() {

            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (getApplicationContext() != null){
                        setmLastLocation(location);

                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        LatLng latLng = new LatLng(latitude, longitude);

                        try {
                            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            if (addresses.isEmpty()){
                                Objects.requireNonNull(getSupportActionBar()).setSubtitle(MainMapAtmDisplay.this.getString(R.string.nav_bar_title_no_result));
                            } else {
                                Objects.requireNonNull(getSupportActionBar()).setSubtitle(MainMapAtmDisplay.this.getString(R.string.nav_bar_title_done) + addresses.get(0).getLocality());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Check if the location is inside DE
                        if (allowedBoundsGermany.contains(latLng)) {

                            // Move Camera
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            Log.i("Ausgeführt in: ", "onLocationChanged");

                            // Check for connectivity
                            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                            // If there is a connection, do the search
                            if(!CheckConnection.isConnected(cm)) {

                                // if there is no connection, show an alert dialog
                                CustomAlertDialog dialog = new CustomAlertDialog();
                                dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                            } else {
                                String browserKey = getResources().getString(R.string.browser_key);
                                SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this, adapter);
                                SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlAtm(latitude, longitude, browserKey), MainMapAtmDisplay.this, adapter);

                                //nearByBanks();
                                //nearByAtms();
                            }
                        } else {
                            CustomAlertDialog alert = new CustomAlertDialog();
                            alert.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.out_of_bounds_alert_DE));
                        }


                        Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                        setmLastLocation(location);

                        if (mCurrentLocationMarker != null) {
                            mCurrentLocationMarker.remove();
                        }

                        // Place current location marker
                        latLng = new LatLng(location.getLatitude(), location.getLongitude());

                        //Move map camera
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                    }
                }
            }
        };

    private void checkLocationPermission() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Check if the explanation should be showed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an explanation to the user *ASYNCHRONUSLY*, don't block this thread waiting for the users response
                    new AlertDialog.Builder(this)
                            .setTitle(MainMapAtmDisplay.this.getString(R.string.location_permission_title_DE))
                            .setMessage(MainMapAtmDisplay.this.getString(R.string.location_permission_body_DE))
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

    public void onRequestPermissionsResult(int reqestCode, @NonNull String permissions[], @NonNull int[] grantResults){

            switch (reqestCode){
                case MY_PERMISSIONS_REQUEST_LOCATION:{

                    // If request is cancelled, the result array is empty
                    if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){

                        // Request was granted. Perform the location related tasks
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                            // Permissions granted, actiate the functionality that depends on the permission.
                            Toast.makeText(this, MainMapAtmDisplay.this.getString(R.string.permission_granted_DE), Toast.LENGTH_LONG).show();
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                            mMap.setMyLocationEnabled(true);
                        }
                    } else {

                        // Permissions denied, disable the functionality that depends on the permission.
                        Toast.makeText(this, MainMapAtmDisplay.this.getString(R.string.permission_denied_DE), Toast.LENGTH_LONG).show();
                        final ProgressBar loadingProgressBar = findViewById(R.id.progresLoader);
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                }
            }
        }

    public void setmLastLocation(Location mLastLocation) {
        this.mLastLocation = mLastLocation;
    }
}
