package com.atm_search.cseh_17.geld_kompass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;

    @SuppressLint("InflateParams")
    CustomInfoWindowAdapter(Context mContext) {
        mWindow = LayoutInflater.from(mContext).inflate(R.layout.custom_info_window, null);
    }

    private void addWindowTitle(Marker marker, View view){
        String title = marker.getTitle();
        TextView tvTitle = view.findViewById(R.id.ciw_title);

        tvTitle.setText(title);
    }

    private String getTitle(Marker marker){
       return marker.getTitle();
    }

    @Override
    public View getInfoWindow(Marker marker) {
        //addWindowTitle(marker, mWindow);
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        addWindowTitle(marker, mWindow);
        ImageView image = mWindow.findViewById(R.id.ciw_logo);
        if (getTitle(marker).toLowerCase().contains("sparkasse")){
            image.setImageResource(R.drawable.detail_logo_sparkasse);
        } else {
            if (getTitle(marker).toLowerCase().contains("deutsche") | getTitle(marker).toLowerCase().contains("commerz") | getTitle(marker).toLowerCase().contains("post") | getTitle(marker).toLowerCase().contains("hypo") | getTitle(marker).toLowerCase().contains("dresdener")){
                image.setImageResource(R.drawable.detail_logo_cash_group);
            } else {
                if (getTitle(marker).toLowerCase().contains("bbb") | getTitle(marker).toLowerCase().contains("pax") | getTitle(marker).toLowerCase().contains("santander") | getTitle(marker).toLowerCase().contains("sparda") | getTitle(marker).toLowerCase().contains("targo") | getTitle(marker).toLowerCase().contains("degussa") | getTitle(marker).toLowerCase().contains("südwest") | getTitle(marker).toLowerCase().contains("national") | getTitle(marker).toLowerCase().contains("olb") | getTitle(marker).toLowerCase().contains("oldenburgische")){
                    image.setImageResource(R.drawable.detail_logo_cash_pool);
                } else {
                    image.setImageResource(R.drawable.detail_logos_generic);
                }
            }
        }
        return mWindow;
    }
}
