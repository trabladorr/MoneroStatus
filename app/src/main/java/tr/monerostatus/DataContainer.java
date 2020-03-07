package tr.monerostatus;

import android.content.res.Resources;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tr.monerostatus.jsondata.BlockExplorerDataParser;
import tr.monerostatus.jsondata.CoinMarketCapParser;
import tr.monerostatus.jsondata.CoinMarketCapParser.CoinMarketCap;
import tr.monerostatus.jsondata.ExchangeDataParser;
import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeOrderData;
import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeTickerData;
import tr.monerostatus.jsondata.FixerIOParser;
import tr.monerostatus.jsondata.FixerIOParser.FixerIO;
import tr.monerostatus.jsondata.JSONRetriever;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

;

public class DataContainer implements JSONUpdatable {
	private static CoinMarketCap coinmarketcapXmrData = null;
	private static CoinMarketCap coinmarketcapBtcData = null;
//	private static BitcoinAverage bitcoinAverageData = null;
	private static FixerIO fixerIOData = null;
	private static Map<String,ExchangeTickerData> exchangeTickerData = new HashMap<String,ExchangeTickerData>();
	private static Map<String,ExchangeOrderData> exchangeOrderData = new HashMap<String,ExchangeOrderData>();
	private static Map<String,Map<String, ExchangeTickerData>> exchangeMarketData = new HashMap<String,Map<String, ExchangeTickerData>>();
	private static Map<String,BlockExplorerDataParser.BlockExplorerData> blockExplorerData = new HashMap<String,BlockExplorerDataParser.BlockExplorerData>();

	private static final Object coinmarketcapXmrDataLock = new Object();
	private static final Object coinmarketcapBtcDataLock = new Object();
//	private static final Object bitcoinAverageDataLock = new Object();
	private static final Object fixerIODataLock = new Object();
	private static final Object exchangeTickerDataLock = new Object();
	private static final Object exchangeOrderDataLock = new Object();
	private static final Object exchangeMarketDataLock = new Object();
	private static final Object blockExplorerDataLock = new Object();

    private static Map<String,Float> btcExchangeRate = new HashMap<String,Float>();
	
	private static boolean continuous = false;
	private static Set<Refreshable> refreshables = new HashSet<Refreshable>();

	private static final Object continuousLock = new Object();
	private static final Object refreshLock = new Object();
	
	public static void registerRefreshable(Refreshable r){
		synchronized(refreshLock){
			refreshables.add(r);
		}
	}
	
	public static void unregisterRefreshable(Refreshable r){
		synchronized(refreshLock){
			refreshables.remove(r);
		}
	}
	
	public static void fetchAllData(Resources resources){
		DataContainer dummy = new DataContainer();

		CoinMarketCapParser.get("monero", dummy, resources);
		CoinMarketCapParser.get("bitcoin", dummy, resources);

		for (String explorer: BlockExplorerDataParser.explorerCodes) {
			BlockExplorerDataParser.getData(explorer, dummy, resources);
		}

//		BitcoinAverageParser.get(dummy, resources);
		FixerIOParser.get(dummy, resources);

		for (String exchange: ExchangeDataParser.exchangeCodes){
			ExchangeDataParser.getTicker(exchange, "XMR", "BTC", dummy, resources);
			ExchangeDataParser.getOrders(exchange, "XMR", "BTC", dummy, resources);
			ExchangeDataParser.getMarkets(exchange, "XMR", dummy, resources);
		}
	}

    public static void fetchWidgetData(Resources resources, List<String> exchanges){
        DataContainer dummy = new DataContainer();

		CoinMarketCapParser.get("monero", dummy, resources);
		CoinMarketCapParser.get("bitcoin", dummy, resources);

		for (String explorer: BlockExplorerDataParser.explorerCodes) {
			BlockExplorerDataParser.getData(explorer, dummy, resources);
		}

//        BitcoinAverageParser.get(dummy, resources);
		FixerIOParser.get(dummy, resources);

		if (exchanges != null){
			for (String exchange: exchanges){
				ExchangeDataParser.getTicker(exchange, "XMR", "BTC", dummy, resources);
			}
		}
		else {
			for (String exchange : ExchangeDataParser.exchangeCodes) {
				ExchangeDataParser.getTicker(exchange, "XMR", "BTC", dummy, resources);
			}
		}
    }

