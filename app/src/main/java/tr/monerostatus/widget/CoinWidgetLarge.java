package tr.monerostatus.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.NumberFormat;

import tr.monerostatus.R;
import tr.monerostatus.experimental.GcmIntentService;

public class CoinWidgetLarge extends AppWidgetProvider{

	@Override
    public void onEnabled(Context context){
    	super.onEnabled(context);
    }
    
    @Override
    public void onDisabled(Context context){
    	super.onDisabled(context);
    	
//        SharedPreferences.Editor prefsEd = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0).edit();
//        prefsEd.clear();
//        prefsEd.apply();
    }
    
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		for (int appWidgetId:appWidgetIds)
				updateWidget(context, CoinWidgetTools.prepareViews(context, appWidgetId, this.getClass(), R.layout.widget_large), appWidgetId);

		context.startService(new Intent(context.getApplicationContext(), WidgetUpdaterLarge.class));

        Intent refreshGCMIntent = new Intent(context, GcmIntentService.class);
        refreshGCMIntent.setAction(GcmIntentService.ACTION_REFRESH_ALERTS);
        context.startService(refreshGCMIntent);
    }
	
	@Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);
        if (intent.getAction().equals(CoinWidgetTools.MONERO_APPWIDGET_UPDATE)){                	
        	Toast.makeText(context, context.getString(R.string.refresh_toast), Toast.LENGTH_SHORT).show();

			context.startService(new Intent(context.getApplicationContext(), WidgetUpdaterLarge.class));
        }
        	
        
    }
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds){
		super.onDeleted(context, appWidgetIds);
		
		for(int appWidgetId: appWidgetIds){
	        SharedPreferences.Editor prefsEd = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0).edit();
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_PRICE_BTC);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CHANGE);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_BTC_PRICE_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_PRICE_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_VOLUME_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE_VOLUME_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_HASHRATE);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_TIMESTAMP);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_COINS_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_PROJECTION_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_MARKET_CAP);
	        prefsEd.apply();
		}
	}
	
	public static void updateWidget(Context context, RemoteViews remoteViews, int appWidgetId) {
		StringBuilder currencySymbol = new StringBuilder();
		remoteViews = CoinWidgetTools.refineViews(context, remoteViews, appWidgetId, currencySymbol);
		if (remoteViews == null)
			return;
		
		SharedPreferences prefs = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0);
        		
		remoteViews.setTextViewText(R.id.widget_text_exchange_volume, NumberFormat.getInstance().format(Float.valueOf(prefs.getFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE_VOLUME_CURRENCY, 0)*prefs.getFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_BTC_PRICE_CURRENCY, 0)).intValue())+currencySymbol);
		remoteViews.setTextViewText(R.id.widget_text_user_coins_currency, String.format("%.2f", prefs.getFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_COINS_CURRENCY, -1))+currencySymbol);
		remoteViews.setTextViewText(R.id.widget_text_user_projection_currency,  String.format("%.2f", prefs.getFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_PROJECTION_CURRENCY, -1))+currencySymbol);
		remoteViews.setTextViewText(R.id.widget_text_market_capitalization,  NumberFormat.getInstance().format(Float.valueOf(prefs.getFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_MARKET_CAP, 0)).intValue())+currencySymbol);
				
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		widgetManager.updateAppWidget(appWidgetId, remoteViews);
	}
}