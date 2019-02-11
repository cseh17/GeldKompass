package de.atm_search.cseh_17.geld_kompass;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;


class CustomSearchDistanceDialog{

    Dialog showDialog(final Activity activity, String msg) {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom_dialog_distance_search);

        TextView text = dialog.findViewById(R.id.custom_attention_search_distance_text_dialog);
        text.setText(msg);

        ProgressBar progressBar = dialog.findViewById(R.id.dialog_progresLoader);
        progressBar.setVisibility(View.VISIBLE);

        // Add keyListener, in order to dismiss Dialog when back key is pressed
        dialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {

                if (i == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                }
                return true;
            }
        });

        dialog.show();

        return dialog;
    }
}