    public static void fetchRequiredData(Resources resources){
        DataContainer dummy = new DataContainer();

        Set<Refreshable> refreshablesCopy;
        synchronized(refreshLock) {
            refreshablesCopy = new HashSet<Refreshable>(refreshables);
        }

        Set<Class<?>> continuousRequirements = new HashSet<Class<?>>();

        for(Refreshable r:refreshablesCopy) {
//            Log.d("DBG: DataContainer", r.getClass().toString());
            continuousRequirements.addAll(r.continuousRequirements());
        }

//        for(Class<?> c:continuousRequirements) {
//            Log.d("DBG: DataContainer", c.toString());
//        }

        if (continuousRequirements.contains(CoinMarketCapParser.class)) {
			CoinMarketCapParser.get("monero", dummy, resources);
			CoinMarketCapParser.get("bitcoin", dummy, resources);
        }

        if (continuousRequirements.contains(BlockExplorerDataParser.class)) {
            for (String explorer : BlockExplorerDataParser.explorerCodes) {
                BlockExplorerDataParser.getData(explorer, dummy, resources);
            }
        }

//		if (continuousRequirements.contains(BitcoinAverageParser.class)) {
//			BitcoinAverageParser.get(dummy, resources);
//		}

		if (continuousRequirements.contains(FixerIOParser.class)) {
			FixerIOParser.get(dummy, resources);
		}

        for (Class<?> exchangeClass: ExchangeDataParser.exchangeClasses){
            if (continuousRequirements.contains(exchangeClass)) {
                String exchange = null;
                try {
                    exchange = (String) exchangeClass.getField("exchange_code").get(null);
                } catch (Exception e) {
                    continue;
                }
                if (continuousRequirements.contains(ExchangeDataParser.ExchangeTickerData.class))
                    ExchangeDataParser.getTicker(exchange, "XMR", "BTC", dummy, resources);
                if (continuousRequirements.contains(ExchangeDataParser.ExchangeOrderData.class))
                    ExchangeDataParser.getOrders(exchange, "XMR", "BTC", dummy, resources);
                if (continuousRequirements.contains(ExchangeDataParser.ExchangeMarketData.class))
                    ExchangeDataParser.getMarkets(exchange, "XMR", dummy, resources);
            }
        }


    }


	public void updateFromJSONData(JSONRetriever retriever, Object data) {
		if (data instanceof ExchangeTickerData){
			ExchangeTickerData datac = (ExchangeTickerData)data;
			synchronized(exchangeTickerDataLock){
				exchangeTickerData.put(datac.getExchange(), datac);
			}
		}
		else if (data instanceof CoinMarketCap){
			if (((CoinMarketCap)data).getCoin().equals("monero")){
				synchronized (coinmarketcapXmrDataLock) {
					coinmarketcapXmrData = (CoinMarketCap)data;
				}
			}
			else if (((CoinMarketCap)data).getCoin().equals("bitcoin")){
				synchronized (coinmarketcapBtcDataLock) {
					coinmarketcapBtcData = (CoinMarketCap)data;
				}
			}
		}
//		else if (data instanceof BitcoinAverage){
//			synchronized (bitcoinAverageDataLock) {
//				bitcoinAverageData = (BitcoinAverage)data;
//			}
//		}
		else if (data instanceof FixerIO){
			synchronized (fixerIODataLock) {
				fixerIOData = (FixerIO)data;
			}
		}
		else if (data instanceof BlockExplorerDataParser.BlockExplorerData){
			BlockExplorerDataParser.BlockExplorerData datac = (BlockExplorerDataParser.BlockExplorerData)data;
			synchronized(blockExplorerDataLock){
				blockExplorerData.put(datac.getExplorer(), datac);
			}
		}
		else if (data instanceof ExchangeOrderData){
			ExchangeOrderData datac = (ExchangeOrderData)data;
			synchronized(exchangeOrderDataLock){
				exchangeOrderData.put(datac.getExchange(), datac);
			}
		}
		else if (data instanceof List && !((List<?>)data).isEmpty()){
			@SuppressWarnings("unchecked")
			List<ExchangeTickerData> datac = (List<ExchangeTickerData>)data;
			String exchange = datac.get(0).getExchange();
			Map<String,ExchangeTickerData> markets = new HashMap<String,ExchangeTickerData>();
			for (ExchangeTickerData datad: datac)
				markets.put(datad.getCoin(), datad);
			synchronized(exchangeMarketDataLock){
				exchangeMarketData.put(exchange, markets);
			}
		}
		
		boolean continuousCopy = false;
		synchronized(continuousLock){
			continuousCopy = continuous;
		}

//        if (data != null)
//            Log.d("DBG: DataContainer", data.getClass().toString());
//        else
//            Log.d("DBG: DataContainer", "null");

        if (!continuousCopy || !JSONRetriever.isFetching()){
            Set<Refreshable> refreshablesCopy;
            synchronized(refreshLock) {
                refreshablesCopy = new HashSet<Refreshable>(refreshables);
            }
            for (Refreshable r:refreshablesCopy){
                try{
                    r.refresh(data);
                }
                catch (Exception e){
                    Log.e("DataContainer", "Exception during r.refresh():", e);
                }
            }
        }
		
		if (continuousCopy && !JSONRetriever.isFetching() && retriever != null){
			fetchRequiredData(retriever.resources);
		}
	}
	
