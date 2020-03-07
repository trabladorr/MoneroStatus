package tr.monerostatus.jsondata.exchanges;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeOrderData;
import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeTickerData;
import tr.monerostatus.app.GraphHelper.GraphPoint;
import tr.monerostatus.jsondata.JSONRetriever;
import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;
import tr.monerostatus.jsondata.exchanges.Poloniex.PoloniexTickerParser.PoloniexTicker;
import android.content.res.Resources;
import android.util.Log;


public class Poloniex {
	public final static String exchange_code = "poloniex";

	public static class PoloniexOrderParser implements JSONParser {
		private final static String api_request_url = "https://poloniex.com/public?command=returnOrderBook&currencyPair=***&depth=500";
	    private final static String parse_asks = "asks";
	    private final static String parse_bids = "bids";
	    
	    public final String coin;
		private final String currency;
	    
	    private PoloniexOrderParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }

	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url.replace("***", currency.toUpperCase()+"_"+coin.toUpperCase()), new PoloniexOrderParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			
			try {
				JSONObject obj = new JSONObject(data);
				
			    List<GraphPoint> buyList = new ArrayList<GraphPoint>();
			    List<GraphPoint> sellList =  new ArrayList<GraphPoint>();

				JSONArray arr = obj.getJSONArray(parse_bids);
				
				for (int i=arr.length()-1; i>=0; i--){
					JSONArray order = arr.getJSONArray(i);
					buyList.add(new GraphPoint(order.getDouble(0), order.getDouble(0)*order.getDouble(1)));
				}
				arr = obj.getJSONArray(parse_asks);
				
				for (int i=0; i<arr.length(); i++){
					JSONArray order = arr.getJSONArray(i);
					sellList.add(new GraphPoint(order.getDouble(0), order.getDouble(0)*order.getDouble(1)));
				}
				return new PoloniexOrders(coin, currency, buyList, sellList, retriever.resources);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class PoloniexOrders extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;
			
			public PoloniexOrders(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
		    	this.coin = coin;
		    	this.currency = currency;
				this.buy = buy;
				this.sell = sell;
			}

			public String getExchange() {
				return exchange_code;
			}
			
			public String getCoin() {
				return coin;
			}

			public String getCurrency() {
				return currency;
			}

			public List<GraphPoint> getSellData() {
				return sell;
			}

			public List<GraphPoint> getBuyData() {
				return buy;
			}
		}
	}
	
	public static class PoloniexTickerParser implements JSONParser {
	    private final static String api_request_url = "https://poloniex.com/public?command=returnTicker";
	    private final static String parse_last_price = "last";
	    private final static String parse_change = "percentChange";
	    private final static String parse_volume = "baseVolume";
	    public final String coin;
	    public final String currency;
	    
	    private PoloniexTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url, new PoloniexTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject obj = new JSONObject(data).getJSONObject(currency.toUpperCase()+"_"+coin.toUpperCase());
				
				Float price = Float.parseFloat(obj.getString(parse_last_price));
				Float change = Float.parseFloat(obj.getString(parse_change));
				Float volume = Float.parseFloat(obj.getString(parse_volume));
				
				return new PoloniexTicker(coin, currency, price, change, volume);			
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class PoloniexTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;
		    
		    public PoloniexTicker(String coin, String currency, Float price, Float change, Float volume){
		    	this.coin = coin;
		    	this.currency = currency;
		    	this.price = price;
		    	this.change = change;
		    	this.volume = volume;
		    }
		    
		    public String getExchange(){
		    	return exchange_code;
		    }

			public String getCoin() {
				return coin;
			}

			public String getCurrency() {
				return currency;
			}
			
			public Float getPrice() {
				return price;
			}

			@Override
			public Float getChange() {
				return change;
			}

			@Override
			public Float getVolume() {
				return volume;
			}
		}
	}
	
	public static class PoloniexMarketParser implements JSONParser {
	    private final static String api_request_url = "https://poloniex.com/public?command=returnTicker";
	    private final static String parse_last_price = "last";
	    private final static String parse_change = "percentChange";
	    private final static String parse_volume = "baseVolume";
	    public final String currency;
	    
	    private PoloniexMarketParser(String currency){
	    	this.currency = currency;
	    }
	    
	    public static void get(String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url, new PoloniexMarketParser(currency), updatable, resources).execute();
	    }

		@Override
		public List<ExchangeTickerData> parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject dataObj = new JSONObject(data);
				Iterator<?> i = dataObj.keys();
				List<ExchangeTickerData> markets = new ArrayList<ExchangeTickerData>();
				while (i.hasNext()){
					String key = (String)i.next();
					if (key.startsWith(currency)){
						JSONObject obj = dataObj.getJSONObject(key);

						Float price = Float.parseFloat(obj.getString(parse_last_price));
						Float change = Float.parseFloat(obj.getString(parse_change));
						Float volume = Float.parseFloat(obj.getString(parse_volume));
						
						markets.add(new PoloniexTicker(key.substring(currency.length()+1), currency, price, change, volume));
					}
				}
				return markets;			
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
	}
}
