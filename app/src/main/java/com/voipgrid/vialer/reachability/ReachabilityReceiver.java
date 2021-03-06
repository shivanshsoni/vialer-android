package com.voipgrid.vialer.reachability;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.voipgrid.vialer.logging.RemoteLogger;


public class ReachabilityReceiver extends BroadcastReceiver {

    private Context mContext;
    private static ReachabilityInterface mReachabilityInterface;
    private RemoteLogger mRemoteLogger;

    public ReachabilityReceiver(Context context) {
        mContext = context;
        mRemoteLogger = new RemoteLogger(ReachabilityReceiver.class).enableConsoleLogging();
    }

    public void startListening() {
        mContext.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (mReachabilityInterface != null) {
            mReachabilityInterface.networkChange();
        }
    }

    public void stopListening() {
        try {
            mContext.unregisterReceiver(this);
        } catch(IllegalArgumentException e) {
            mRemoteLogger.w("Trying to unregister ConnectivityManager.CONNECTIVITY_ACTION not registered.");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mReachabilityInterface != null) {
            mReachabilityInterface.networkChange();
        }
    }

    static void setInterfaceCallback(ReachabilityInterface listener) {
        mReachabilityInterface = listener;
    }
}
