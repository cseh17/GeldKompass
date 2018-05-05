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

import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFor {

    // Function to search and display Bank branches.
    public static void nearByBanks(final GoogleMap mMap, final LinkedList<RVRowInformation> data, GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, String url, final Activity mActivity, final RVAdapter adapter) {

        // Clear map & data set. Avoids duplicates on map & on list
        mMap.clear();
        data.clear();

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);
        Log.i("Ausgeführt", "now");

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
                                    mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(mActivity));

                                }
                            }
                            if (data.isEmpty()){
                                CustomAlertDialog alert = new CustomAlertDialog();
                                alert.showDialog(mActivity, "No Results!");
                            }
                        }
                        loadingProgressBar.setVisibility(View.GONE);
                    }


                    @Override
                    public void onFailure(@NonNull Call<MyAtms> call, @NonNull Throwable t) {

                    }
                });
    }

    // Function to search and display atms
    public static void nearByAtms(final GoogleMap mMap, final LinkedList<RVRowInformation> data, GoogleAPIService mService, final int[] images, final double latitude, final double longitude, final Context mContext, String url, final Activity mActivity, final RVAdapter adapter) {

        final ProgressBar loadingProgressBar = mActivity.findViewById(R.id.progresLoader);
        loadingProgressBar.setVisibility(View.VISIBLE);

        mService.getNearByPoi(url)
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
                                        Collections.sort(data, new CompareDistancesOnList());
                                    }
                                }
                            } else {
                                // Handle if no results were found
                                loadingProgressBar.setVisibility(View.GONE);
                                CustomAlertDialog alert = new CustomAlertDialog();
                                alert.showDialog(mActivity, "No results");
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
