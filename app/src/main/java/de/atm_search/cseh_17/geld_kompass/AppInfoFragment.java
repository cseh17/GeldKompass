package de.atm_search.cseh_17.geld_kompass;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.perf.metrics.AddTrace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AppInfoFragment extends Fragment {


    @Nullable
    @Override
    @AddTrace(name = "AppInfoFragment-onCreateView")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_info_screen, container, false);
    }
}
