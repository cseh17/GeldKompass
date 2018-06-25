package com.atm_search.cseh_17.geld_kompass;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.perf.metrics.AddTrace;

public class AppInfoFragment extends Fragment {


    @Nullable
    @Override
    @AddTrace(name = "AppInfoFragment-onCreateView")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.about_info_screen, container, false);
        return view;
    }
}
