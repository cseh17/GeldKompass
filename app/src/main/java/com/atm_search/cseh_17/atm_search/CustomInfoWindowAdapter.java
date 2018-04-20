package com.atm_search.cseh_17.atm_search;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;
    private Context mContext;

    public CustomInfoWindowAdapter(Context mContext) {
        this.mContext = mContext;
        mWindow = LayoutInflater.from(mContext).inflate(R.layout.custom_info_window, null);
    }

    private void addWindowTitle(Marker marker, View view){
        String title = marker.getTitle();
        TextView tvTitle = (TextView) view.findViewById(R.id.ciw_title);

        tvTitle.setText(title);
    }

    private String getTitle(Marker marker, View view){
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
        if (getTitle(marker, mWindow).toLowerCase().contains("sparkasse")){
            image.setImageResource(R.drawable.detail_logo_sparkasse);
        } else {
            if (getTitle(marker, mWindow).toLowerCase().contains("deutsche") | getTitle(marker, mWindow).toLowerCase().contains("commerz") | getTitle(marker, mWindow).toLowerCase().contains("post") | getTitle(marker, mWindow).toLowerCase().contains("hypo") | getTitle(marker, mWindow).toLowerCase().contains("dresdener")){
                image.setImageResource(R.drawable.detail_logo_cash_group);
            } else {
                if (getTitle(marker, mWindow).toLowerCase().contains("bbb") | getTitle(marker, mWindow).toLowerCase().contains("pax") | getTitle(marker, mWindow).toLowerCase().contains("santander") | getTitle(marker, mWindow).toLowerCase().contains("sparda") | getTitle(marker, mWindow).toLowerCase().contains("targo")){
                    image.setImageResource(R.drawable.detail_logo_cash_pool);
                }
            }
        }
        return mWindow;
    }
}
