package com.voipgrid.vialer.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.migrating.MigrationAsyncTask;

/**
 * Activity to start the updating process.
 */
public class UpdateActivity extends Activity {

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_logo);

        // Start updateHelper that will check if any actions have to be performed
        // before starting the app. onTaskCompleted will be called when the updateHelper
        // has completed it's work.
        MigrationAsyncTask migration = new MigrationAsyncTask(() -> {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        });
        migration.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mProgressDialog = ProgressDialog.show(
                this, this.getString(R.string.update_spinner_title),
                this.getString(R.string.update_spinner_message), true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
