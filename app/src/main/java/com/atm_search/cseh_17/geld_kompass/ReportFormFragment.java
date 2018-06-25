package com.atm_search.cseh_17.geld_kompass;


import android.app.Fragment;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;


import com.google.firebase.perf.metrics.AddTrace;


public class ReportFormFragment extends android.support.v4.app.Fragment{


    @Nullable
    @Override
    @AddTrace(name = "ReportFormFragment-onCreateView")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.report_form, container, false);

        final Spinner question1 = view.findViewById(R.id.question1_spinner);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(getContext(), R.array.report_spinner_problem, android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        question1.setAdapter(adapter);

        final Spinner question2 = view.findViewById(R.id.question2_spinner);
        ArrayAdapter adapter1 = ArrayAdapter.createFromResource(getContext(), R.array.report_spinner_banks, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        question2.setAdapter(adapter1);

        final Button submitButton = view.findViewById(R.id.rf_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ProgressBar loadingProgressBar = getActivity().findViewById(R.id.rf_progressLoader);
                loadingProgressBar.setVisibility(View.VISIBLE);
                new NetworkTraffic(getActivity(), question1, question2, loadingProgressBar).execute();
            }
        });

        CustomWarningDialog dialog = new CustomWarningDialog();
        dialog.showDialog(getActivity(),getActivity().getString(R.string.report_title));

        return view;
    }
}
