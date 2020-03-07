package tr.monerostatus.jsondata;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tr.monerostatus.jsondata.JSONRetriever.JSONParser;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;

public class CoinMarketCapParser implements JSONParser {
	private final static String api_request_url = "https://api.coinmarketcap.com/v1/ticker/***/";
    private final static String parse_price_btc = "price_btc";
    private final static String parse_price_usd = "price_usd";
    private final static String parse_volume_usd = "24h_volume_usd";
    private final static String parse_marketcap_usd = "market_cap_usd";
    private final static String parse_change = "percent_change_24h";
    private final static String parse_position = "rank";


    public final String coin;


    private CoinMarketCapParser(String coin){
    	this.coin = coin;
    }
    
    public static void get(String coin, JSONUpdatable updatable, Resources resources){	
    	new JSONRetriever(api_request_url.replace("***", coin.toUpperCase()), new CoinMarketCapParser(coin), updatable, resources).execute();
    }

	@Override
	public Object parseJSONData(JSONRetriever retriever, JSONTokener data) {
		try {

			JSONObject obj = (JSONObject)(((JSONArray) data.nextValue()).get(0));

            Float price_usd = Double.valueOf(obj.getDouble(parse_price_usd)).floatValue();
            Float price_btc = Double.valueOf(obj.getDouble(parse_price_btc)).floatValue();
            Float volume_usd = Double.valueOf(obj.getDouble(parse_volume_usd)).floatValue();
            Float marketcap_usd = Double.valueOf(obj.getDouble(parse_marketcap_usd)).floatValue();
            Float change = Double.valueOf(obj.getDouble(parse_change)).floatValue();
            Integer position = NumberFormat.getNumberInstance(java.util.Locale.US).parse(obj.getString(parse_position)).intValue();

			
			return new CoinMarketCap(coin, price_usd, price_btc, volume_usd, marketcap_usd, change, position);
		} 
		catch (Exception e) {
	        Log.e(this.getClass().getSimpleName(),"Json parse failed: "+e.getLocalizedMessage());
		}
		return null;
	}
	
	public static class CoinMarketCap{
	    private final String coin;
	    private final Float price_usd;
        private final Float price_btc;
        private final Float volume_usd;
        private final Float marketcap_usd;
        private final Float change;
        private final Integer position;
	    
	    public CoinMarketCap(String coin, Float price_usd, Float price_btc, Float volume_usd, Float marketcap_usd, Float change, Integer position){
	    	this.coin = coin;
            this.price_usd = price_usd;
            this.price_btc = price_btc;
            this.volume_usd = volume_usd;
            this.marketcap_usd = marketcap_usd;
            this.change = change;
            this.position = position;
	    }
	    
	    public String getCoin(){
	    	return coin;
	    }

        public Float getPriceUsd(){
            return price_usd;
        }

        public Float getPriceBtc(){
            return price_btc;
        }

        public Float getVolumeUsd(){
            return volume_usd;
        }

        public Float getMarketCapUsd(){
            return marketcap_usd;
        }

        public Float getChange(){
            return change;
        }

        public Integer getPosition(){
            return position;
        }

	}
    
    
}
