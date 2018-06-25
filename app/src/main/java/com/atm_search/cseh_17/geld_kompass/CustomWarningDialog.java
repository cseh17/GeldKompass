package com.atm_search.cseh_17.geld_kompass;


import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;

public class CustomWarningDialog{



    public void showDialog (final Activity activity, String msg){


        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom_dialog_reporting);

        TextView text = dialog.findViewById(R.id.custom_attention_text_dialog);
        text.setText(msg);

        Button dialogButtonPos = dialog.findViewById(R.id.custom_dialog_attention_btn_accept);
        Button dialogButtonNeg = dialog.findViewById(R.id.custom_dialog_attention_btn_denied);
        dialogButtonNeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();
                dialog.dismiss();
            }
        });

        dialogButtonPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }


        });

        dialog.show();

        // Create a Handler that closes the dialog after 1 minute, if user did not close it manually
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, 60000);

    }
}


