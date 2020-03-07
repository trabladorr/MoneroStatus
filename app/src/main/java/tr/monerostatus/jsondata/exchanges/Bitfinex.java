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


public class Bitfinex {
	public final static String exchange_code = "bitfinex";

	public static class BittrexOrderParser implements JSONParser {
		private final static String api_request_url = "https://api.bitfinex.com/v1/book/***^^^?limit_bids=1000&limit_asks=1000";
	    private final static String parse_buy = "bids";
	    private final static String parse_sell = "asks";
	    private final static String parse_quantity = "amount";
	    private final static String parse_rate = "price";
	    
	    public final String coin;
		private final String currency;
	    
	    private BittrexOrderParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url.replace("***", coin.toUpperCase()).replace("^^^", currency.toUpperCase()), new BittrexOrderParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			
			try {
				JSONObject obj = new JSONObject(data);
				
			    List<GraphPoint> buyList = new ArrayList<GraphPoint>();
			    List<GraphPoint> sellList =  new ArrayList<GraphPoint>();

				JSONArray arr = obj.getJSONArray(parse_buy);
				for (int i=arr.length()-1; i>=0; i--){
					JSONObject order = arr.getJSONObject(i);
					buyList.add(new GraphPoint(order.getDouble(parse_rate), order.getDouble(parse_rate)*order.getDouble(parse_quantity)));
				}
				
				arr = obj.getJSONArray(parse_sell);
				for (int i=0; i<arr.length(); i++){
					JSONObject order = arr.getJSONObject(i);
					sellList.add(new GraphPoint(order.getDouble(parse_rate), order.getDouble(parse_rate)*order.getDouble(parse_quantity)));
				}


					
				return new BitfinexOrders(coin, currency, buyList, sellList, retriever.resources);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class BitfinexOrders extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;
			
			public BitfinexOrders(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
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
	
	public static class BitfinexTickerParser implements JSONParser {
	    private final static String api_request_url = "https://api.bitfinex.com/v1/pubticker/***^^^";

	    private final static String parse_last_price = "last_price";
		private final static String parse_volume = "volume";
		private final static String parse_low = "low";
		private final static String parse_high = "high";
	    
	    public final String coin;
	    public final String currency;
	    
	    private BitfinexTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url.replace("***", coin.toUpperCase()).replace("^^^", currency.toUpperCase()), new BitfinexTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
                JSONObject obj = new JSONObject(data);

                Float price = Double.valueOf(obj.getDouble(parse_last_price)).floatValue();
                Float avg = (Double.valueOf(obj.getDouble(parse_low)).floatValue() + Double.valueOf(obj.getDouble(parse_high)).floatValue())/2.0f;
                Float change = price / avg - 1;
                Float volume = Double.valueOf(obj.getDouble(parse_volume)).floatValue() / price;

                return new BitfinexTicker(coin, currency, price, change, volume);
            }
            catch (Exception e) {
                Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
            }
            return null;
		}
		
		public static class BitfinexTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;
		    
		    public BitfinexTicker(String coin, String currency, Float price, Float change, Float volume){
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
