package com.atm_search.cseh_17.geld_kompass;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.Window;
import android.widget.TextView;


public class CustomSearchDistanceDialog{

    public Dialog showDialog (final Activity activity, String msg) {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom_dialog_distance_search);

        TextView text = dialog.findViewById(R.id.custom_attention_search_distance_text_dialog);
        text.setText(msg);

        dialog.show();

        return dialog;
    }

}








