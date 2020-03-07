package tr.monerostatus.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tr.monerostatus.DataContainer;
import tr.monerostatus.DataContainer.Refreshable;
import tr.monerostatus.R;

public class WidgetUpdaterMedium extends Service implements Refreshable, WidgetUpdaterTools.CoinWidgetUpdater {

	private static Set<Class<?>> baseRequirements = new HashSet<Class<?>>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		ComponentName widget = new ComponentName(context,CoinWidgetMedium.class);
    	int[] ids = appWidgetManager.getAppWidgetIds(widget);

        List<String> currentExchanges = new ArrayList<String>();
        SharedPreferences prefs = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0);
        for (int appWidgetId:ids){
            String exchange = prefs.getString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE, null);
            currentExchanges.add(exchange);
        }


        DataContainer.registerRefreshable(this);
        DataContainer.fetchWidgetData(getResources(), currentExchanges);

        //Set refresh icon to active
    	for (int appWidgetId:ids)
    		CoinWidgetMedium.updateWidget(context, CoinWidgetTools.prepareViews(context, appWidgetId, getCoinWidgetProvider(), getWidgetLayout()), appWidgetId);

    	return START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent){
		return null;
	}
	
	@Override
	public void onDestroy(){
		DataContainer.unregisterRefreshable(this);
	}

	@Override
	public int getWidgetLayout() {
		return R.layout.widget_medium;
	}

	@Override
	public Class<? extends AppWidgetProvider> getCoinWidgetProvider() {
		return CoinWidgetMedium.class;
	}

	public void updateWithPreparedViews(Context context, RemoteViews remoteViews, int appWidgetId) {
		CoinWidgetMedium.updateWidget(context, remoteViews, appWidgetId);		
	}

	public void refresh(Object lastData) {
		WidgetUpdaterTools.processInitialData(lastData, getApplicationContext(), this);
	}

    @Override
    public Set<Class<?>> continuousRequirements() {
        return baseRequirements;
    }

    public void endUpdate() {
		stopSelf();
	}
	
	public String getSize(){
		return WidgetUpdaterTools.SIZE_MEDIUM;
	}

}
