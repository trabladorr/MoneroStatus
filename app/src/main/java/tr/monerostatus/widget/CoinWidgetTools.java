package tr.monerostatus.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tr.monerostatus.DataContainer;
import tr.monerostatus.R;
import tr.monerostatus.app.MainActivity;

public class CoinWidgetTools{
	
    public static final String PACKAGE_NAME = "tr.monerostatus";
    public static final String MONERO_APPWIDGET_UPDATE = PACKAGE_NAME+".widget."+"MONERO_APPWIDGET_UPDATE";
	
    public static final String PREFS_NAME = "XMRWidget";
    public static final String PREF_PREFIX_KEY = "xmrwidget_";
    public static final String PREF_TIMESTAMP = "_timestamp";
    public static final String PREF_PRICE_BTC = "_price_btc";
    public static final String PREF_CHANGE = "_change";
    public static final String PREF_BTC_PRICE_CURRENCY = "_btc_price";
    public static final String PREF_PRICE_CURRENCY = "_price_currency";
    public static final String PREF_VOLUME_CURRENCY = "_volume_currency";
    public static final String PREF_EXCHANGE_VOLUME_CURRENCY = "_exchange_volume_currency";
    public static final String PREF_HASHRATE = "_hashrate_mh";
    public static final String PREF_CURRENCY = "_currency";
    public static final String PREF_EXCHANGE = "_exchange";
    public static final String PREF_USER_COINS_CURRENCY = "_user_coins_currency";
    public static final String PREF_USER_PROJECTION_CURRENCY = "_user_projection_currency";
    public static final String PREF_MARKET_CAP = "_market_capitalization";
    
    private static List<Class<? extends AppWidgetProvider>> tmpProviders = new ArrayList<Class<? extends AppWidgetProvider>>();
    static{
    	tmpProviders.add(CoinWidgetMedium.class);
    	tmpProviders.add(CoinWidgetLarge.class);
    }
    public static final List<Class<? extends AppWidgetProvider>> providers = Collections.unmodifiableList(tmpProviders);
    
