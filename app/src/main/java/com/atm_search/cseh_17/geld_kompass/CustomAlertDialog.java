package com.atm_search.cseh_17.geld_kompass;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;

public class CustomAlertDialog {

    public void showDialog(Activity activity, String msg){

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.custom_dialog);

            TextView text = dialog.findViewById(R.id.custom_alert_text_dialog);
            text.setText(msg);

            Button dialogButton = dialog.findViewById(R.id.custom_dialog_btn_dialog);
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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

