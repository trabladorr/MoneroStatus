package tr.monerostatus.jsondata;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.monerostatus.app.GraphHelper.GraphPoint;
import tr.monerostatus.jsondata.JSONRetriever.JSONUpdatable;
import tr.monerostatus.jsondata.exchanges.Bitfinex;
import tr.monerostatus.jsondata.exchanges.Bittrex;
import tr.monerostatus.jsondata.exchanges.Bter;
import tr.monerostatus.jsondata.exchanges.Cryptopia;
import tr.monerostatus.jsondata.exchanges.Hitbtc;
import tr.monerostatus.jsondata.exchanges.Poloniex;
import tr.monerostatus.jsondata.exchanges.ShapeShift;

public class ExchangeDataParser {

	public static final Class<?> exchangeClasses[] = {Poloniex.class, Bittrex.class, Bter.class, /*MintPal.class,*/ Hitbtc.class, /*Melotic.class,*/ ShapeShift.class, /*Cryptsy.class,*/ Cryptopia.class, Bitfinex.class};
    private static final Map<String, Class<?>>  exchangeClassMapTmp = new HashMap<String, Class<?>>();
	private static final Map<String, Class<?>> tickerParsers = new HashMap<String, Class<?>>();
	private static final Map<String, Class<?>> orderParsers = new HashMap<String, Class<?>>();
    private static final Map<String, Class<?>>  marketParsers = new HashMap<String, Class<?>>();
    private static final List<String> exchangeCodesTmp = new ArrayList<String>();

	static{
		loadExchanges();
	}

    public static final Map<String, Class<?>> exchangeClassMap = Collections.unmodifiableMap(exchangeClassMapTmp);
//
//    static {
//        for (String e:exchangeClassMapTmp.keySet())
//            Log.d("DBG: ExchangeDataParser",e+":"+exchangeClassMapTmp.get(e).toString());
//    }

    private static void loadExchanges(){
        for (Class<?> c: exchangeClasses){
            String exchange = null;
            try {
                exchange = (String)c.getField("exchange_code").get(null);
            }
            catch (Exception e) {
                continue;
            }
            exchangeClassMapTmp.put(exchange,c);
            for (Class<?> s: c.getClasses()){
                if (s.getSimpleName().endsWith("TickerParser"))
                    tickerParsers.put(exchange, s);
                else if (s.getSimpleName().endsWith("OrderParser"))
                    orderParsers.put(exchange, s);
                else if (s.getSimpleName().endsWith("MarketParser"))
                    marketParsers.put(exchange, s);
            }
            exchangeCodesTmp.add(exchange);

        }

    }

    public static final String exchangeCodes[] = exchangeCodesTmp.toArray(new String[exchangeCodesTmp.size()]);
    
    public static void getTicker(String exchange, String coin, String currency, JSONUpdatable updatable, Resources resources){
    	try {
            if (tickerParsers.containsKey(exchange))
			    tickerParsers.get(exchange).getMethod("get", String.class, String.class, JSONUpdatable.class, Resources.class).invoke(null, coin, currency, updatable, resources);
		}
    	catch (Exception e) {
    	}
    }
    
    public static void getOrders(String exchange, String coin, String currency, JSONUpdatable updatable, Resources resources){
    	try {
            if (orderParsers.containsKey(exchange))
                orderParsers.get(exchange).getMethod("get", String.class, String.class, JSONUpdatable.class, Resources.class).invoke(null, coin, currency, updatable, resources);
		}
        catch (Exception e) {
        }
    }
    
    public static void getMarkets(String exchange, String currency, JSONUpdatable updatable, Resources resources){
    	try {
            if (marketParsers.containsKey(exchange))
                marketParsers.get(exchange).getMethod("get", String.class, JSONUpdatable.class, Resources.class).invoke(null, currency, updatable, resources);
		}
        catch (Exception e) {
        }
    }

    public static String[] getExchangesWithOrders(){
        List<String> ret = new ArrayList<String>();
        for (String exchange : exchangeCodes){
            if (orderParsers.keySet().contains(exchange))
                ret.add(exchange);
        }
        return ret.toArray(new String[ret.size()]);
    }

    public static String[] getExchangesWithMarkets(){
    	List<String> ret = new ArrayList<String>();
		for (String exchange : exchangeCodes){
            if (marketParsers.keySet().contains(exchange))
                ret.add(exchange);
		}
		return ret.toArray(new String[ret.size()]);
    }
    
    public interface ExchangeTickerData{

	    String getExchange();
	    String getCoin();
	    String getCurrency();
	    Float getPrice();
	    Float getChange();
	    Float getVolume();
    }
    
    public static abstract class ExchangeOrderData{
	    public abstract String getExchange();
	    public abstract String getCoin();
	    public abstract String getCurrency();
	    public abstract List<GraphPoint> getSellData();
	    public abstract List<GraphPoint> getBuyData();
    }

    public static abstract class ExchangeMarketData{}
}
