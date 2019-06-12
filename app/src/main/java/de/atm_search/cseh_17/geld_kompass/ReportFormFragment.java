package de.atm_search.cseh_17.geld_kompass;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;

import android.preference.PreferenceManager;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;


import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.perf.metrics.AddTrace;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;



public class ReportFormFragment extends Fragment {

    private LatLngBounds allowedBoundsGermany = new LatLngBounds(new LatLng( 47.2701115, 5.8663425), new LatLng(55.0815,15.0418962));

    @Nullable
    @Override
    @AddTrace(name = "ReportFormFragment-onCreateView")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.report_form, container, false);
        SharedPreferences reportingPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        final Spinner question1 = view.findViewById(R.id.question1_spinner);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(Objects.requireNonNull(getContext()), R.array.report_spinner_problem, android.R.layout.simple_spinner_item );
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

                // Check if location is inside DE
                LatLng mLocation = MainMapAtmDisplay.getLocation();
                if (allowedBoundsGermany.contains(mLocation)) {

                    // Check for connectivity
                    ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    // If there is a connection, do the search
                    if (!CheckConnection.isConnected(Objects.requireNonNull(cm))) {

                        // if there is no connection, show an alert dialog
                        CustomAlertDialog dialog = new CustomAlertDialog();
                        dialog.showDialog(getActivity(), Objects.requireNonNull(getActivity()).getString(R.string.no_internet_alert_DE));
                    } else {

                        // if there is a connection submit
                        final ProgressBar loadingProgressBar = Objects.requireNonNull(getActivity()).findViewById(R.id.rf_progressLoader);
                        loadingProgressBar.setVisibility(View.VISIBLE);
                        new NetworkTraffic(getActivity(), question1, question2, loadingProgressBar).execute();
                    }
                } else {
                    CustomAlertDialog dialog = new CustomAlertDialog();
                    dialog.showDialog(getActivity(), Objects.requireNonNull(getActivity()).getString(R.string.out_of_bounds_alert_DE2));
                }
            }
        });

        // Check if there is a sharedPreference called "isReportingLocationCollectionAllowed".
        boolean isAllowed = reportingPreferences.getBoolean("isReportingLocationCollectionAllowed", false);

        // If there is none, or the value of it is "false", create the dialog to ask for permission.
        if (!isAllowed) {
            CustomWarningDialog dialog = new CustomWarningDialog();
            dialog.showDialog(getActivity(), getContext(), Objects.requireNonNull(getActivity()).getString(R.string.report_permission_dialog_body));
        }
        return view;
    }
}