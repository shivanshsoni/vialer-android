package com.voipgrid.vialer.sip;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.dialer.ToneGenerator;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SipService ensures proper lifecycle management for the PJSUA2 library and
 * provides a persistent interface to SIP services throughout the app.
 *
 */
public class SipService extends Service {
    private final IBinder mBinder = new SipServiceBinder();

    private Handler mHandler;
    private Intent mIncomingCallDetails = null;
    private ToneGenerator mToneGenerator;

    private Preferences mPreferences;
    private RemoteLogger mRemoteLogger;
    private SipBroadcaster mSipBroadcaster;
    private SipCall mCurrentCall;
    private SipCall mInitialCall;
    private SipConfig mSipConfig;
    private NativeCallManager mNativeCallManager;

    private List<SipCall> mCallList = new ArrayList<>();
    private String mInitialCallType;

    private int mCheckServiceUsedTimer = 10000;
    private Handler mCheckServiceHandler;
    private Runnable mCheckServiceRunnable;

    private final BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                // When the native call has been picked up and there is a current call in the ringing state
                // Then decline the current call.
                if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    mRemoteLogger.e("Native call is picked up.");
                    mRemoteLogger.e("Is there an active call: " + (mCurrentCall != null));
                    if (mCurrentCall != null) {
                        mRemoteLogger.e("Current call state: " + mCurrentCall.getCurrentCallState());
                        switch (mCurrentCall.getCurrentCallState()) {
                            case SipConstants.CALL_INCOMING_RINGING:
                                mRemoteLogger.e("Our call is still ringing. So decline it.");
                                mCurrentCall.decline();
                                break;
                            case SipConstants.CALL_CONNECTED_MESSAGE:
                                mRemoteLogger.e("Our call is connected.");
                                if (!mCurrentCall.isOnHold()) {
                                    mRemoteLogger.e("Call was not on hold already. So put call on hold.");
                                    mCurrentCall.toggleHold();
                                }
                                break;
                        }
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Set when the SipService is active. This is used to respond to the middleware.
     */
    public static boolean sipServiceActive = false;


    /**
     * Class the be able to bind a activity to this service.
     */
    public class SipServiceBinder extends Binder {
        public SipService getService() {
            // Return this instance of SipService so clients can call public methods.
            return SipService.this;
        }
    }

    /**
     * SIP does not present Media by default.
     * Use Android's ToneGenerator to play a dial tone at certain required times.
     * @see for usage of delayed "mRingbackRunnable" callback.
     */
    private Runnable mRingbackRunnable = new Runnable() {
        @Override
        public void run() {
            // Play a ring back tone to update a user that setup is ongoing.
            mToneGenerator.startTone(ToneGenerator.Constants.TONE_SUP_DIAL, 1000);
            mHandler.postDelayed(mRingbackRunnable, 4000);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        mToneGenerator = new ToneGenerator(
                AudioManager.STREAM_VOICE_CALL,
                SipConstants.RINGING_VOLUME);

        mSipBroadcaster = new SipBroadcaster(this);

        mPreferences = new Preferences(this);
        mRemoteLogger = new RemoteLogger(SipService.class).enableConsoleLogging();
        mNativeCallManager = new NativeCallManager((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));

        mRemoteLogger.d("onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

        registerReceiver(phoneStateReceiver, filter);

        // Create runnable to check if the SipService is still in use.
        mCheckServiceHandler = new Handler();
        mCheckServiceRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if the service is being used after 10 seconds and shutdown the service
                // if required.
                checkServiceBeingUsed();
                mCheckServiceHandler.postDelayed(this, mCheckServiceUsedTimer);
            }
        };
        mCheckServiceHandler.postDelayed(mCheckServiceRunnable, mCheckServiceUsedTimer);

        PhoneAccount phoneAccount = new JsonStorage<PhoneAccount>(this).get(PhoneAccount.class);
        if (phoneAccount != null) {
            // Try to load PJSIP library.
            mSipConfig = new SipConfig(this, phoneAccount);
            try {
                mSipConfig.initLibrary();
            } catch (SipConfig.LibraryInitFailedException e) {
                stopSelf();
            }
        } else {
            // User has no sip account so destroy the service.
            mRemoteLogger.w("No sip account when trying to create service");
            stopSelf();
        }
    }

    private void checkServiceBeingUsed() {
        mRemoteLogger.d("checkServiceBeingUsed");
        if (mCurrentCall == null) {
            mRemoteLogger.i("No active calls stop the service");
            stopSelf();
        }
    }

    public RemoteLogger getRemoteLogger() {
        return mRemoteLogger;
    }

    public Preferences getPreferences() {
        return mPreferences;
    }

    public NativeCallManager getNativeCallManager() {
        return mNativeCallManager;
    }

    public String getInitialCallType() {
        return mInitialCallType;
    }

    public SipConfig getSipConfig() {
        return mSipConfig;
    }

    @Override
    public void onDestroy() {
        mRemoteLogger.d("onDestroy");

        // If no phoneaccount was found in the onCreate there won't be a sipconfig either.
        // Check to avoid nullpointers.
        if (mSipConfig != null) {
            mSipConfig.cleanUp();
        }

        mSipBroadcaster.broadcastServiceInfo(SipConstants.SERVICE_STOPPED);

        try {
            unregisterReceiver(phoneStateReceiver);
        } catch(IllegalArgumentException e) {
            mRemoteLogger.w("Trying to unregister phoneStateReceiver not registered.");
        }

        mCheckServiceHandler.removeCallbacks(mCheckServiceRunnable);

        sipServiceActive = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRemoteLogger.d("onStartCommand");

        mInitialCallType = intent.getAction();
        Uri number = intent.getData();

        switch (mInitialCallType) {
            case SipConstants.ACTION_CALL_INCOMING:
                mRemoteLogger.d("incomingCall");
                mIncomingCallDetails = intent;
                break;
            case SipConstants.ACTION_CALL_OUTGOING:
                mRemoteLogger.d("outgoingCall");
                makeCall(
                        number,
                        intent.getStringExtra(SipConstants.EXTRA_CONTACT_NAME),
                        intent.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER),
                        true
                );
                break;
            default:
                stopSelf();
        }

        if (intent.getType() != null) {
            if (intent.getType().equals(SipConstants.CALL_DECLINE_INCOMING_CALL)) {
                try {
                    mCurrentCall.decline();
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                    stopSelf();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return START_NOT_STICKY;
    }

    public SipBroadcaster getSipBroadcaster() {
        return mSipBroadcaster;
    }

    /**
     * Play the busy tone used when a call get's disconnected by the recipient.
     */
    public void playBusyTone() {
        mToneGenerator.startTone(ToneGenerator.Constants.TONE_CDMA_NETWORK_BUSY, 1500);
    }

    /**
     * Start the ring back for a outgoing call.
     */
    public void startRingback() {
        mRemoteLogger.d("onCallStartRingback");
        mHandler.postDelayed(mRingbackRunnable, 2000);
    }

    /**
     * Stop the ring back for a outgoing call.
     */
    public void stopRingback() {
        mRemoteLogger.d("onCallStopRingback");
        mHandler.removeCallbacks(mRingbackRunnable);
    }

    /**
     * Function to make a outgoing call without starting a activity.
     * @param number
     * @param contactName
     * @param phoneNumber
     */
    public void makeCall(Uri number, String contactName, String phoneNumber) {
       makeCall(number, contactName, phoneNumber, false);
    }

    /**
     * Function to make a call with or without starting a activity.
     * @param number
     * @param contactName
     * @param phoneNumber
     * @param startActivity
     */
    public void makeCall(Uri number, String contactName, String phoneNumber, boolean startActivity) {
        SipCall call = new SipCall(this, getSipConfig().getSipAccount());
        call.setPhoneNumberUri(number);
        call.setCallerId(contactName);
        call.setPhoneNumber(phoneNumber);
        call.onCallOutgoing(number, startActivity);
    }

    /**
     * Start the activity for a (initial) outgoing call.
     * @param sipCall
     * @param number
     */
    public void startOutgoingCallActivity(SipCall sipCall, Uri number) {
        startCallActivity(
                number,
                CallActivity.TYPE_OUTGOING_CALL,
                sipCall.getCallerId(),
                sipCall.getPhoneNumber()
        );
    }

    /**
     * Start the activity for a incoming call.
     * @param number
     * @param callerId
     */
    public void startIncomingCallActivity(String number, String callerId) {
        startCallActivity(
                SipUri.sipAddressUri(this, PhoneNumberUtils.format(number)),
                CallActivity.TYPE_INCOMING_CALL,
                callerId,
                number
        );
    }

    private void startCallActivity(Uri sipAddressUri, @CallActivity.CallTypes String type, String callerId, String number) {
        mRemoteLogger.d("callVisibleForUser");
        Intent intent = new Intent(this, CallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setDataAndType(sipAddressUri, type);
        intent.putExtra(CallActivity.CONTACT_NAME, callerId);
        intent.putExtra(CallActivity.PHONE_NUMBER, number);

        sipServiceActive = true;
        startActivity(intent);
    }

    /**
     * Set the current call and add it to the list of calls if it does not exists. If this is the
     * first call made set mInitialCall as well.
     * @param call
     */
    public void setCurrentCall(SipCall call) {
        if (call != null && mInitialCall == null) {
            setInitialCall(call);
        }
        mCurrentCall = call;
        if (!mCallList.contains(call) && call != null) {
            mCallList.add(call);
        }
    }

    public SipCall getCurrentCall() {
        return mCurrentCall;
    }

    /**
     * Removes the call from the list and deletes it. If there are no calls left stop
     * the service.
     * @param call
     */
    public void removeCallFromList(SipCall call) {
        mCallList.remove(call);

        if (mCallList.isEmpty()) {
            setCurrentCall(null);
            NotificationHelper notificationHelper = NotificationHelper.getInstance(this);
            notificationHelper.removeAllNotifications();
            stopSelf();
        } else if (call.getCallIsTransferred()) {
            setCurrentCall(null);
        } else {
            setCurrentCall(mCallList.get(0));
        }
    }

    /**
     * Get the details used for a incoming call.
     * @return
     */
    public Intent getIncomingCallDetails() {
        return mIncomingCallDetails;
    }

    private void setInitialCall(SipCall initialCall) {
        mInitialCall = initialCall;
    }

    public SipCall getInitialCall() {
        return mInitialCall;
    }

    public SipCall getFirstCall() {

        if (mCallList.size() > 0) {
            return mCallList.get(0);
        } else {
            return null;
        }
    }
}
