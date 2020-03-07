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


public class Cryptopia {
	public final static String exchange_code = "cryptopia";

	public static class CryptopiaOrderParser implements JSONParser {
		private final static String api_request_url = "https://www.cryptopia.co.nz/api/GetMarketOrders/2999/1000";

		private final static String parse_data = "Data";
	    private final static String parse_buy = "Buy";
	    private final static String parse_sell = "Sell";
	    private final static String parse_price = "Price";
	    private final static String parse_total = "Total";
	    
	    public final String coin;
		private final String currency;
	    
	    private CryptopiaOrderParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url.replace("***", currency).replace("^^^", coin), new CryptopiaOrderParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			
			try {
				JSONObject obj = new JSONObject(data).getJSONObject(parse_data);
				
			    List<GraphPoint> buyList = new ArrayList<GraphPoint>();
			    List<GraphPoint> sellList =  new ArrayList<GraphPoint>();

				JSONArray arr = obj.getJSONArray(parse_buy);
				for (int i=arr.length()-1; i>=0; i--){
					JSONObject order = arr.getJSONObject(i);
					buyList.add(new GraphPoint(order.getDouble(parse_price), order.getDouble(parse_total)));
				}
				
				arr = obj.getJSONArray(parse_sell);
				for (int i=0; i<arr.length(); i++){
					JSONObject order = arr.getJSONObject(i);
					sellList.add(new GraphPoint(order.getDouble(parse_price), order.getDouble(parse_total)));
				}


					
				return new CryptopiaOrders(coin, currency, buyList, sellList, retriever.resources);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class CryptopiaOrders extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;
			
			public CryptopiaOrders(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
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
	
	public static class CryptopiaTickerParser implements JSONParser {
	    private final static String api_request_url = "https://www.cryptopia.co.nz/api/GetMarket/2999";

	    private final static String parse_data = "Data";
	    private final static String parse_last_price = "LastPrice";
	    private final static String parse_change = "Change";
	    private final static String parse_volume = "Volume";
	    
	    public final String coin;
	    public final String currency;
	    
	    private CryptopiaTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url, new CryptopiaTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject obj = new JSONObject(data).getJSONObject(parse_data);

				Float price = Double.valueOf(obj.getDouble(parse_last_price)).floatValue();
				Float change = Double.valueOf(obj.getDouble(parse_change)).floatValue();
				Float volume = Double.valueOf(obj.getDouble(parse_volume)).floatValue();

				return new CryptopiaTicker(coin, currency, price, change, volume);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class CryptopiaTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;
		    
		    public CryptopiaTicker(String coin, String currency, Float price, Float change, Float volume){
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
