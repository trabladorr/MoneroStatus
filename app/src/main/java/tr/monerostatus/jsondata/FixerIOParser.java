package tr.monerostatus.jsondata;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

public class FixerIOParser implements JSONParser {
	private final static String api_request_url = "https://api.fixer.io/latest?base=USD";
	private final static String parse_rates = "rates";

    private FixerIOParser(){
    }
    
    public static void get(JSONUpdatable updatable, Resources resources){	
    	new JSONRetriever(api_request_url, new FixerIOParser(), updatable, resources).execute();
    }

	@Override
	public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
		try {
			JSONObject all_rates = new JSONObject(data).getJSONObject(parse_rates);
			
			Map<String, Double> currencyPrice = new HashMap<String, Double>();

            Iterator<String> iter = all_rates.keys();

            while (iter.hasNext()){
                String next = iter.next();
                currencyPrice.put(next,all_rates.getDouble(next));
            }
            currencyPrice.put("USD",Double.valueOf(1));
			
			return new FixerIO(currencyPrice);
		} 
		catch (Exception e) {
	        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
		}
		return null;
	}
	
	public static class FixerIO{
	    private final Map<String, Double> currencyPrice;
	    
	    public FixerIO(Map<String, Double> currencyPrice){
	    	this.currencyPrice = Collections.unmodifiableMap(currencyPrice);
	    }
	    
	    public Double convertUSDtoCurrency(String currency){
            currency = currency.toUpperCase();
	    	if (currencyPrice.containsKey(currency))
                return currencyPrice.get(currency);
            return Double.valueOf(0);
		}
	}
    
    
}
