package com.voipgrid.vialer.fcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.middleware.MiddlewareMessage;
import com.voipgrid.vialer.middleware.handlers.MiddlewareCallMessageHandler;

/**
 * Listen to messages from FCM. The backend server sends us FCM notifications when we have
 * incoming calls.
 */
public class FcmMessagingService extends FirebaseMessagingService {

    private RemoteLogger mRemoteLogger;
    private MiddlewareCallMessageHandler mMiddlewareCallMessageHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mRemoteLogger = new RemoteLogger(FcmMessagingService.class).enableConsoleLogging();
        mMiddlewareCallMessageHandler = new MiddlewareCallMessageHandler(this);
        mRemoteLogger.d("onCreate");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        MiddlewareMessage middlewareMessage = MiddlewareMessage.Builder.fromRemoteMessage(remoteMessage);

        LogHelper.using(mRemoteLogger).logMiddlewareMessageReceived(remoteMessage, middlewareMessage.getRequestToken());

        if(!middlewareMessage.hasRequestType()) {
            mRemoteLogger.e("No requestType");
            return;
        }

        if (middlewareMessage.isCall()) {
            mMiddlewareCallMessageHandler.handle(middlewareMessage);
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
