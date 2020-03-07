package tr.monerostatus.jsondata.exchanges;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeTickerData;
import tr.monerostatus.jsondata.JSONRetriever;
import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

public class ShapeShift {
	public final static String exchange_code = "shapeshift";

	public static class ShapeShiftTickerParser implements JSONParser {
        private final static String api_request_url = "https://shapeshift.io/rate/***";
        private final static String parse_last_price = "rate";
        private final static String parse_error = "error";

	    public final String coin;
	    public final String currency;
	    
	    private ShapeShiftTickerParser(String coin, String currency){
	    	this.coin = coin;
	    	this.currency = currency;
	    }
	    
	    public static void get(String coin, String currency, JSONUpdatable updatable, Resources resources){
            new JSONRetriever(api_request_url.replace("***", coin.toLowerCase()+"_"+currency.toLowerCase()), new ShapeShiftTickerParser(coin, currency), updatable, resources).execute();
	    }

		@Override
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject obj = new JSONObject(data);

                if (obj.has(parse_error))
                    return null;

				Float price = Float.parseFloat(obj.getString(parse_last_price));
				
				return new ShapeShiftTicker(coin, currency, price);
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public static class ShapeShiftTicker implements ExchangeTickerData{
		    public final String coin;
		    public final String currency;
		    public final Float price;
		    
		    public ShapeShiftTicker(String coin, String currency, Float price){
		    	this.coin = coin;
		    	this.currency = currency;
		    	this.price = price;
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
				return 0f;
			}

			public Float getVolume() {
				return 0f;
			}
		}
	}

	public static class ShapeShiftMarketParser implements JSONParser, JSONUpdatable{
	    private final static String coins_request_url = "https://shapeshift.io/getcoins";
        private final static String parse_status = "status";
        private final static String parse_status_available = "available";

        private volatile int marketsExpected = 0;
        private List<ExchangeTickerData> markets = new ArrayList<ExchangeTickerData>();
        private final static long MARKETTIMEOUT = 10000;
        private static Resources res;
	    public final String currency;
	    
	    private ShapeShiftMarketParser(String currency){
	    	this.currency = currency;
	    }
	    
	    public static void get(String currency, JSONUpdatable updatable, Resources resources){
            res = resources;
	    	new JSONRetriever(coins_request_url, new ShapeShiftMarketParser(currency), updatable, resources).execute();
	    }

		@Override
		public List<ExchangeTickerData> parseJSONData(JSONRetriever retriever, JSONTokener data) {
			try {
				JSONObject dataObj = new JSONObject(data);
				Iterator<?> i = dataObj.keys();
				while (i.hasNext()){
					String key = (String)i.next();
					if (key.equalsIgnoreCase(currency))
                        continue;

					JSONObject obj = dataObj.getJSONObject(key);
                    if (obj.getString(parse_status).equalsIgnoreCase(parse_status_available)) {
                        ShapeShiftTickerParser.get(key, currency, this, res);
                        marketsExpected ++;
                    }
                }
			} 
			catch (Exception e) {
		        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
			}

            long timeout = System.currentTimeMillis();
            while(timeout < System.currentTimeMillis() + MARKETTIMEOUT){
                if (marketsExpected == 0)
                    return markets;
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                }
            }

			return null;
		}

        public void updateFromJSONData(JSONRetriever retriever, Object data) {
            if (data != null) {
                markets.add((ShapeShiftTickerParser.ShapeShiftTicker) data);
            }
            marketsExpected --;
        }
	}
}
