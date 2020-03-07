package tr.monerostatus.jsondata;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

public class BitcoinAverageParser implements JSONParser {
	private final static String api_request_url = "https://api.bitcoinaverage.com/ticker/all";
    private final static String parse_avg = "last";
    
    public final static String currency_codes[] = {"aud","brl","cad","chf","cny","eur","gbp","idr","ils","inr","jpy","krw","mxn","myr","ngn","nzd","pln","rub","sek","sgd","try", "usd","zar"};

    private BitcoinAverageParser(){
    }
    
    public static void get(JSONUpdatable updatable, Resources resources){	
    	new JSONRetriever(api_request_url, new BitcoinAverageParser(), updatable, resources).execute();
    }

	@Override
	public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
		try {
			JSONObject obj = new JSONObject(data);
			
			Map<String, Double> currencyPrice = new HashMap<String, Double>();
			
			for (String currency: currency_codes){
				try {
					currencyPrice.put(currency, obj.getJSONObject(currency.toUpperCase()).getDouble(parse_avg));
				}
				catch (Exception e){
				}
			}
			
			return new BitcoinAverage(currencyPrice);			
		} 
		catch (Exception e) {
	        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
		}
		return null;
	}
	
	public static class BitcoinAverage{
	    private final Map<String, Double> currencyPrice;
	    
	    public BitcoinAverage(Map<String, Double> currencyPrice){
	    	this.currencyPrice = Collections.unmodifiableMap(currencyPrice);
	    }
	    
	    public Double getCurrencyPrice(String currency){
	    	return currencyPrice.get(currency);
		}
	}
    
    
}
