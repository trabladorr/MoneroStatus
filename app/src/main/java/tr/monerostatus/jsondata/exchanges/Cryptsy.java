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


public class Cryptsy {
	public final static String exchange_code = "cryptsy";

	public static class CryptsyOrderParser implements JSONParser {
		private final static String api_request_url = "http://pubapi.cryptsy.com/api.php?method=singleorderdata&marketid=505";
		private final static String parse_return = "return";
	    private final static String parse_buy = "buyorders";
	    private final static String parse_sell = "sellorders";
	    private final static String parse_total = "total";
	    private final static String parse_price = "price";
	    
	    public final String coin;
		private final String currency;
	    
	    private CryptsyOrderParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }

	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url, new CryptsyOrderParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {

			try {
				JSONObject obj = new JSONObject(data).getJSONObject(parse_return).getJSONObject(coin.toUpperCase());

			    List<GraphPoint> buyList = new ArrayList<GraphPoint>();
			    List<GraphPoint> sellList =  new ArrayList<GraphPoint>();

				JSONArray arr = obj.getJSONArray(parse_buy);
				for (int i=arr.length()-1; i>=0; i--){
					JSONObject order = arr.getJSONObject(i);
					buyList.add(new GraphPoint(Double.valueOf(order.getString(parse_price)), Double.valueOf(order.getString(parse_total))));
				}

				arr = obj.getJSONArray(parse_sell);
				for (int i=0; i<arr.length(); i++){
					JSONObject order = arr.getJSONObject(i);
					sellList.add(new GraphPoint(Double.valueOf(order.getString(parse_price)), Double.valueOf(order.getString(parse_total))));
				}



				return new CryptsyOrders(coin, currency, buyList, sellList, retriever.resources);
			}
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}

		public static class CryptsyOrders extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;

			public CryptsyOrders(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
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

	public static class CryptsyTickerParser implements JSONParser {
	    private final static String api_request_url = "http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid=505";
		private final static String parse_return = "return";
		private final static String parse_markets = "markets";
		private final static String parse_last_price = "lasttradeprice";
		private final static String parse_volume = "volume";
		private final static String parse_recent = "recenttrades";
		private final static String parse_price = "price";

	    public final String coin;
	    public final String currency;

	    private CryptsyTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }

	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url, new CryptsyTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject obj = new JSONObject(data).getJSONObject(parse_return).getJSONObject(parse_markets).getJSONObject(coin.toUpperCase());
				
				Float price = Double.valueOf(obj.getString(parse_last_price)).floatValue();
				Float volume = Double.valueOf(obj.getString(parse_volume)).floatValue()*price;
				Float earliestPrice = Double.valueOf(((JSONObject)obj.getJSONArray(parse_recent).get(obj.getJSONArray(parse_recent).length()-1)).getString(parse_price)).floatValue();
				Float change = price/earliestPrice - 1;

					return new CryptsyTicker(coin, currency, price, change, volume);
			}
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}

		public static class CryptsyTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;

		    public CryptsyTicker(String coin, String currency, Float price, Float change, Float volume){
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
