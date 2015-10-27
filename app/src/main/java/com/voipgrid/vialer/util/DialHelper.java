package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.TwoStepCallActivity;

/**
 * Helper class to use to setup a outgoing call. Based on connectivity choose between SIP or
 * A/B connect
 */
public class DialHelper {

    private final static String TAG = DialHelper.class.getSimpleName();
    private final Preferences mPreferences;

    private Context mContext;

    private Storage mStorage;

    private ConnectivityHelper mConnectivityHelper;

    private AnalyticsHelper mAnalyticsHelper;

    public DialHelper(Context context, Storage storage,
            ConnectivityHelper connectivityHelper, AnalyticsHelper analyticsHelper ) {
        mContext = context;
        mStorage = storage;
        mConnectivityHelper = connectivityHelper;
        mAnalyticsHelper = analyticsHelper;
        mPreferences = new Preferences(context);
    }

    /**
     * Setup a connection for the given number. When an PhoneAccount and a fast data connection
     * are both available the call will be setup using SIP otherwise make an API call to setup the
     * connection through A/B connect
     * @param number
     * @param contactName
     */
    public void callNumber(String number, String contactName) {
        if (mPreferences.canUseSip()
                && mStorage.has(PhoneAccount.class)
                && mConnectivityHelper.hasFastData()) {
            callWithSip(number, contactName);
        } else {
            callWithApi(number, contactName);
        }
    }

    /**
     * Setup a call using SIP
     * @param number
     * @param contactName
     */
    private void callWithSip(String number, String contactName) {
        Intent intent = new Intent(mContext, SipService.class);
        intent.setAction(SipConstants.ACTION_VIALER_OUTGOING);

        // set a phoneNumberUri as DATA for the intent to SipServiceOld.
        intent.setData(SipUri.build(mContext, number));

        intent.putExtra(SipConstants.EXTRA_PHONE_NUMBER, number);
        intent.putExtra(SipConstants.EXTRA_CONTACT_NAME, contactName);

        mContext.startService(intent);

        mAnalyticsHelper.send(
                mContext.getString(R.string.analytics_dimension),
                mContext.getString(R.string.analytics_event_category_call),
                mContext.getString(R.string.analytics_event_action_outbound),
                mContext.getString(R.string.analytics_event_label_sip)
        );
    }

    /**
     * Setup a call using A/B connect
     * @param number
     * @param contact
     */
    private void callWithApi(String number, String contact) {
        Intent intent = new Intent(mContext, TwoStepCallActivity.class);
        intent.putExtra(TwoStepCallActivity.NUMBER_TO_CALL, number);
        mContext.startActivity(intent);
    }
}