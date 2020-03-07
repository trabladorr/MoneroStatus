package tr.monerostatus.app;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tr.monerostatus.DataContainer;
import tr.monerostatus.DataContainer.Refreshable;
import tr.monerostatus.R;
import tr.monerostatus.jsondata.CoinMarketCapNorthpoleParser;
import tr.monerostatus.jsondata.CoinMarketCapParser;
import tr.monerostatus.jsondata.ExchangeDataParser;
import tr.monerostatus.jsondata.ExchangeDataParser.ExchangeTickerData;
import tr.monerostatus.jsondata.FixerIOParser;

public class MarketFragment extends Fragment implements Refreshable {
	public static String FRAGMENT_TAG = "market_fragment";

	private String chosenExchange = null;
	private String chosenCoin = null;
	private static Set<Class<?>> baseRequirements = new HashSet<Class<?>>();
	static{
		baseRequirements.add(ExchangeDataParser.ExchangeMarketData.class);
		baseRequirements.add(CoinMarketCapParser.class);
//		baseRequirements.add(BitcoinAverageParser.class);
		baseRequirements.add(FixerIOParser.class);
		baseRequirements.addAll(Arrays.asList(ExchangeDataParser.exchangeClasses));
	}
	
	private Set<String> exchanges = new HashSet<String>();
	private Set<String> coins = new HashSet<String>();
	
	private Spinner exchangeSpinner;
	private Spinner coinSpinner;
	
		OnItemSelectedListener exchangeSpinnerListener = new OnItemSelectedListener(){
		@Override
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			chosenExchange = exchanges.toArray(new String[exchanges.size()])[pos];
			
			coins = DataContainer.getExchangeMarketCoins(chosenExchange);
			
			coinSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, coins.toArray(new String[coins.size()])));


	    	updateValues(getView());
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
	};
	
	OnItemSelectedListener coinSpinnerListener = new OnItemSelectedListener(){
		@Override
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			chosenCoin = coins.toArray(new String[coins.size()])[pos];
			
	    	updateValues(getView());
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
	};
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    private void updateExchanges(View view){
    	if (!DataContainer.getExchangesWithMarket().equals(exchanges)){
			exchanges = DataContainer.getExchangesWithMarket();
			
			String[] prettyExchanges = view.getContext().getResources().getStringArray(R.array.exchanges);
			String[] exchangeCodes = ExchangeDataParser.exchangeCodes;
			
			List<String> actualExchanges = new ArrayList<String>();
			for (String exchange: exchanges)
				actualExchanges.add(prettyExchanges[Arrays.asList(exchangeCodes).indexOf(exchange)]);
			
			ArrayAdapter<String> exchangeAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, actualExchanges.toArray(new String[actualExchanges.size()]));
			exchangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			exchangeSpinner.setAdapter(exchangeAdapter);
			
			if (chosenExchange != null)
				((TextView) view.findViewById(R.id.market_text_exchange)).setText(prettyExchanges[Arrays.asList(exchangeCodes).indexOf(chosenExchange)]+":");
		}
    }
    
    private void updateCoins(View view){
    	if (chosenExchange != null && !DataContainer.getExchangeMarketCoins(chosenExchange).equals(coins)){
    		coins = DataContainer.getExchangeMarketCoins(chosenExchange);
			
			ArrayAdapter<String> coinAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, coins.toArray(new String[coins.size()]));
			coinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        coinSpinner.setAdapter(coinAdapter);
		}
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.market_fragment, container,false);
		
        exchangeSpinner = (Spinner) rootView.findViewById(R.id.market_select_exchange_spinner);
        exchangeSpinner.setOnItemSelectedListener(exchangeSpinnerListener);
        updateExchanges(rootView);
        
        coinSpinner = (Spinner) rootView.findViewById(R.id.market_select_coin_spinner);
        coinSpinner.setOnItemSelectedListener(coinSpinnerListener);
        updateCoins(rootView);

        return rootView;
	}
	
	@Override public void onResume(){
		super.onResume();
		DataContainer.registerRefreshable(this);
	}
	
	@Override public void onPause(){
		super.onPause();
		DataContainer.unregisterRefreshable(this);
	}
	
	public void refresh(Object lastData){
		updateValues(getView());
	}

    @Override
    public Set<Class<?>> continuousRequirements() {
        return baseRequirements;
    }

    protected void updateValues(View view){
		if (view == null){
			Log.w("InfoFragment","updateUserValues: View null");
			return;
		}
		
		updateExchanges(view);
		updateCoins(view);
		
		Float coinPriceXmr = 0f;
		String currencySymbol = view.getContext().getString(R.string.xmr_symbol);
		
		//get MoneroBlocksParser's price in Btc
		if (chosenExchange != null && chosenCoin != null && DataContainer.getExchangeMarketData(chosenExchange, chosenCoin) != null){
			ExchangeTickerData data = DataContainer.getExchangeMarketData(chosenExchange, chosenCoin);
			coinPriceXmr = data.getPrice();
			
			((TextView) view.findViewById(R.id.market_text_price_xmr)).setText(String.format("%.8f",coinPriceXmr)+" "+currencySymbol);
			((TextView) view.findViewById(R.id.market_text_exchange_change)).setText(String.format("%+.3f", data.getChange()*100)+" %");
			((TextView) view.findViewById(R.id.market_text_exchange_volume)).setText(String.format("%.2f",data.getVolume())+" "+currencySymbol);
			
			Float totalVolume = 0f;
			for (String coin: coins)
				if (DataContainer.getExchangeMarketData(chosenExchange, coin) != null)
					totalVolume += DataContainer.getExchangeMarketData(chosenExchange, coin).getVolume();
			
			((TextView) view.findViewById(R.id.market_text_exchange_total_volume)).setText(String.format("%.2f",totalVolume)+" "+currencySymbol);
		}
	}
	
	
	
}
