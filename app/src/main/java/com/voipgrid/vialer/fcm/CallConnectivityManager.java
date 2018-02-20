package com.voipgrid.vialer.fcm;

import static android.content.Context.POWER_SERVICE;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.util.ConnectivityHelper;

import java.util.HashMap;

public class CallConnectivityManager {

    private RemoteLogger mRemoteLogger;
    private ConnectivityHelper mConnectivityHelper;
    private Context mContext;

    /**
     * The maximum number of attempts that will be tolerated before
     * the call is rejected due to poor connectivity.
     */
    private static final int MAX_FAILED_CONNECTIVITY_ATTEMPTS = 8;

    /**
     * Tracks the number of attempts made by each push message when experiencing poor connectivity,
     * when reaching {@value #MAX_FAILED_CONNECTIVITY_ATTEMPTS} the call will be rejected.
     */
    private static final HashMap<String, Integer> attempts = new HashMap<>();

    public CallConnectivityManager(Context context) {
        mRemoteLogger = new RemoteLogger(this.getClass());
        mConnectivityHelper = ConnectivityHelper.get(context);
        mContext = context;
    }

    /**
     * Check if the current connection if sufficient to handle a call.
     */
    public boolean isConnectionSufficient(MiddlewareMessage message) {
        boolean connectionSufficient =
                mConnectivityHelper.hasNetworkConnection() && mConnectivityHelper.hasFastData();
        // Device can ben in Idle mode when it's been idling to long. This means that network
        // connectivity
        // is reduced. So we check if we are in that mode and the connection is insufficient.
        // just return and don't reply to the middleware for now.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
            boolean isDeviceIdleMode = powerManager.isDeviceIdleMode();
            mRemoteLogger.d("is device in idle mode: " + isDeviceIdleMode);
            if (isDeviceIdleMode && !connectionSufficient) {
                mRemoteLogger.e(
                        "Device in idle mode and connection insufficient. For now do nothing wait"
                                + " for next middleware push.");
                incrementAttemptsForMessage(message);
                return false;
            }
        }

        if (!connectionSufficient) {
            mRemoteLogger.e(
                    "Connection is insufficient. For now do nothing and wait for next middleware "
                            + "push");
            incrementAttemptsForMessage(message);
            return false;
        }

        attempts.remove(message.getRequestToken());
        return true;
    }

    /**
     * Check if the incoming middleware message has reached the maximum number of attempts.
     */
    public boolean hasReachedMaxAttempts(MiddlewareMessage message) {
        Integer attemptsForMessage = attempts.get(message.getRequestToken());

        if (attemptsForMessage == null) return false;

        mRemoteLogger.i("Currently at attempt #" + attemptsForMessage + " for push message: "
                + message.getRequestToken());

        if (attemptsForMessage >= MAX_FAILED_CONNECTIVITY_ATTEMPTS) {
            attempts.remove(message.getRequestToken());
            return true;
        }

        return false;
    }

    public ConnectivityHelper getConnectivityHelper() {
        return mConnectivityHelper;
    }

    private void incrementAttemptsForMessage(MiddlewareMessage message) {
        Integer attemptsForMessage = attempts.get(message.getRequestToken());

        attemptsForMessage = attemptsForMessage == null ? 0 : attemptsForMessage;

        attempts.put(message.getRequestToken(), ++attemptsForMessage);
    }
}