	public static boolean isFetching(){
		return JSONRetriever.isFetching();
	}
	
	public static void setContinuous(boolean cont){
		synchronized(continuousLock){
			continuous = cont;
		}
	}
	
	public static boolean isContinuous(){
		synchronized(continuousLock){
			return continuous;
		}
	}
	
	public static interface Refreshable{
        public void refresh(Object lastData);
        public Set<Class<?>> continuousRequirements();
	}
	
	public static CoinMarketCap getCoinmarketcapXmrData(){
		synchronized(coinmarketcapXmrDataLock){
			return coinmarketcapXmrData;
		}
	}
	
	public static CoinMarketCap getCoinmarketcapBtcData(){
		synchronized(coinmarketcapBtcDataLock){
			return coinmarketcapBtcData;
		}
	}

    public static Double getBTCPriceCurrency(String currency){
        Double ret;
        synchronized(coinmarketcapBtcDataLock){
            if (coinmarketcapBtcData == null)
                return null;
            ret = Double.valueOf(coinmarketcapBtcData.getPriceUsd());
        }
        synchronized(fixerIODataLock){
            if (fixerIOData == null)
                return null;
            return ret * fixerIOData.convertUSDtoCurrency(currency);
        }
    }

    public static Float getXMRVolumeCurrency(String currency){
        Float ret;
        synchronized(coinmarketcapXmrDataLock){
            if (coinmarketcapXmrData == null)
                return null;
            ret = coinmarketcapXmrData.getVolumeUsd();
        }
        synchronized(fixerIODataLock){
            if (fixerIOData == null)
                return null;
            return Double.valueOf(ret * fixerIOData.convertUSDtoCurrency(currency)).floatValue();
        }
    }

//	public static BitcoinAverage getBitcoinAverageData(){
//		synchronized(bitcoinAverageDataLock){
//			return bitcoinAverageData;
//		}
//	}
	public static FixerIO getFixerIOData(){
		synchronized(fixerIODataLock){
			return fixerIOData;
		}
	}
	
	public static ExchangeTickerData getExchangeTickerData(String exchange){
		synchronized(exchangeTickerDataLock){
			return exchangeTickerData.get(exchange);
		}
	}
	
	public static int getExchangeTickerDataSize(){
		synchronized(exchangeTickerDataLock){
			return exchangeTickerData.size();
		}
	}
	
	public static ExchangeTickerData getExchangeMarketData(String exchange, String coin){
		synchronized(exchangeMarketDataLock){
			return exchangeMarketData.get(exchange).get(coin);
		}
	}
	
	public static Set<String> getExchangesWithMarket(){
		synchronized(exchangeMarketDataLock){
			return exchangeMarketData.keySet();
		}
	}
	
	public static Set<String> getExchangeMarketCoins(String exchange){
		synchronized(exchangeMarketDataLock){
			return exchangeMarketData.get(exchange).keySet();
		}
	}

    public static Set<String> getExchangesWithOrders(){
        synchronized(exchangeOrderDataLock){
            return exchangeOrderData.keySet();
        }
    }
	
	public static ExchangeOrderData getExchangeOrderData(String exchange){
		synchronized(exchangeOrderDataLock){
			return exchangeOrderData.get(exchange);
		}
	}
	
	public static int getExchangeOrderDataSize(){
		synchronized(exchangeOrderDataLock){
			return exchangeOrderData.size();
		}
	}

	public static BlockExplorerDataParser.BlockExplorerData getBlockExplorerData(String explorer){
		synchronized(blockExplorerDataLock){
			return blockExplorerData.get(explorer);
		}
	}

	public static BlockExplorerDataParser.BlockExplorerData getBlockExplorerData(){
		int maxHeight = 0;
		BlockExplorerDataParser.BlockExplorerData ret = null;
		synchronized(blockExplorerDataLock){
			for (String explorer: blockExplorerData.keySet())
				if (blockExplorerData.get(explorer) != null && blockExplorerData.get(explorer).getHeight() > maxHeight){
					ret = blockExplorerData.get(explorer);
					maxHeight = ret.getHeight();
				}
		}
		return ret;
	}

}
