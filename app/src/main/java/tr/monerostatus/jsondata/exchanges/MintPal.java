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


public class MintPal {
	public final static String exchange_code = "mintpal";
	
	public static class MintPalOrderParser implements JSONParser, JSONUpdatable {
		private final static String api_request_url = "https://api.mintpal.com/v1/market/orders/";
	    private final static String parse_orders = "orders";
	    private final static String parse_price = "price";
	    private final static String parse_total = "total";
	    private final static String buy = "BUY";
	    private final static String sell = "SELL";
	    private List<GraphPoint> buyList = null;
	    private List<GraphPoint> sellList = null;
	    private int received = 0;
	    
	    public final String coin;
		private final String currency;
	    private final JSONUpdatable updatable;
	    
	    private MintPalOrderParser(String coin, String currency, JSONUpdatable updatable){
	    	this.coin = coin;
	    	this.currency = currency;
	    	this.updatable = updatable;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	MintPalOrderParser parser = new MintPalOrderParser(coin, currency, updatable);
	    	new JSONRetriever(api_request_url+coin.toUpperCase()+"/"+currency.toUpperCase()+"/"+sell, parser, parser, resources).execute();
	    	new JSONRetriever(api_request_url+coin.toUpperCase()+"/"+currency.toUpperCase()+"/"+buy, parser, parser, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			
			try {
				JSONObject obj = new JSONObject(data);
				
				JSONArray arr = obj.getJSONArray(parse_orders);

				if (retriever.url.endsWith(buy)){
					List<GraphPoint> buyList = new ArrayList<GraphPoint>();
				    
					for (int i=arr.length()-1; i>=0; i--){
						JSONObject order = arr.getJSONObject(i);
						buyList.add(new GraphPoint(order.getDouble(parse_price), order.getDouble(parse_total)));
					}
					return buyList;
				}
				else if (retriever.url.endsWith(sell)){
					List<GraphPoint> sellList = new ArrayList<GraphPoint>();
					for (int i=0; i<arr.length(); i++){
						JSONObject order = arr.getJSONObject(i);
						sellList.add(new GraphPoint(order.getDouble(parse_price), order.getDouble(parse_total)));
					}
					return sellList;
				}
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void updateFromJSONData(JSONRetriever retriever, Object data) {
			received ++;
			if (retriever.url.endsWith(buy) && data instanceof List)
				buyList = (List<GraphPoint>) data;
			else if (retriever.url.endsWith(sell) && data instanceof List)
				sellList = (List<GraphPoint>) data;
			
			if (received == 2){
				if (buyList == null || sellList == null)
					updatable.updateFromJSONData(retriever, null);
				else
					updatable.updateFromJSONData(retriever, new MintPalOrders(coin, currency, buyList, sellList, retriever.resources));
			}
		}
		
		public static class MintPalOrders extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;
			
			public MintPalOrders(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
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
	
	public static class MintPalTickerParser implements JSONParser {
	    private final static String api_request_url = "https://api.mintpal.com/v1/market/stats/";
	    private final static String parse_last_price = "last_price";
	    private final static String parse_change = "change";
	    private final static String parse_volume = "24hvol";
	    public final String coin;
	    public final String currency;
	    
	    private MintPalTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	
	    	new JSONRetriever(api_request_url+coin.toUpperCase()+"/"+currency.toUpperCase(), new MintPalTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject obj = new JSONArray(data).getJSONObject(0);
				
				Float price = Float.parseFloat(obj.getString(parse_last_price));
				Float change = Float.parseFloat(obj.getString(parse_change))/100;
				Float volume = Float.parseFloat(obj.getString(parse_volume));
				
				return new MintpalTicker(coin, currency, price, change, volume);			
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class MintpalTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;
		    
		    public MintpalTicker(String coin, String currency, Float price, Float change, Float volume){
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
