package tr.monerostatus.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import tr.monerostatus.DataContainer;
import tr.monerostatus.app.CalcFragment;
import tr.monerostatus.jsondata.BlockExplorerDataParser;
import tr.monerostatus.jsondata.CoinMarketCapNorthpoleParser;
import tr.monerostatus.jsondata.CoinMarketCapParser;
import tr.monerostatus.jsondata.ExchangeDataParser;
import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeTickerData;
import tr.monerostatus.jsondata.FixerIOParser.FixerIO;
import tr.monerostatus.jsondata.blockexplorers.ChainRadarParser.ChainRadar;

public class WidgetUpdaterTools{
	
	public static final String SIZE_MEDIUM = "medium-2x1";
	public static final String SIZE_LARGE = "large-2x2";
	
    private static List<Class<? extends Service>> tmpUpdaters = new ArrayList<Class<? extends Service>>();
    static{
    	tmpUpdaters.add(WidgetUpdaterMedium.class);
    	tmpUpdaters.add(WidgetUpdaterLarge.class);
    }
    public static final List<Class<? extends Service>> updaters = Collections.unmodifiableList(tmpUpdaters);
    
//    public static void startUpdaters(Context context){
//    	for (Class<? extends Service> updater: updaters)
//    		context.startService(new Intent(context.getApplicationContext(), updater));
//    }
	
	public static void processInitialData(Object lastData, Context context, CoinWidgetUpdater updater) {
		//Context context = getApplicationContext();
		SharedPreferences prefs = context.getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0);
		SharedPreferences.Editor prefsEd = prefs.edit();
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		
		ComponentName widget = new ComponentName(context, updater.getCoinWidgetProvider());
		int[] ids = appWidgetManager.getAppWidgetIds(widget);

		Float totalVolume = 0f;
		
		//sum up exchanges' volumes
		for (String exchange: ExchangeDataParser.exchangeCodes)
			if (DataContainer.getExchangeTickerData(exchange) != null)
				totalVolume += DataContainer.getExchangeTickerData(exchange).getVolume();
        
    	for (int appWidgetId:ids){
            String exchange = prefs.getString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE, null);
            String currency = prefs.getString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CURRENCY, null);


    		Float xmrPriceBtc = 0f;
    		Double btcPriceCurrency = 0d;
            
            if (exchange == null || currency == null)
            	continue;
            
            boolean dummyUpdate = false; // true when updating widget only to signify update's end
            
    		if (lastData instanceof ExchangeTickerData){//any exchange data received
    			if (((ExchangeTickerData)lastData).getExchange().equals(exchange) &&  DataContainer.getExchangeTickerData(exchange) != null){//chosen exchange data received
        			xmrPriceBtc = DataContainer.getExchangeTickerData(exchange).getPrice();
					prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_PRICE_BTC, xmrPriceBtc);
					prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CHANGE, DataContainer.getExchangeTickerData(exchange).getChange());
	            	if (updater.getSize().equals(SIZE_LARGE))
	            		prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE_VOLUME_CURRENCY, DataContainer.getExchangeTickerData(exchange).getVolume());
    			}
			}
			else if ((lastData instanceof FixerIO || (lastData instanceof CoinMarketCapNorthpoleParser.CoinMarketCap && DataContainer.getCoinmarketcapBtcData() != null)) &&
                    DataContainer.getFixerIOData() != null && DataContainer.getCoinmarketcapBtcData() != null){
				btcPriceCurrency = DataContainer.getBTCPriceCurrency(currency);
				prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_BTC_PRICE_CURRENCY, btcPriceCurrency.intValue());
			}
			else if(lastData instanceof BlockExplorerDataParser.BlockExplorerData && DataContainer.getBlockExplorerData() != null){
				prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_HASHRATE, DataContainer.getBlockExplorerData().getHashrate().floatValue());
			}
			else if (lastData instanceof CoinMarketCapParser.CoinMarketCap && DataContainer.getXMRVolumeCurrency(currency) != null) {
    			prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_VOLUME_CURRENCY, DataContainer.getXMRVolumeCurrency(currency));
			}
			else if (!DataContainer.isFetching()){
				dummyUpdate = true;
			}
			else{
				continue;
			}
            
            //special case to calculate monero's price in chosen currency, using chosen exchange and bitcoinAverage price
            if ((lastData instanceof ExchangeTickerData || lastData instanceof FixerIO) && DataContainer.getExchangeTickerData(exchange) != null && DataContainer.getFixerIOData() != null && DataContainer.getCoinmarketcapBtcData() != null){
            	xmrPriceBtc = DataContainer.getExchangeTickerData(exchange).getPrice();
                btcPriceCurrency = DataContainer.getBTCPriceCurrency(currency);
            	prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_PRICE_CURRENCY, Double.valueOf(xmrPriceBtc * btcPriceCurrency).floatValue());
            }
            
            //special case to calculate market capitalization &&
            //special case to calculate user's coin's value and daily mining projection based on hashrate
            if (updater.getSize().equals(SIZE_LARGE)
	            	&& (lastData instanceof ExchangeTickerData || lastData instanceof FixerIO || lastData instanceof ChainRadar)
	            	&& DataContainer.getExchangeTickerData(exchange) != null && DataContainer.getBTCPriceCurrency(currency) != null && DataContainer.getBlockExplorerData() != null){
            	
            	xmrPriceBtc = DataContainer.getExchangeTickerData(exchange).getPrice();
                btcPriceCurrency = DataContainer.getBTCPriceCurrency(currency);
            	
            	//get user values
            	SharedPreferences pref = context.getSharedPreferences(CalcFragment.PREFS_NAME, 0);
                Float userCoinsValue = pref.getFloat(CalcFragment.PREF_USER_COINS, 0f);
                Float userHashrateValue = pref.getFloat(CalcFragment.PREF_USER_HASHRATE, 0f);
                
                //calculate mining projection
            	Double projection = 0d;
				projection = DataContainer.getBlockExplorerData().getReward()*30d*24d*(userHashrateValue/(Double.valueOf(DataContainer.getBlockExplorerData().getHashrate()*1000000)));
				projection *= 0.9; //10% orphan
				
				prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_COINS_CURRENCY, Double.valueOf(userCoinsValue * xmrPriceBtc * btcPriceCurrency).floatValue());
				prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_PROJECTION_CURRENCY, Double.valueOf(projection * xmrPriceBtc * btcPriceCurrency).floatValue());
				prefsEd.putFloat(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_MARKET_CAP, Double.valueOf(xmrPriceBtc * btcPriceCurrency * DataContainer.getBlockExplorerData().getTotalCoins()).intValue());
            }
            	
			
			RemoteViews remoteViews = CoinWidgetTools.prepareViews(context, appWidgetId, updater.getCoinWidgetProvider(), updater.getWidgetLayout());
			
			if (!dummyUpdate){
				String dateString = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date());
				prefsEd.putString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_TIMESTAMP, dateString);
				prefsEd.apply();
			}
			
			updater.updateWithPreparedViews(context, remoteViews, appWidgetId);
    	}
    	
    	if (!DataContainer.isFetching()) {
            updater.endUpdate();
        }
	}
	
	public static interface CoinWidgetUpdater{
		
		public int getWidgetLayout();
		
		public Class<? extends AppWidgetProvider> getCoinWidgetProvider();
		
		public void updateWithPreparedViews(Context context, RemoteViews remoteViews, int appWidgetId);
		
		public void endUpdate();
		
		public String getSize();
	}
} 