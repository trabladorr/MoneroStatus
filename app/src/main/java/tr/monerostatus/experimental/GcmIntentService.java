package tr.monerostatus.experimental;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class GcmIntentService extends IntentService {
    public static final String ACTION_DISABLE_ALERTS = "tr.monerostatus.alert.DISABLE_ALERTS";
    public static final String ACTION_REFRESH_ALERTS = "tr.monerostatus.alert.REFRESH_ALERTS";

    public static void startActionFoo(Context context, String param1, String param2) {
    }


    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DISABLE_ALERTS.equals(action)) {
                disableAlerts();
            }
            else if (ACTION_REFRESH_ALERTS.equals(action)) {
                refreshActiveAlerts();
            }
        }
    }

    private void disableAlerts() {
        Context context = getApplicationContext();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        try {
            String token = InstanceID.getInstance(context).getToken(AlarmFragment.SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        }
        catch(IOException e){
            return;
        }
        if (AlarmFragment.isAlertLowerOn(context))
            AlarmFragment.setAlert(context, gcm, AlarmFragment.LOWER, null, false, null);
        if (AlarmFragment.isAlertHigherOn(context))
            AlarmFragment.setAlert(context, gcm, AlarmFragment.HIGHER, null, false, null);

        NotificationManager mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(GcmHandlerService.NOTIFICATION_ID);

    }

    private void refreshActiveAlerts() {
        Context context = getApplicationContext();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        try {
            String token = InstanceID.getInstance(context).getToken(AlarmFragment.SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        }
        catch(IOException e){
            return;
        }
        if (AlarmFragment.isAlertLowerOn(context) || AlarmFragment.isAlertHigherOn(context))
            AlarmFragment.resendAlerts(context, gcm);
    }
}
