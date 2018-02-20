package com.voipgrid.vialer.fcm;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.RemoteLogger;

/**
 * Listen to messages from FCM. The backend server sends us FCM notifications when we have
 * incoming calls.
 */
public class FcmMessagingService extends FirebaseMessagingService {

    private RemoteLogger mRemoteLogger;
    private CallMiddlewareMessageHandler mCallMiddlewareMessageHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mRemoteLogger = new RemoteLogger(FcmMessagingService.class).enableConsoleLogging();
        mCallMiddlewareMessageHandler = new CallMiddlewareMessageHandler(this);
        mRemoteLogger.d("onCreate");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        MiddlewareMessage middlewareMessage = MiddlewareMessage.Builder.fromRemoteMessage(remoteMessage);
        Log.e("TEST123", "message:" + middlewareMessage);
        if(!middlewareMessage.hasRequestType()) {
            mRemoteLogger.e("No requestType");
            return;
        }

        LogHelper.using(mRemoteLogger).logMiddlewareMessageReceived(remoteMessage, middlewareMessage.getRequestToken());

        if (middlewareMessage.isCall()) {
            mCallMiddlewareMessageHandler.handle(middlewareMessage);
            return;
        }

        mRemoteLogger.d("No handlers are available fo middleware message type: " + middlewareMessage.getRequestType());
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        mRemoteLogger.d("Message deleted on the FCM server.");
    }
}
