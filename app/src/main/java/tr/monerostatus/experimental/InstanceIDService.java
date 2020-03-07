package tr.monerostatus.experimental;

import com.google.android.gms.iid.InstanceIDListenerService;

public class InstanceIDService extends InstanceIDListenerService {
    public void onTokenRefresh() {
        //Toast.makeText(getApplicationContext(), "GCM Token Refreshed", Toast.LENGTH_SHORT).show();
    }
}
