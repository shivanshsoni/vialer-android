package com.voipgrid.vialer.middleware;

import static com.voipgrid.vialer.middleware.MiddlewareMessage.Builder.SUPPRESSED;

import android.content.Context;
import android.support.annotation.StringDef;

import com.google.firebase.messaging.RemoteMessage;
import com.voipgrid.vialer.R;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MiddlewareMessage implements Serializable {
    @StringDef({CALL_REQUEST_TYPE, MESSAGE_REQUEST_TYPE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Types {}
    public final static String CALL_REQUEST_TYPE = "call";
    public final static String MESSAGE_REQUEST_TYPE = "message";

    private String responseUrl, requestToken, phoneNumber, callerId, messageStartTime, requestType, from;

    public MiddlewareMessage(String responseUrl, String requestToken, String phoneNumber,
            String callerId, String messageStartTime, @Types String requestType,
            String from) {
        this.responseUrl = responseUrl;
        this.requestToken = requestToken;
        this.phoneNumber = phoneNumber;
        this.callerId = callerId;
        this.messageStartTime = messageStartTime;
        this.requestType = requestType;
        this.from = from;
    }

    /**
     * Determine if the message if of the given type.
     *
     * @param type The type to test against.
     * @return boolean TRUE if the message is of the provided type.
     */
    public boolean is(@Types String type) {
        return requestType.equals(type);
    }

    public boolean isCall() {
        return is(CALL_REQUEST_TYPE);
    }

    public boolean isMessage() {
        return is(MESSAGE_REQUEST_TYPE);
    }

    public boolean isNumberSuppressed() {
        return phoneNumber != null && (phoneNumber.equalsIgnoreCase(SUPPRESSED) || phoneNumber.toLowerCase().contains("xxxx"));
    }

    public boolean hasRequestType() {
        return ! requestType.equals("");
    }

    public String getRequestType() {
        return requestType;
    }

    public String getResponseUrl() {
        return responseUrl;
    }

    public String getRequestToken() {
        return requestToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns the phone number, unless it is suppressed then it will return the suppressed string.
     *
     * @param context
     * @return
     */
    public String getAdjustedPhoneNumber(Context context) {
        return isNumberSuppressed() ? context.getString(R.string.supressed_number) : getPhoneNumber();
    }

    public String getCallerId() {
        return callerId;
    }

    public String getMessageStartTime() {
        return messageStartTime;
    }

    public String getFrom() {
        return from;
    }

    @Override
    public String toString() {
        return "PushMessage{" +
                "responseUrl='" + responseUrl + '\'' +
                ", requestToken='" + requestToken + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", callerId='" + callerId + '\'' +
                ", messageStartTime='" + messageStartTime + '\'' +
                ", requestType='" + requestType + '\'' +
                ", from='" + from + '\'' +
                '}';
    }

    public static class Builder {
        final static String MESSAGE_TYPE = "type";
        final static String RESPONSE_URL = "response_api";
        final static String REQUEST_TOKEN = "unique_key";
        final static String PHONE_NUMBER = "phonenumber";
        final static String CALLER_ID = "caller_id";
        final static String SUPPRESSED = "supressed";
        public final static String MESSAGE_START_TIME = "message_start_time";

        private RemoteMessage mRemoteMessage;

        protected MiddlewareMessage build(RemoteMessage remoteMessage) {
            mRemoteMessage = remoteMessage;

            return new MiddlewareMessage(
                    stringFromData(RESPONSE_URL),
                    stringFromData(REQUEST_TOKEN),
                    stringFromData(PHONE_NUMBER),
                    stringFromData(CALLER_ID),
                    stringFromData(MESSAGE_START_TIME),
                    stringFromData(MESSAGE_TYPE),
                    mRemoteMessage.getFrom()
            );
        }

        /**
         * Attempts to build a middleware message from a remote message.
         *
         * @param remoteMessage
         * @return The MiddlewareMessage or NULL if the message cannot be built.
         */
        public static MiddlewareMessage fromRemoteMessage(RemoteMessage remoteMessage) {
            return new MiddlewareMessage.Builder().build(remoteMessage);
        }

        private String stringFromData(String key) {
            return mRemoteMessage.getData().get(key) != null ? mRemoteMessage.getData().get(key) : "";
        }
    }
}
