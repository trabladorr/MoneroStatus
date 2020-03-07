package tr.monerostatus.jsondata.exchanges;

import java.util.ArrayList;
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
import android.content.res.Resources;
import android.util.Log;


public class Hitbtc {
	public final static String exchange_code = "hitbtc";

	public static class HitbtcOrderParser implements JSONParser {
		private final static String api_request_url = "https://api.hitbtc.com/api/1/public/***/orderbook?format_price=number&format_amount=number";
	    private final static String parse_asks = "asks";
	    private final static String parse_bids = "bids";
	    
	    public final String coin;
		private final String currency;
	    
	    private HitbtcOrderParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }

	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url.replace("***", coin.toUpperCase()+currency.toUpperCase()), new HitbtcOrderParser(coin, currency), updatable, resources).execute();
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
				return new HitbtcOrder(coin, currency, buyList, sellList, retriever.resources);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class HitbtcOrder extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;
			
			public HitbtcOrder(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
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
	
	public static class HitbtcTickerParser implements JSONParser {
	    private final static String api_request_url = "https://api.hitbtc.com/api/1/public/***/ticker";
	    private final static String parse_last_price = "last";
	    private final static String parse_volume = "volume";
	    public final String coin;
	    public final String currency;
	    
	    private HitbtcTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url.replace("***", coin.toUpperCase()+currency.toUpperCase()), new HitbtcTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject obj = new JSONObject(data);
				
				Float price = Float.parseFloat(obj.getString(parse_last_price));
				Float change = Float.valueOf(0);
				Float volume = Float.parseFloat(obj.getString(parse_volume))*price;
				
				return new HitbtcTicker(coin, currency, price, change, volume);			
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class HitbtcTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;
		    
		    public HitbtcTicker(String coin, String currency, Float price, Float change, Float volume){
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
}
