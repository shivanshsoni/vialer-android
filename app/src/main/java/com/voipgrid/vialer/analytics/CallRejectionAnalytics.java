package com.voipgrid.vialer.analytics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.middleware.MiddlewareMessage;
import com.voipgrid.vialer.util.AccountHelper;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallRejectionAnalytics {

    private Context mContext;
    private AnalyticsHelper mAnalyticsHelper;
    private AccountHelper mAccountHelper;
    private JsonStorage mJsonStorage;
    private RemoteLogger mRemoteLogger;

    private Callback<ResponseBody> callback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

        }

        @Override
        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            mRemoteLogger.e("Unable to send response to middleware: " + t.getMessage());
        }
    };

    public CallRejectionAnalytics(Context context) {
        mContext = context;
        mAnalyticsHelper = new AnalyticsHelper(VialerApplication.get().getDefaultTracker());
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
        mJsonStorage = new JsonStorage(context);
        mAccountHelper = new AccountHelper(context);
    }

    public void rejectDueToPoorConnectivity(MiddlewareMessage middlewareMessage, ConnectivityHelper connectivityHelper) {
        mRemoteLogger.d("Reject due to lack of connection");

        String rejectionReason = connectivityHelper.getAnalyticsLabel();

        String analyticsLabel = rejectionReason;

        mAnalyticsHelper.sendEvent(
                mContext.getString(R.string.analytics_event_category_middleware),
                mContext.getString(R.string.analytics_event_action_middleware_rejected),
                analyticsLabel
        );

        replyServer(middlewareMessage, rejectionReason);
    }

    public void rejectDueToCallAlreadyInProgress(MiddlewareMessage middlewareMessage) {
        mRemoteLogger.d("Reject due to call in progress");

        String rejectionReason = mContext.getString(R.string.analytics_event_label_declined_another_call_in_progress);

        mAnalyticsHelper.sendEvent(
                mContext.getString(R.string.analytics_event_category_call),
                mContext.getString(R.string.analytics_event_action_inbound),
                rejectionReason
        );

        replyServer(middlewareMessage, rejectionReason);
    }

    public void rejectDueToUserDeclining(MiddlewareMessage middlewareMessage) {
        mRemoteLogger.d("Reject due to user declining");

        String rejectionReason = mContext.getString(R.string.analytics_event_label_declined);

        mAnalyticsHelper.sendEvent(
                mContext.getString(R.string.analytics_event_category_call),
                mContext.getString(R.string.analytics_event_action_inbound),
                rejectionReason
        );

        replyServer(middlewareMessage, rejectionReason);
    }

    /**
     * Notify the middleware server that we are, in fact, alive.
     */
    private void replyServer(MiddlewareMessage middlewareMessage, String rejectionReason) {
        mRemoteLogger.d("replyServer");

        createApi(middlewareMessage, false)
                .reply(middlewareMessage.getRequestToken(), false, middlewareMessage.getMessageStartTime())
                .enqueue(callback);

        sendRejectionReasonToServer(middlewareMessage, rejectionReason);
    }

    /**
     * Sends the rejection reason to the relevant end-point.
     *
     * @param middlewareMessage
     * @param reason
     */
    private void sendRejectionReasonToServer(MiddlewareMessage middlewareMessage, String reason) {
        String sipUserId = ((PhoneAccount) mJsonStorage.get(PhoneAccount.class)).getAccountId();

        createApi(middlewareMessage, true)
                .rejectReason(sipUserId, middlewareMessage.getRequestToken(), reason)
                .enqueue(callback);
    }

    /**
     * Creates the API.
     *
     * @param middlewareMessage
     * @return
     */
    private Registration createApi(MiddlewareMessage middlewareMessage, boolean withBasicAuth) {
        if(withBasicAuth) {
            return ServiceGenerator.createService(
                    mContext,
                    Registration.class,
                    middlewareMessage.getResponseUrl(),
                    mAccountHelper.getEmail(),
                    mAccountHelper.getPassword()
            );
        }

        return ServiceGenerator.createService(
                mContext,
                Registration.class,
                middlewareMessage.getResponseUrl()
        );
    }
}
