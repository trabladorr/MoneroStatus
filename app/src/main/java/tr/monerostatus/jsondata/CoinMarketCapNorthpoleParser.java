package tr.monerostatus.jsondata;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONTokener;

import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;
import android.content.res.Resources;
import android.util.Log;

public class CoinMarketCapNorthpoleParser implements JSONParser {
	private final static String api_request_url = "http://coinmarketcap.northpole.ro/api/v6/***.json";
    private final static String parse_price = "price";
    private final static String parse_volume = "volume24";
    private final static String parse_marketcap = "marketCap";
    private final static String parse_change = "change24h";
    private final static String parse_position = "position";
    private final static String parse_btc = "btc";

    
    public final String coin;
    
    
    private CoinMarketCapNorthpoleParser(String coin){
    	this.coin = coin;
    }
    
    public static void get(String coin, JSONUpdatable updatable, Resources resources){	
    	new JSONRetriever(api_request_url.replace("***", coin.toUpperCase()), new CoinMarketCapNorthpoleParser(coin), updatable, resources).execute();
    }

	@Override
	public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
		try {
			JSONObject obj = new JSONObject(data);


            Float change = Double.valueOf(obj.getDouble(parse_change)).floatValue();
            Integer position = NumberFormat.getNumberInstance(java.util.Locale.US).parse(obj.getString(parse_position)).intValue();

			Map<String, Integer> marketCap = new HashMap<String, Integer>();
			Map<String, Float> currencyPrice = new HashMap<String, Float>();
			Map<String, Float> volume = new HashMap<String, Float>();

            for (Iterator<String> currencyIter = obj.getJSONObject(parse_price).keys(); currencyIter.hasNext();){
                String currency = currencyIter.next();
//				marketCap.put(currency, NumberFormat.getNumberInstance(java.util.Locale.US).parse(obj.getJSONObject(parse_marketcap).getString(currency)).intValue());
                marketCap.put(currency, Double.valueOf(obj.getJSONObject(parse_marketcap).getDouble(currency)).intValue());
                currencyPrice.put(currency, Double.valueOf(obj.getJSONObject(parse_price).getDouble(currency)).floatValue());
                volume.put(currency, Double.valueOf(obj.getJSONObject(parse_volume).getDouble(currency)).floatValue());
			}

			
			return new CoinMarketCap(coin, change, position, marketCap, currencyPrice, volume);
		} 
		catch (Exception e) {
	        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
		}
		return null;
	}
	
	public static class CoinMarketCap{
	    private final String coin;
        private final Float change;
        private final Integer position;
	    private final Map<String, Integer> marketCap;
        private final Map<String, Float> currencyPrice;
        private final Map<String, Float> volume;
	    
	    public CoinMarketCap(String coin, Float change, Integer position, Map<String, Integer> marketCap, Map<String, Float> currencyPrice, Map<String, Float> volume){
	    	this.coin = coin;
            this.change = change;
            this.position = position;
	    	this.marketCap = Collections.unmodifiableMap(marketCap);
	    	this.currencyPrice = Collections.unmodifiableMap(currencyPrice);
	    	this.volume = Collections.unmodifiableMap(volume);
	    }
	    
	    public String getCoin(){
	    	return coin;
	    }

        public Float getChange(){
            return change;
        }

        public Integer getPosition(){
            return position;
        }

        public Set<String> getCurrencies(){
            return currencyPrice.keySet();
        }

	    public Integer getMarketCap(String currency){
	    	return marketCap.get(currency);	
	    }
	    
	    public Float getCurrencyPrice(String currency){
	    	return currencyPrice.get(currency);	
		}
	    
	    public Float getVolume(String currency){
            return volume.get(currency);
        }
	}
    
    
}
