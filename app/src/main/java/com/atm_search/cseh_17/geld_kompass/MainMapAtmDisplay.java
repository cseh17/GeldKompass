package com.atm_search.cseh_17.geld_kompass;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.atm_search.cseh_17.geld_kompass.Remote.APIService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;

import static android.content.ContentValues.TAG;


// Activity that displays a map showing the place at the device's current location
public class MainMapAtmDisplay extends AppCompatActivity implements
        OnMapReadyCallback{

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    private static double latitude, longitude;
    private LocationRequest mLocationRequest;
    private Marker mCurrentLocationMarker;
    SupportMapFragment mapFragment;
    AppInfoFragment appInfoFragment;
    static ReportFormFragment reportFragment;
    DatenschutzFragment datenschutzFragment;
    private FusedLocationProviderClient mFusedLocationClient;
    APIService mService;
    private LatLngBounds allowedBoundsGermany = new LatLngBounds(new LatLng( 47.2701115, 5.8663425), new LatLng(55.0815,15.0418962));
    private RecyclerView recyclerView;
    private DrawerLayout mDrawerLayout;
    LinkedList<RVRowInformation> data = new LinkedList<>(Collections.<RVRowInformation>emptyList());
    RVAdapter adapter;
    boolean cashGroupIsSelected, cashPoolIsSelected, sparkasseIsSelected, volksbankIsSelected;
    private FirebaseAnalytics mFirebaseAnalytics;
    private AdView mBannerAdListView;
    private InterstitialAd mInterstitialAd;
    private LatLng defaultLocation = new LatLng(51.163375, 10.447683);
    private LatLng mLastLocation;



    @AddTrace(name = "MainOnCreate")
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Set the filter buttons isSelected variables to false
        cashGroupIsSelected = false;
        cashPoolIsSelected = false;
        sparkasseIsSelected = false;
        volksbankIsSelected = false;

        // Retrieve the content view that renders the map
        setContentView(R.layout.activity_main_map_atm_display);

        // Set the Drawer Layout (menu)
        mDrawerLayout = findViewById(R.id.drawer_layout);

        //Initialise AdMob ads
        MobileAds.initialize(this, "ca-app-pub-3182911509384087~8231949051");
        mBannerAdListView = findViewById(R.id.mainListAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mBannerAdListView.loadAd(adRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3182911509384087/5866953241");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        //Set an AdListner to reload Interstitial ads after being closed
        mInterstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdClosed(){
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

        // Implement menu functionality and click listeners
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    String mFragmentToSet = null;
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                        // Create FragmentManager & initialize InfoFragment & ReportFormFragment
                        //FragmentManager fm = getSupportFragmentManager();
                        appInfoFragment = new AppInfoFragment();
                        reportFragment = new ReportFormFragment();
                        datenschutzFragment = new DatenschutzFragment();

                        switch (item.getItemId()) {

                            case R.id.report_missing:
                                setNavButtonUnclickable();
                                Bundle params = new Bundle();
                                params.putString("menuItem", "report_missing");
                                mFirebaseAnalytics.logEvent("MenuItemPressed", params);
                                if (mInterstitialAd.isLoaded()){
                                    mInterstitialAd.show();
                                }
                                //FragmentTransaction ft;
                                //ft = fm.beginTransaction();
                                //ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                //ft.add(R.id.main_activity_layout, reportFragment).addToBackStack(null).commit();
                                mFragmentToSet = "report";
                                item.setChecked(true);
                                mDrawerLayout.closeDrawers();
                                setNavButtonClickable();
                                break;

                            case R.id.about:
                                setNavButtonUnclickable();
                                params = new Bundle();
                                params.putString("menuItem", "about");
                                mFirebaseAnalytics.logEvent("MenuItemPressed", params);
                                item.setChecked(true);
                                //ft = fm.beginTransaction();
                                //ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                //ft.add(R.id.main_activity_layout, appInfoFragment).addToBackStack(null).commit();
                                mFragmentToSet = "info";
                                mDrawerLayout.closeDrawers();
                                setNavButtonClickable();
                                break;

                            case R.id.datenschutz:
                                setNavButtonUnclickable();
                                params = new Bundle();
                                params.putString("menuItem", "datenschutz");
                                mFirebaseAnalytics.logEvent("MenuItemPressed", params);
                                if (mInterstitialAd.isLoaded()){
                                    mInterstitialAd.show();
                                }
                                item.setChecked(true);
                                mFragmentToSet = "datenschutz";
                                mDrawerLayout.closeDrawers();
                                setNavButtonClickable();
                                break;

                            default:
                                mDrawerLayout.closeDrawers();
                                return true;
                        }

                        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                            @Override
                            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

                            }

                            @Override
                            public void onDrawerOpened(@NonNull View drawerView) {

                            }

                            @Override
                            public void onDrawerClosed(@NonNull View drawerView) {

                                // Create FragmentManager & initialize InfoFragment & ReportFormFragment
                                FragmentManager fm = getSupportFragmentManager();

                                if (mFragmentToSet == "report") {
                                    FragmentTransaction ft;
                                    ft = fm.beginTransaction();
                                    ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                    ft.add(R.id.main_activity_layout, reportFragment).addToBackStack(null).commit();
                                    mFragmentToSet = null;
                                }

                                if (mFragmentToSet == "datenschutz") {
                                    FragmentTransaction ft;
                                    ft = fm.beginTransaction();
                                    ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                    ft.add(R.id.main_activity_layout, datenschutzFragment).addToBackStack(null).commit();
                                    mFragmentToSet = null;
                                }

                                if (mFragmentToSet == "info") {
                                    FragmentTransaction ft;
                                    ft = fm.beginTransaction();
                                    ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                    ft.add(R.id.main_activity_layout, appInfoFragment).addToBackStack(null).commit();
                                    mFragmentToSet = null;
                                }

                            }

                            @Override
                            public void onDrawerStateChanged(int newState) {

                            }
                        });
                        return true;
                    }
                }
        );

        // Set toolbar
        android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        // Initialise RecyclerView
        recyclerView = findViewById(R.id.rv_list_items);
        adapter = new RVAdapter(this, data);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.GONE);

        //Start location client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialise Service
        mService = Common.getGooglePIService();

        // Initialise TabView
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        Bundle params = new Bundle();
                        params.putString("tabs", "map");
                        mFirebaseAnalytics.logEvent("TabSelected", params);
                        Objects.requireNonNull(mapFragment.getView()).setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        params = new Bundle();
                        params.putString("tabs", "list");
                        mFirebaseAnalytics.logEvent("TabSelected", params);
                        recyclerView.setVisibility(View.VISIBLE);

                        // When ListView is selected, check if the data Object is empty, and if true, show alert.
                        if (adapter.getItemCount() == 0) {
                            CustomAlertDialog dialog = new CustomAlertDialog();
                            dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_result_alert_list_DE));
                        }
                        break;

                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0:
                        Objects.requireNonNull(mapFragment.getView()).setVisibility(View.GONE);
                        break;
                    case 1:
                        recyclerView.setVisibility(View.GONE);
                        break;

                }

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

        //Create 3 filterButons ClickListeners
        final FloatingActionButton floatingCashGroupFilterButton = findViewById(R.id.filterCashGroupButton);
        final FloatingActionButton floatingCashPoolFilterButton = findViewById(R.id.filterCashPoolButton);
        final FloatingActionButton floatingSparkasseFilterButton = findViewById(R.id.filterSparkasseButton);
        final FloatingActionButton floatingVolksbankFilterButton = findViewById(R.id.filterVolksbankButton);

        floatingCashGroupFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Bundle params = new Bundle();
                params.putString("filter", "CashGroup");
                mFirebaseAnalytics.logEvent("FilterApplied", params);

                if (cashGroupIsSelected) {

                    cashGroupIsSelected = false;
                    cashPoolIsSelected = false;
                    sparkasseIsSelected = false;
                    volksbankIsSelected = false;
                    floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));

                    SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter, data);
                } else {
                    cashGroupIsSelected = true;
                    cashPoolIsSelected = false;
                    sparkasseIsSelected = false;
                    volksbankIsSelected = false;
                    floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.colorPrimaryLight)));
                    floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));


                    // Check for connectivity
                    ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    // If there is a connection, do the search
                    if (!CheckConnection.isConnected(Objects.requireNonNull(cm))) {

                        // if there is no connection, show an alert dialog
                        CustomAlertDialog dialog = new CustomAlertDialog();
                        dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                        final ProgressBar loadingProgressBar = MainMapAtmDisplay.this.findViewById(R.id.main_progresLoader);
                        loadingProgressBar.setVisibility(View.GONE);
                    } else {

                        // if there is a connection, do job
                        boolean returnValue = Filters.nearByBanksFilteredCashGroup(mMap, data, mService, latitude, longitude, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter);
                        if (!returnValue){
                            cashGroupIsSelected = false;
                            cashPoolIsSelected = false;
                            sparkasseIsSelected = false;
                            volksbankIsSelected = false;
                            floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));

                        }
                    }
                }
            }
        });

        floatingCashPoolFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Bundle params = new Bundle();
                params.putString("filter", "CashPool");
                mFirebaseAnalytics.logEvent("FilterApplied", params);

                if (cashPoolIsSelected) {

                    cashGroupIsSelected = false;
                    cashPoolIsSelected = false;
                    sparkasseIsSelected = false;
                    volksbankIsSelected = false;
                    floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this, adapter);
                    SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter, data);
                    //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlAtm(latitude, longitude, browserKey) , MainMapAtmDisplay.this, adapter);
                    //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, images, adapter, data);
                } else {
                    cashGroupIsSelected = false;
                    cashPoolIsSelected = true;
                    sparkasseIsSelected = false;
                    volksbankIsSelected = false;
                    floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.colorPrimaryLight)));
                    floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));


                    // Check for connectivity
                    ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    // If there is a connection, do the search
                    if (!CheckConnection.isConnected(Objects.requireNonNull(cm))) {

                        // if there is no connection, show an alert dialog
                        CustomAlertDialog dialog = new CustomAlertDialog();
                        dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                        final ProgressBar loadingProgressBar = MainMapAtmDisplay.this.findViewById(R.id.main_progresLoader);
                        loadingProgressBar.setVisibility(View.GONE);
                    } else {

                        // if there is a connection, do job
                        boolean returnValue = Filters.nearByBanksFilteredCashPool(mMap, data, mService, latitude, longitude, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter);
                        if (!returnValue){
                            cashGroupIsSelected = false;
                            cashPoolIsSelected = false;
                            sparkasseIsSelected = false;
                            volksbankIsSelected = false;
                            floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));

                        }
                    }
                }
            }
        });

        floatingSparkasseFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Bundle params = new Bundle();
                params.putString("filter", "Sparkasse");
                mFirebaseAnalytics.logEvent("FilterApplied", params);

                if (sparkasseIsSelected) {

                    cashGroupIsSelected = false;
                    cashPoolIsSelected = false;
                    sparkasseIsSelected = false;
                    volksbankIsSelected = false;
                    floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this, adapter);
                    SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter, data);
                    //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlAtm(latitude, longitude, browserKey), MainMapAtmDisplay.this, adapter);
                    //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, images, adapter, data);
                } else {
                    cashGroupIsSelected = false;
                    cashPoolIsSelected = false;
                    sparkasseIsSelected = true;
                    volksbankIsSelected = false;
                    floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.colorPrimaryLight)));
                    floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));

                    // Check for connectivity
                    ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    // If there is a connection, do the search
                    if (!CheckConnection.isConnected(Objects.requireNonNull(cm))) {

                        // if there is no connection, show an alert dialog
                        CustomAlertDialog dialog = new CustomAlertDialog();
                        dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                        final ProgressBar loadingProgressBar = MainMapAtmDisplay.this.findViewById(R.id.main_progresLoader);
                        loadingProgressBar.setVisibility(View.GONE);
                    } else {

                        // if there is a connection, do job
                        boolean returnValue = Filters.nearByBanksFilteredSparkasse(mMap, data, mService, latitude, longitude, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter);
                        if (!returnValue){
                            cashGroupIsSelected = false;
                            cashPoolIsSelected = false;
                            sparkasseIsSelected = false;
                            volksbankIsSelected = false;
                            floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                        }
                    }
                }
            }
        });

        floatingVolksbankFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bundle params = new Bundle();
                params.putString("filter", "VolksbankenGroup");
                mFirebaseAnalytics.logEvent("FilterApplied", params);

                if (volksbankIsSelected) {

                    cashGroupIsSelected = false;
                    cashPoolIsSelected = false;
                    sparkasseIsSelected = false;
                    volksbankIsSelected = false;
                    floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", browserKey), MainMapAtmDisplay.this, adapter);
                    SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter, data);
                    //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlAtm(latitude, longitude, browserKey), MainMapAtmDisplay.this, adapter);
                    //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, images, adapter, data);
                } else {
                    cashGroupIsSelected = false;
                    cashPoolIsSelected = false;
                    sparkasseIsSelected = false;
                    volksbankIsSelected = true;
                    floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                    floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.colorPrimaryLight)));

                    // Check for connectivity
                    ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    // If there is a connection, do the search
                    if (!CheckConnection.isConnected(Objects.requireNonNull(cm))) {

                        // if there is no connection, show an alert dialog
                        CustomAlertDialog dialog = new CustomAlertDialog();
                        dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                        final ProgressBar loadingProgressBar = MainMapAtmDisplay.this.findViewById(R.id.main_progresLoader);
                        loadingProgressBar.setVisibility(View.GONE);
                    } else {

                        // if there is a connection, do job
                        boolean returnValue = Filters.nearByBanksFilteredVolksbank(mMap, data, mService, latitude, longitude, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter);
                        if (!returnValue){
                            cashGroupIsSelected = false;
                            cashPoolIsSelected = false;
                            sparkasseIsSelected = false;
                            volksbankIsSelected = false;
                            floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                            floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                        }
                    }
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

        ((GeldKompassApp)this.getApplication()).startActivityTransitionTimer();
        try {

            // If not empty try and cache the data for later usage
            CacheData.writeAtmData(this, this.getString(R.string.KEY_for_atms), null);

            // Also save the time the data was cached in UNIX timestamp format
            long unixTime = System.currentTimeMillis() / 1000;
            CacheData.writeObject(this, this.getString(R.string.KEY_for_timestamp), unixTime);

            // Save the location where the data was chached
            CacheData.writeObject(this, this.getString(R.string.KEY_for_latitude), latitude);
            CacheData.writeObject(this, this.getString(R.string.KEY_for_longitude), longitude);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        Log.i("onPause", "cache deleted");
    }

    public void onResume(){
        super.onResume();

        // Get the navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Iterate trough all the menu items of the navigationView drawer, and set them as unchecked
        for (int i = 0; i < navigationView.getMenu().size(); i++){
            navigationView.getMenu().getItem(i).setChecked(false);
        }

        // Check if app returned from background
        GeldKompassApp mGeldKompassApp = (GeldKompassApp)this.getApplication();
        if (mGeldKompassApp.wasInBackground) {

            Log.i("App returned", "from background");

            // When app returns from background, check for internet connection
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            // If there is a connection, do the search
            if (!CheckConnection.isConnected(Objects.requireNonNull(cm))) {

                // If there is no connection, show an alert dialog
                CustomAlertDialog dialog = new CustomAlertDialog();
                dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                final ProgressBar loadingProgressBar = MainMapAtmDisplay.this.findViewById(R.id.main_progresLoader);
                loadingProgressBar.setVisibility(View.GONE);
            } else {
                 if (mMap != null){
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onMapReady(mMap);
                            Log.i("onResume", "reload map");
                        }
                    },1500);

                }
            }
        }
        mGeldKompassApp.stopActivityTransitionTimer();
    }

    @Override
    public void onBackPressed() {

        // Get the navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Iterate trough all the menu items of the navigationView drawer, and set them as unchecked
        for (int i = 0; i < navigationView.getMenu().size(); i++){
            navigationView.getMenu().getItem(i).setChecked(false);
        }

        // If drawer is open when back is pressed, then close
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

    }


    public void setNavButtonUnclickable(){

        // Get the navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Iterate trough all the menu items of the navigationView drawer, and set them as unclickable
        for (int i = 0; i < navigationView.getMenu().size(); i++){
            navigationView.getMenu().getItem(i).setCheckable(false);
        }
    }

    public void setNavButtonClickable(){

        // Get the navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Iterate trough all the menu items of the navigationView drawer, and set them as unclickable
        for (int i = 0; i < navigationView.getMenu().size(); i++){
            navigationView.getMenu().getItem(i).setCheckable(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    @AddTrace(name = "MainOnMapReady")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = LocationRequest.create();

        // Automatic location update set to 2 min.
        mLocationRequest.setInterval(180000);
        mLocationRequest.setFastestInterval(180000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check if the map has been loaded, and the View is not empty
        if (mMap != null  &&
                Objects.requireNonNull(mapFragment.getView()).findViewById(Integer.parseInt("1")) != null) {

            // Get the View
            View locationCompass = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("5"));

            // Position the CompassButton
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationCompass.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            layoutParams.setMargins(30, 280,0, 0);

            // Set camera to default location when last location is not available
            if (mLastLocation == null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(mLastLocation));
            }
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
                final FloatingActionButton floatingCashGroupFilterButton = findViewById(R.id.filterCashGroupButton);
                final FloatingActionButton floatingCashPoolFilterButton = findViewById(R.id.filterCashPoolButton);
                final FloatingActionButton floatingSparkasseFilterButton = findViewById(R.id.filterSparkasseButton);
                final FloatingActionButton floatingVolksbankFilterButton = findViewById(R.id.filterVolksbankButton);

                Trace myTrace = FirebasePerformance.getInstance().newTrace("Main-getLocation");
                myTrace.start();
                for (Location location : locationResult.getLocations()) {
                    if (getApplicationContext() != null){

                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        LatLng latLng = new LatLng(latitude, longitude);

                        mLastLocation = latLng;

                        // Set the NavigationBar Subtitle to show to actual location -> city name.
                        Objects.requireNonNull(getSupportActionBar()).setSubtitle(MainMapAtmDisplay.this.getString(R.string.nav_bar_title_done) + " " + GetCityNameFromCoordinates.getCityName(latitude, longitude, MainMapAtmDisplay.this));

                        // Check if the location is inside DE
                        if (allowedBoundsGermany.contains(latLng)) {

                            // Move Camera
                            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));

                            // Check for connectivity
                            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                            // If there is a connection, do the search
                            if(!CheckConnection.isConnected(Objects.requireNonNull(cm))) {

                                // if there is no connection, show an alert dialog
                                CustomAlertDialog dialog = new CustomAlertDialog();
                                dialog.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.no_internet_alert_DE));
                                final ProgressBar loadingProgressBar = MainMapAtmDisplay.this.findViewById(R.id.main_progresLoader);
                                loadingProgressBar.setVisibility(View.GONE);
                            } else {
                                cashGroupIsSelected = false;
                                cashPoolIsSelected = false;
                                sparkasseIsSelected = false;
                                volksbankIsSelected = false;
                                floatingCashGroupFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                                floatingCashPoolFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                                floatingSparkasseFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                                floatingVolksbankFilterButton.setBackgroundTintList(ColorStateList.valueOf(MainMapAtmDisplay.this.getColor(R.color.white)));
                                //SearchFor.nearByBanks(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlBank(latitude, longitude, "bank", getResources().getString(R.string.browser_key)), MainMapAtmDisplay.this, adapter);
                                SearchFor.osmNearByBanks(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, adapter, data);
                                //SearchFor.nearByAtms(mMap, data, mService, images, latitude, longitude, MainMapAtmDisplay.this, GenerateUrls.getUrlAtm(latitude, longitude, getResources().getString(R.string.browser_key)), MainMapAtmDisplay.this, adapter);
                                //SearchFor.osmNearByAtms(mService, latitude, longitude, mMap, MainMapAtmDisplay.this, MainMapAtmDisplay.this, images, adapter, data);

                            }
                        } else {
                            Bundle params = new Bundle();
                            params.putString("outOfBounds", "true");
                            mFirebaseAnalytics.logEvent("AppOpenedOutsideDE", params);
                            CustomAlertDialog alert = new CustomAlertDialog();
                            alert.showDialog(MainMapAtmDisplay.this, MainMapAtmDisplay.this.getString(R.string.out_of_bounds_alert_DE));
                            final ProgressBar loadingProgressBar = MainMapAtmDisplay.this.findViewById(R.id.main_progresLoader);
                            loadingProgressBar.setVisibility(View.GONE);
                        }

                        Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                        myTrace.stop();
                        if (mCurrentLocationMarker != null) {
                            mCurrentLocationMarker.remove();
                        }
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
                        Bundle params = new Bundle();
                        params.putString("granted", "true");
                        mFirebaseAnalytics.logEvent("LocationPermission", params);
                        Toast.makeText(this, MainMapAtmDisplay.this.getString(R.string.permission_granted_DE), Toast.LENGTH_LONG).show();
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {

                    // Permissions denied, disable the functionality that depends on the permission.
                    Bundle params = new Bundle();
                    params.putString("granted", "false");
                    mFirebaseAnalytics.logEvent("LocationPermission", params);
                    Toast.makeText(this, MainMapAtmDisplay.this.getString(R.string.permission_denied_DE), Toast.LENGTH_LONG).show();
                    final ProgressBar loadingProgressBar = findViewById(R.id.main_progresLoader);
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }
        }
    }

    public static LatLng getLocation(){

        return new LatLng(latitude, longitude);
    }
}