	public static RemoteViews prepareViews(Context context, Integer appWidgetId, Class<? extends AppWidgetProvider> provider, int layout){
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layout);
		SharedPreferences pref = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0);
		
		Intent mainIntent = new Intent(context, MainActivity.class);
		mainIntent.setAction("android.intent.action.MAIN");
        mainIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        String currency = pref.getString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CURRENCY, null);
        mainIntent.putExtra("CURRENCY", currency);
        String exchange = pref.getString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE, null);
        mainIntent.putExtra("EXCHANGE", exchange);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        remoteViews.setOnClickPendingIntent(R.id.monero_logo_imageView, PendingIntent.getActivity(context, appWidgetId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        remoteViews.setOnClickPendingIntent(R.id.widget_text_btc_price, PendingIntent.getActivity(context, appWidgetId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        remoteViews.setOnClickPendingIntent(R.id.widget_btc_price_symbol, PendingIntent.getActivity(context, appWidgetId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        
		Intent refeshIntent = new Intent(context, provider);
        refeshIntent.setAction(MONERO_APPWIDGET_UPDATE);
        remoteViews.setOnClickPendingIntent(R.id.refesh_imageView, PendingIntent.getBroadcast(context, appWidgetId, refeshIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        
        if (provider.equals(CoinWidgetMedium.class))
        	remoteViews.setOnClickPendingIntent(R.id.widget_hashrate, PendingIntent.getBroadcast(context, appWidgetId, refeshIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        else if (provider.equals(CoinWidgetLarge.class))
        	remoteViews.setOnClickPendingIntent(R.id.widget_clickable_space, PendingIntent.getBroadcast(context, appWidgetId, refeshIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        	
        	
        
        if (DataContainer.isFetching())
        	remoteViews.setImageViewResource(R.id.refesh_imageView, R.drawable.refresh_active);
        else
        	remoteViews.setImageViewResource(R.id.refesh_imageView, R.drawable.refresh_inactive);
        
        if (exchange != null)
        	remoteViews.setTextViewText(R.id.widget_text_exchange, context.getResources().getString(context.getResources().getIdentifier("widget_exchange_"+exchange, "string", PACKAGE_NAME)));
                
		return remoteViews;
	}

	public static int getColorForChange(Float change, Resources res){
		TypedValue neg_high = new TypedValue(), neg_med = new TypedValue(), neg_low = new TypedValue(), pos_low = new TypedValue(), pos_med = new TypedValue(), pos_high = new TypedValue();
		res.getValue(R.dimen.change_negative_high_boundary, neg_high, true);
		res.getValue(R.dimen.change_negative_medium_boundary, neg_med, true);
		res.getValue(R.dimen.change_negative_low_boundary, neg_low, true);
		res.getValue(R.dimen.change_positive_low_boundary, pos_low, true);
		res.getValue(R.dimen.change_positive_medium_boundary, pos_med, true);
		res.getValue(R.dimen.change_positive_high_boundary, pos_high, true);
		
		int resId;
		
		if (change <= neg_high.getFloat())
			resId =  R.color.widget_change_negative_high;
		else if (change <= neg_med.getFloat())
			resId =  R.color.widget_change_negative_medium;
		else if (change <= neg_low.getFloat())
			resId =  R.color.widget_change_negative_low;
		else if (change <= pos_low.getFloat())
			resId =  R.color.widget_change_none;
		else if (change <= pos_med.getFloat())
			resId =  R.color.widget_change_positive_low;
		else if (change <= pos_high.getFloat())
			resId =  R.color.widget_change_positive_medium;
		else
			resId =  R.color.widget_change_positive_high;
		
		return res.getColor(resId);
	}
	
	public static RemoteViews refineViews(Context context, RemoteViews remoteViews, int appWidgetId, final StringBuilder currencySymbol) {
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		Resources resources = context.getResources();
		
		SharedPreferences prefs = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0);
        String currency = prefs.getString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CURRENCY, null);

        if (currency == null || prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_TIMESTAMP,"").equals("")){
        	widgetManager.updateAppWidget(appWidgetId, remoteViews);
        	return null;
        }
        		
        prefs = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0);
        
		Double megahash = Double.valueOf(prefs.getFloat(PREF_PREFIX_KEY + appWidgetId + PREF_HASHRATE, -1));
		
		try{
			currencySymbol.append(resources.getString(resources.getIdentifier(currency+"_symbol", "string", PACKAGE_NAME)));
		}
		catch(Exception e){
			currencySymbol.append("?");
		}

    	remoteViews.setImageViewResource(R.id.widget_price_btc_symbol, R.drawable.bitcoin_symbol);
    	remoteViews.setImageViewResource(R.id.widget_btc_price_symbol, R.drawable.bitcoin_symbol);

    	remoteViews.setTextColor(R.id.widget_text_price_btc, getColorForChange(prefs.getFloat(PREF_PREFIX_KEY + appWidgetId + PREF_CHANGE, 0), resources));
    	
		remoteViews.setTextViewText(R.id.widget_text_time, prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_TIMESTAMP,""));
		remoteViews.setTextViewText(R.id.widget_text_price_btc, String.format("%.5f", prefs.getFloat(PREF_PREFIX_KEY + appWidgetId + PREF_PRICE_BTC, -1)));
		remoteViews.setTextViewText(R.id.widget_text_btc_price, "=" +  NumberFormat.getInstance().format(Float.valueOf(prefs.getFloat(PREF_PREFIX_KEY + appWidgetId + PREF_BTC_PRICE_CURRENCY, -1)).intValue())+currencySymbol);
		remoteViews.setTextViewText(R.id.widget_text_price_currency, String.format("%.2f", prefs.getFloat(PREF_PREFIX_KEY + appWidgetId + PREF_PRICE_CURRENCY, -1))+currencySymbol);
		remoteViews.setTextViewText(R.id.widget_text_volume, NumberFormat.getInstance().format(Float.valueOf(prefs.getFloat(PREF_PREFIX_KEY + appWidgetId + PREF_VOLUME_CURRENCY, 0)).intValue())+currencySymbol);
		remoteViews.setTextViewText(R.id.widget_hashrate, resources.getString(R.string.hashrate)+String.format("%.2f", megahash)+resources.getString(R.string.megahash_per_second_symbol));
		
		return remoteViews;
	}

	
}
