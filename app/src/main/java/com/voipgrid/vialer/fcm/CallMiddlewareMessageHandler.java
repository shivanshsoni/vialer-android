package com.voipgrid.vialer.fcm;

import static com.voipgrid.vialer.middleware.MiddlewareMessage.Builder.MESSAGE_START_TIME;
import static com.voipgrid.vialer.sip.SipService.EXTRA_MIDDLEWARE_MESSAGE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.voipgrid.vialer.analytics.CallRejectionAnalytics;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.middleware.MiddlewareMessage;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.util.PhoneNumberUtils;

public class CallMiddlewareMessageHandler {

    private RemoteLogger mRemoteLogger;
    private Context mContext;
    private CallConnectivityManager mCallConnectivityManager;
    private CallRejectionAnalytics mCallRejectionAnalytics;

    public CallMiddlewareMessageHandler(Context context) {
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
        mContext = context;
        mCallConnectivityManager = new CallConnectivityManager(context);
        mCallRejectionAnalytics = new CallRejectionAnalytics(context);
    }

    /**
     * Handles the message from the middleware alerting us of an incoming call.
     *
     * @param middlewareMessage
     */
    public void handle(MiddlewareMessage middlewareMessage) {
        LogHelper.using(mRemoteLogger).logCallHandling(SipService.sipServiceActive, mCallConnectivityManager.getConnectivityHelper(), middlewareMessage);

        // If the connection is not currently sufficient we will wait for more push messages
        // as the connection may improve (e.g. leaving standby mode), if we have reached too many
        // attempts then the call will be rejected.
        if(!mCallConnectivityManager.isConnectionSufficient(middlewareMessage)) {
            if(mCallConnectivityManager.hasReachedMaxAttempts(middlewareMessage)) {
                mCallRejectionAnalytics.rejectDueToPoorConnectivity(middlewareMessage, mCallConnectivityManager.getConnectivityHelper());
                return;
            }
            mRemoteLogger.e("Connection is insufficient. For now do nothing and wait for next middleware push");
            return;
        }

        if (isCallAlreadyInProgress()) {
            mCallRejectionAnalytics.rejectDueToCallAlreadyInProgress(middlewareMessage);
            return;
        }

        mRemoteLogger.d("Payload processed, calling startService method");
        startSipServiceToAllowIncomingCall(middlewareMessage);
    }

    /**
     * Check whether the SIP service is actively handling a call, in this situation
     * we cannot handle another one.
     *
     * @return TRUE if there is a call already in progress.
     */
    private boolean isCallAlreadyInProgress() {
        return SipService.sipServiceActive;
    }

    /**
     * Creates the intent and starts the SIP service, this will allow the app to receive
     * SIP messaging and respond to the incoming call.
     *
     * @param middlewareMessage Contains the information required by the SIP service.
     */
    private void startSipServiceToAllowIncomingCall(MiddlewareMessage middlewareMessage) {
        mRemoteLogger.d("startSipServiceToAllowIncomingCall");
        Intent intent = new Intent(mContext, SipService.class);
        intent.setAction(SipConstants.ACTION_CALL_INCOMING);

        // Set a phoneNumberUri as DATA for the intent to SipServiceOld.
        Uri sipAddressUri = SipUri.sipAddressUri(
                mContext,
                PhoneNumberUtils.format(middlewareMessage.getAdjustedPhoneNumber(mContext))
        );
        intent.setData(sipAddressUri);

        intent.putExtra(SipConstants.EXTRA_RESPONSE_URL, middlewareMessage.getResponseUrl());
        intent.putExtra(SipConstants.EXTRA_REQUEST_TOKEN, middlewareMessage.getRequestToken());
        intent.putExtra(SipConstants.EXTRA_PHONE_NUMBER, middlewareMessage.getAdjustedPhoneNumber(mContext));
        intent.putExtra(SipConstants.EXTRA_CONTACT_NAME, middlewareMessage.getCallerId());
        intent.putExtra(MESSAGE_START_TIME, middlewareMessage.getMessageStartTime());
        intent.putExtra(EXTRA_MIDDLEWARE_MESSAGE, middlewareMessage);

        mContext.startService(intent);
    }
}
