package com.voipgrid.vialer.contacts;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.voipgrid.vialer.BuildConfig;


/**
 * Class for initiating the ContactSync.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String LOG_TAG = SyncAdapter.class.getName();
    private static final boolean DEBUG = true;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        long start = 0;
        long end = 0;
        if (DEBUG){
            Log.d(LOG_TAG, "Start syncing contacts");
            start = System.currentTimeMillis();
        }
        new ContactsSyncTask(getContext()).sync();
        if (DEBUG){
            end = System.currentTimeMillis();
            Log.d(LOG_TAG, "Done syncing contacts, it took " + Double.toString((end - start) / 1000) + " seconds");
        }
    }
}
