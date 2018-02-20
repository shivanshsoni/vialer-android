package com.voipgrid.vialer.fcm;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.util.ConnectivityHelper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallRejector {

    private Context mContext;
    private AnalyticsHelper mAnalyticsHelper;
    private RemoteLogger mRemoteLogger;

    public CallRejector(Context context) {
        mContext = context;
        mAnalyticsHelper = new AnalyticsHelper(VialerApplication.get().getDefaultTracker());
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
    }

    public void rejectForPoorConnectivity(MiddlewareMessage middlewareMessage, ConnectivityHelper connectivityHelper) {
        mRemoteLogger.d("Reject due to lack of connection");
        // Inform the middleware the incoming call is received but the app can not handle
        // the sip call because there is no LTE or Wifi connection available at this
        // point.
        String analyticsLabel = connectivityHelper.getAnalyticsLabel();

        mAnalyticsHelper.sendEvent(
                mContext.getString(R.string.analytics_event_category_middleware),
                mContext.getString(R.string.analytics_event_action_middleware_rejected),
                analyticsLabel
        );
        replyServer(middlewareMessage);
    }

    public void rejectDueToCallAlreadyInProgress(MiddlewareMessage middlewareMessage) {
        mRemoteLogger.d("Reject due to call in progress");

        mAnalyticsHelper.sendEvent(
                mContext.getString(R.string.analytics_event_category_call),
                mContext.getString(R.string.analytics_event_action_inbound),
                mContext.getString(R.string.analytics_event_label_declined_another_call_in_progress)
        );

        replyServer(middlewareMessage);
    }

    public void rejectDueToUserDeclining(MiddlewareMessage middlewareMessage) {
        mRemoteLogger.d("Reject due to user declining");

        mAnalyticsHelper.sendEvent(
                mContext.getString(R.string.analytics_event_category_call),
                mContext.getString(R.string.analytics_event_action_inbound),
                mContext.getString(R.string.analytics_event_label_declined)
        );

        replyServer(middlewareMessage);
    }

    /**
     * Notify the middleware server that we are, in fact, alive.
     */
    private void replyServer(MiddlewareMessage middlewareMessage) {
        mRemoteLogger.d("replyServer");
        Registration registrationApi = ServiceGenerator.createService(
                mContext,
                Registration.class,
                middlewareMessage.getResponseUrl()
        );
        Call<ResponseBody> call = registrationApi.reply(middlewareMessage.getRequestToken(), false, middlewareMessage.getMessageStartTime());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                mRemoteLogger.e("Unable to send response to middleware: " + t.getMessage());
            }
        });
    }
}
