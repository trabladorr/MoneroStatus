package tr.monerostatus.jsondata.exchanges;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

import tr.monerostatus.app.GraphHelper.GraphPoint;
import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeOrderData;
import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeTickerData;
import tr.monerostatus.jsondata.JSONRetriever;
import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;


public class Bter {
	public final static String exchange_code = "bter";
	
	public static class BterOrderParser implements JSONParser {
		private final static String api_request_url = "https://data.bter.com/api/1/depth/";
	    private final static String parse_asks = "asks";
	    private final static String parse_bids = "bids";
	    
	    public final String coin;
		private final String currency;
	    
	    private BterOrderParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url+coin.toLowerCase()+"_"+currency.toLowerCase(), new BterOrderParser(coin, currency), updatable, resources).execute();
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
				for (int i=arr.length()-1; i>=0; i--){
					JSONArray order = arr.getJSONArray(i);
					sellList.add(new GraphPoint(order.getDouble(0), order.getDouble(0)*order.getDouble(1)));
				}
					
				return new BterOrders(coin, currency, buyList, sellList, retriever.resources);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class BterOrders extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;
			
			public BterOrders(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
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
	
	public static class BterTickerParser implements JSONParser {
	    private final static String api_request_url = "https://data.bter.com/api/1/ticker/";
	    
	    private final static String parse_last_price = "last";
	    private final static String avg = "avg";
	    private final static String parse_volume = "vol_btc";
	    
	    public final String coin;
	    public final String currency;
	    
	    private BterTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url+coin.toLowerCase()+"_"+currency.toLowerCase(), new BterTickerParser(coin, currency), updatable, resources).execute();
	    }
	    
		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				
				JSONObject obj = new JSONObject(data);

				Float price = Double.valueOf(obj.getDouble(parse_last_price)).floatValue();
				Float change = price/Double.valueOf(obj.getDouble(avg)).floatValue() - 1;
				Float volume = Double.valueOf(obj.getDouble(parse_volume)).floatValue();
				
				return new BterTicker(coin, currency, price, change, volume);
			
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class BterTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;
		    
		    public BterTicker(String coin, String currency, Float price, Float change, Float volume){
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

			public Float getChange() {
				return change;
			}
			
			public Float getVolume() {
				return volume;
			}
		}
	}

}
