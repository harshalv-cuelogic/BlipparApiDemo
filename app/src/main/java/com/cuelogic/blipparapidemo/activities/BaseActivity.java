package com.cuelogic.blipparapidemo.activities;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by Harshal Vibhandik on 14/08/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

    private ProgressDialog pDialog;

    protected void showProgress() {
        try {
            if (pDialog == null)
                pDialog = ProgressDialog.show(this, null, "Please wait...", true);
            pDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void dismissProgress() {
        try {
            if (pDialog != null)
                pDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showToastShort(String message) {
        Toast.makeText(BaseActivity.this, message, Toast.LENGTH_SHORT).show();
    }
    public void showToastLong(String message) {
        Toast.makeText(BaseActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
