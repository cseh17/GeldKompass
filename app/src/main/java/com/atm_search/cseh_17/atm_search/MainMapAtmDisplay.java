package com.atm_search.cseh_17.atm_search;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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

import com.atm_search.cseh_17.atm_search.Model.MyAtms;
import com.atm_search.cseh_17.atm_search.Model.Results;
import com.atm_search.cseh_17.atm_search.Remote.GoogleAPIService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
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
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

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
                switch (tab.getPosition()){
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
        rlp.setMargins(0,30,30,0);

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
                String browserKey = getResources().getString(R.string.browser_key);
                if (!Filters.nearByBanksFilteredCashGroup(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this )){
                    nearByBanks();
                    nearByAtms();
                }
            }
        });

        FloatingActionButton floatingCashPoolFilterButton = findViewById(R.id.filterCashPoolButton);
        floatingCashPoolFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String browserKey = getResources().getString(R.string.browser_key);
                if (!Filters.nearByBanksFilteredCashPool(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this )){
                    nearByBanks();
                    nearByAtms();
                }
            }
        });

        FloatingActionButton floatingSparkasseFilterButton = findViewById(R.id.filterSparkasseButton);
        floatingSparkasseFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String browserKey = getResources().getString(R.string.browser_key);
                if (!Filters.nearByBanksFilteredSparkasse(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this )){
                    nearByBanks();
                    nearByBanks();
                }
            }
        });
    }

    // Function to search and display atms.
    private void nearByAtms() {
        final ProgressBar loadingProgressBar = findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        String browserKey = getResources().getString(R.string.browser_key);

        mService.getNearByPoi(GenerateUrls.getUrlAtm(latitude, longitude, browserKey))
                .enqueue(new Callback<MyAtms>() {
                    @Override
                    public void onResponse(@NonNull Call<MyAtms> call, @NonNull Response<MyAtms> response) {
                        if (response.isSuccessful()) {

                            // Check if the response body is empty. If not, do all tasks. If empty, play alert Dialog with custom message.
                            if (Objects.requireNonNull(response.body()).getResults().length != 0) {

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
                                    Log.i("Distance to ATM: ", locationToAtmDistance.toString());

                                    // Check if distance is greater than 1500m, and hid all results that are farer than that.
                                    if (locationToAtmDistance > 1500) {
                                        toDisplay = false;
                                        if (zaehler < 20) {
                                            zaehler = zaehler + 1;
                                        }
                                    }


                                    String placeName = googlePlace.getName();
                                    Log.i("ATM Name ", placeName);
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
                    public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                    }
                });
    }

    // Function to search and display Bank branches.
    public void nearByBanks() {

        // Clear map & data set. Avoids duplicates on map & on list
        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);
        Log.i("Ausgeführt", "now");
        String browserKey = getResources().getString(R.string.browser_key);

        mService.getNearByPoi(GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey))
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
                                    //Log.d("TYPE", checkType[j]);
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
                                        if (placeName.toLowerCase().contains("sparkasse")) {
                                            markerOptions.title(placeName);
                                            thisRow.iconId = images[11];
                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.sparkasse_logo_final));
                                        } else {
                                            if (placeName.toLowerCase().contains("deutsche")) {
                                                markerOptions.title("Deutsche Bank");
                                                thisRow.iconId = images[2];
                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.deutschebank_logo_final));
                                            } else {
                                                if (placeName.toLowerCase().contains("post")) {
                                                    markerOptions.title("Postbank");
                                                    thisRow.iconId = images[7];
                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.postbank_logo_final));
                                                } else {
                                                    if (placeName.toLowerCase().contains("volks")) {
                                                        markerOptions.title("Volksbank");
                                                        thisRow.iconId = images[13];
                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.volksbank_logo_final));
                                                    } else {
                                                        if (placeName.toLowerCase().contains("bb")) {
                                                            markerOptions.title("BBBank");
                                                            thisRow.iconId = images[0];
                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bbbank_logo_final));
                                                        } else {
                                                            if (placeName.toLowerCase().contains("hypo")) {
                                                                markerOptions.title("HypoVereinsbank");
                                                                thisRow.iconId = images[4];
                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hypo_logo_final));
                                                            } else {
                                                                if (placeName.toLowerCase().contains("psd")) {
                                                                    markerOptions.title("PSD Bank");
                                                                    thisRow.iconId = images[8];
                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.psd_bank_logo_final));
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
                                                                                if (placeName.toLowerCase().contains("apo")) {
                                                                                    markerOptions.title("Deutsche Apotheker und Ärzte Bank");
                                                                                    thisRow.iconId = images[14];
                                                                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.apotheker_und_aerztebank_logo_final));
                                                                                } else {
                                                                                    if (placeName.toLowerCase().contains("degussa")) {
                                                                                        markerOptions.title("Degussa Bank");
                                                                                        thisRow.iconId = images[15];
                                                                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.degussa_bank_logo_final));
                                                                                    } else {
                                                                                        if (placeName.toLowerCase().contains("lbbw") || placeName.toLowerCase().contains("wüttemb")) {
                                                                                            markerOptions.title("LBBW");
                                                                                            thisRow.iconId = images[16];
                                                                                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lb_bw_logo_final));
                                                                                        } else {
                                                                                            if (placeName.toLowerCase().contains("lbb") || placeName.toLowerCase().contains("landesbank berlin")) {
                                                                                                markerOptions.title("Landesbank Berlin");
                                                                                                thisRow.iconId = images[17];
                                                                                                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.lbb_logo_final));
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
                                    data.add(thisRow);
                                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MainMapAtmDisplay.this));

                                }
                            }
                            if (data.isEmpty()){
                                CustomAlertDialog alert = new CustomAlertDialog();
                                alert.showDialog(MainMapAtmDisplay.this, "No Results!");
                            }
                        }
                        loadingProgressBar.setVisibility(View.GONE);
                    }


                    @Override
                    public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

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

        public void onRequestPermissionsResult(int reqestCode, @NonNull String permissions[], @NonNull int[] grantResults){

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
                Objects.requireNonNull(getSupportActionBar()).setSubtitle("Standort wird ermittlet");
            } else {
                Objects.requireNonNull(getSupportActionBar()).setSubtitle("In der Nähe von " + addresses.get(0).getLocality());
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
            nearByAtms();
            //nearByAtms("ing diba");
            //nearByAtms("deutsche bank");
            //nearByAtms("pax bank");
        } else {
            CustomAlertDialog alert = new CustomAlertDialog();
            alert.showDialog(this, "This app only works inside the borders of Germany");
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
