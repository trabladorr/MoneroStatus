package tr.monerostatus.experimental;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import tr.monerostatus.R;
import tr.monerostatus.app.MainActivity;
//import android.support.v4.NotificationCompat;

public class GcmHandlerService extends GcmListenerService {
    public static final int NOTIFICATION_ID = 1;
    public static final String TAG = "DBG";
    
    private NotificationManager mNotificationManager;
    //NotificationCompat.Builder builder;

    public GcmHandlerService() {
        super();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        Context context = getApplicationContext();
        String exchange = AlarmFragment.getAlertExchange(context);
        if (data.containsKey(exchange)) {
            Float exchangePrice = Float.parseFloat(data.getString(exchange));
            if ((AlarmFragment.isAlertLowerOn(context) && AlarmFragment.getAlertLower(context) > exchangePrice) ||
                (AlarmFragment.isAlertHigherOn(context) && exchangePrice > AlarmFragment.getAlertHigher(context))){
                sendNotification(exchange, data.getString(exchange));
            }
        }
    }

    private void sendNotification(String exchange, String value) {
        //TODO: disable alerts button

        String msg = getApplicationContext().getString(R.string.alarm_notification_message).replace("$$$",value).replace("%%%",exchange);
        String title = getApplicationContext().getString(R.string.alarm_notification_title);

        mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent disableIntent = new Intent(getApplicationContext(), GcmIntentService.class);
        disableIntent.setAction(GcmIntentService.ACTION_DISABLE_ALERTS);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        PendingIntent disableIntentPending = PendingIntent.getService(getApplicationContext(), 0, disableIntent, 0);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.monero_circle);

        //NotificationCompat.Action disableAction = new NotificationCompat.Action.Builder(R.drawable.ic_clear_search_api_holo_light,getApplicationContext().getString(R.string.alarm_disable_alerts),disableIntentPending).build();
        //fix for deprecated, requires API 20

        Notification.Builder mBuilder = new Notification.Builder(this)
            .setSmallIcon(R.drawable.monero_notification_icon)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setStyle(new Notification.BigTextStyle()
            .bigText(msg))
            .setContentText(msg)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(contentIntent)
            .setVibrate(new long[]{200l, 100l, 400l, 100l, 100l})
            .addAction(R.drawable.ic_clear_search_api_holo_light,getApplicationContext().getString(R.string.alarm_disable_alerts),disableIntentPending);

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }
}