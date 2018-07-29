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

    private void addWindowSnippet(Marker marker, View view){
        String subtitle = marker.getSnippet();
        TextView tvSubtitle = view.findViewById(R.id.ciw_subtitle);
        tvSubtitle.setText(subtitle);
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
        addWindowSnippet(marker, mWindow);
        ImageView image = mWindow.findViewById(R.id.ciw_logo);
        if (getTitle(marker).toLowerCase().contains("sparkasse")){
            image.setImageResource(R.drawable.ic_cw_sparkasse);
        } else {
            if (getTitle(marker).toLowerCase().contains("deutsche") | getTitle(marker).toLowerCase().contains("commerz") | getTitle(marker).toLowerCase().contains("post") | getTitle(marker).toLowerCase().contains("hypo") | getTitle(marker).toLowerCase().contains("dresdener")){
                image.setImageResource(R.drawable.ic_cw_cash_group);
            } else {
                if (getTitle(marker).toLowerCase().contains("bbb") | getTitle(marker).toLowerCase().contains("pax") | getTitle(marker).toLowerCase().contains("santander") | getTitle(marker).toLowerCase().contains("sparda") | getTitle(marker).toLowerCase().contains("targo") | getTitle(marker).toLowerCase().contains("degussa") | getTitle(marker).toLowerCase().contains("südwest") | getTitle(marker).toLowerCase().contains("national") | getTitle(marker).toLowerCase().contains("olb") | getTitle(marker).toLowerCase().contains("oldenburgische")){
                    image.setImageResource(R.drawable.ic_cw_cash_pool);
                } else {
                    if (getTitle(marker).toLowerCase().contains("volks")
                            || (getTitle(marker).toLowerCase().contains("aachener"))
                            || (getTitle(marker).toLowerCase().contains("bopfing"))
                            || (getTitle(marker).toLowerCase().contains("brühl"))
                            || (getTitle(marker).toLowerCase().contains("donau"))
                            || (getTitle(marker).toLowerCase().contains("erfurter"))
                            || (getTitle(marker).toLowerCase().contains("federsee bank"))
                            || (getTitle(marker).toLowerCase().contains("frankenberger bank"))
                            || (getTitle(marker).toLowerCase().contains("geno"))
                            || (getTitle(marker).toLowerCase().contains("genossenschafts bank münchen"))
                            || (getTitle(marker).toLowerCase().contains("gls"))
                            || (getTitle(marker).toLowerCase().contains("unterlegäu"))
                            || (getTitle(marker).toLowerCase().contains("kölner"))
                            || (getTitle(marker).toLowerCase().contains("ievo"))
                            || (getTitle(marker).toLowerCase().contains("liga"))
                            || (getTitle(marker).toLowerCase().contains("märki"))
                            || (getTitle(marker).toLowerCase().contains("münchener bank"))
                            || (getTitle(marker).toLowerCase().contains("raiffeisen"))
                            || (getTitle(marker).toLowerCase().contains("rv"))
                            || (getTitle(marker).toLowerCase().contains("darlehenkasse"))
                            || (getTitle(marker).toLowerCase().contains("spaar & kredit"))
                            || (getTitle(marker).toLowerCase().contains("spaar&kredit"))
                            || (getTitle(marker).toLowerCase().contains("spreewald"))
                            || (getTitle(marker).toLowerCase().contains("vr"))
                            || (getTitle(marker).toLowerCase().contains("waldecker"))
                            || (getTitle(marker).toLowerCase().contains("team"))) {
                        image.setImageResource(R.drawable.ic_cw_volksbank);
                    } else {
                        image.setImageResource(R.drawable.ic_cw_generic5);
                    }
                }
            }
        }
        return mWindow;
    }
}
