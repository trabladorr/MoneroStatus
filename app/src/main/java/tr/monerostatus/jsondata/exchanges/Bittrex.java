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


public class Bittrex {
	public final static String exchange_code = "bittrex";

	public static class BittrexOrderParser implements JSONParser {
		private final static String api_request_url = "https://bittrex.com/api/v1.1/public/getorderbook?market=***-^^^&type=both&depth=100";
	    private final static String parse_result = "result";
	    private final static String parse_buy = "buy";
	    private final static String parse_sell = "sell";
	    private final static String parse_quantity = "Quantity";
	    private final static String parse_rate = "Rate";
	    
	    public final String coin;
		private final String currency;
	    
	    private BittrexOrderParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url.replace("***", currency).replace("^^^", coin), new BittrexOrderParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			
			try {
				JSONObject obj = new JSONObject(data).getJSONObject(parse_result);
				
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


					
				return new BittrexOrders(coin, currency, buyList, sellList, retriever.resources);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class BittrexOrders extends ExchangeOrderData{
			private final String coin;
			private final String currency;
			private final List<GraphPoint> buy;
			private final List<GraphPoint> sell;
			
			public BittrexOrders(String coin, String currency, List<GraphPoint> buy, List<GraphPoint> sell, Resources res){
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
	
	public static class BittrexTickerParser implements JSONParser {
	    private final static String api_request_url = "https://bittrex.com/api/v1.1/public/getmarketsummaries";
	    
	    private final static String parse_name = "MarketName";
	    private final static String parse_result = "result";
	    private final static String parse_last_price = "Last";
	    private final static String parse_prev_day = "PrevDay";
	    private final static String parse_volume = "BaseVolume";
	    
	    public final String coin;
	    public final String currency;
	    
	    private BittrexTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
	    	new JSONRetriever(api_request_url, new BittrexTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONArray arr = new JSONObject(data).getJSONArray(parse_result);
				
				for (int i=0;i<arr.length();++i){
					JSONObject obj = arr.getJSONObject(i);
					
					if (!obj.getString(parse_name).equals(currency.toUpperCase()+"-"+coin.toUpperCase()))
							continue;
					Float price = Double.valueOf(obj.getDouble(parse_last_price)).floatValue();
					Float change = price/Double.valueOf(obj.getDouble(parse_prev_day)).floatValue() - 1;
					Float volume = Double.valueOf(obj.getDouble(parse_volume)).floatValue();
					
					return new BittrexTicker(coin, currency, price, change, volume);
				}
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class BittrexTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    public final Float change;
		    public final Float volume;
		    
		    public BittrexTicker(String coin, String currency, Float price, Float change, Float volume){
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
