package tr.monerostatus.app;

import android.app.Fragment;
import android.content.SharedPreferences;
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

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import tr.monerostatus.DataContainer;
import tr.monerostatus.DataContainer.Refreshable;
import tr.monerostatus.R;
import tr.monerostatus.jsondata.BitcoinAverageParser;
import tr.monerostatus.jsondata.BlockExplorerDataParser;
import tr.monerostatus.jsondata.CoinMarketCapNorthpoleParser;
import tr.monerostatus.jsondata.CoinMarketCapParser;
import tr.monerostatus.jsondata.ExchangeDataParser;
import tr.monerostatus.jsondata.FixerIOParser;

public class InfoFragment extends Fragment implements Refreshable {
	public static String FRAGMENT_TAG = "info_fragment";

	private String chosenCurrency = null;
	private String chosenExchange = null;
	private static Set<Class<?>> baseRequirements = new HashSet<Class<?>>();
	static{
		baseRequirements.add(CoinMarketCapParser.class);
		baseRequirements.add(BlockExplorerDataParser.class);
//		baseRequirements.add(BitcoinAverageParser.class);
		baseRequirements.add(FixerIOParser.class);
		baseRequirements.add(ExchangeDataParser.ExchangeTickerData.class);
		baseRequirements.addAll(Arrays.asList(ExchangeDataParser.exchangeClasses));
	}


	static final String PREFS_NAME = "XMRStatus_info";
    static final String PREF_USER_CURRENCY = "user_currency";
    static final String PREF_USER_EXCHANGE = "user_exchange";
		
	OnItemSelectedListener currencySpinnerListener = new OnItemSelectedListener(){
		@Override
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	    	chosenCurrency = BitcoinAverageParser.currency_codes[pos];
	    	
	        SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(PREFS_NAME, 0).edit();
    		prefsEd.putString(PREF_USER_CURRENCY, chosenCurrency);
    		prefsEd.commit();
    		
	    	updateValues(getView());
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
	};
	
	OnItemSelectedListener exchangeSpinnerListener = new OnItemSelectedListener(){
		@Override
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			chosenExchange = ExchangeDataParser.exchangeCodes[pos];
			
	        SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(PREFS_NAME, 0).edit();
    		prefsEd.putString(PREF_USER_EXCHANGE, chosenExchange);
    		prefsEd.commit();
    		
			String list[] = getResources().getStringArray(R.array.exchanges);
			((TextView) getView().findViewById(R.id.main_text_exchange)).setText(list[pos]+":");
			
	    	updateValues(getView());
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
	};
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.info_fragment, container,false);
		
        Spinner currencySpinner = (Spinner) rootView.findViewById(R.id.main_select_currency_spinner);
        ArrayAdapter<CharSequence> currencyAdapter = ArrayAdapter.createFromResource(rootView.getContext(), R.array.bitcoinaverage_currencies, android.R.layout.simple_spinner_item);
        
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);
        currencySpinner.setOnItemSelectedListener(currencySpinnerListener);
        
        setSpinnerCurrency(getActivity().getSharedPreferences(InfoFragment.PREFS_NAME, 0).getString(InfoFragment.PREF_USER_CURRENCY, BitcoinAverageParser.currency_codes[0]), currencySpinner);
        
        
        Spinner exchangeSpinner = (Spinner) rootView.findViewById(R.id.main_select_exchange_spinner);
        ArrayAdapter<CharSequence> exchangeAdapter = ArrayAdapter.createFromResource(rootView.getContext(), R.array.exchanges, android.R.layout.simple_spinner_item);
        
        exchangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exchangeSpinner.setAdapter(exchangeAdapter);
        exchangeSpinner.setOnItemSelectedListener(exchangeSpinnerListener);
        
        setSpinnerExchange(getActivity().getSharedPreferences(InfoFragment.PREFS_NAME, 0).getString(InfoFragment.PREF_USER_EXCHANGE, ExchangeDataParser.exchangeCodes[0]), exchangeSpinner);
		
		return rootView;
	}
	
	@Override public void onResume(){
		super.onResume();
		
		DataContainer.registerRefreshable(this);
        
        updateValues(getView());
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

	public void setSpinnerCurrency(String currency, Spinner spinner) {
		if (currency == null)
			return;
		
		if (spinner == null){
			InfoFragment frag = (InfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
	        spinner = (Spinner) frag.getView().findViewById(R.id.main_select_currency_spinner);
		}

        spinner.setSelection(Arrays.asList(BitcoinAverageParser.currency_codes).indexOf(currency));

		this.chosenCurrency = currency;
	}
	
	public void setSpinnerExchange(String exchange, Spinner spinner) {
		if (exchange == null)
			return;
		
		if (spinner == null){
			InfoFragment frag = (InfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
	        spinner = (Spinner) frag.getView().findViewById(R.id.main_select_exchange_spinner);
		}
		
        spinner.setSelection(Arrays.asList(ExchangeDataParser.exchangeCodes).indexOf(exchange));
        
		this.chosenExchange = exchange;
	}

	
	protected void updateValues(View view){
		if (view == null){
			Log.w("InfoFragment","updateValues: View null");
			return;
		}
		
		Double btcPriceCurrency = 0d;
		Float xmrPriceBtc = 0f;
//		Float totalVolume = 0f;
		String currencySymbol = getResources().getString(getResources().getIdentifier(chosenCurrency+"_symbol", "string", MainActivity.PACKAGE_NAME));
		Integer totalCoins = 0;
		
		//sum up exchanges' volumes
//		for (String exchange: ExchangeDataParser.exchangeCodes)
//			if (DataContainer.getExchangeTickerData(exchange) != null)
//				totalVolume += DataContainer.getExchangeTickerData(exchange).getVolume();

		//get BlockChainExplorer data (difficulty, hashrate, total coins, block height, last reward)
		if (DataContainer.getBlockExplorerData() != null){
			BlockExplorerDataParser.BlockExplorerData data = DataContainer.getBlockExplorerData();

			int pos = Arrays.asList(BlockExplorerDataParser.explorerCodes).indexOf(data.getExplorer());
			String list[] = getResources().getStringArray(R.array.explorers);
			((TextView) getView().findViewById(R.id.main_text_block_explorer_props)).setText(list[pos] + ":");

			Double megahash = data.getHashrate();
			totalCoins = data.getTotalCoins().intValue();
			
			((TextView) view.findViewById(R.id.main_text_difficulty)).setText(NumberFormat.getInstance().format(data.getDifficulty()));
			((TextView) view.findViewById(R.id.main_text_hashrate)).setText(String.format("%.4f", megahash)+" "+getResources().getString(R.string.megahash_per_second_symbol));
			((TextView) view.findViewById(R.id.main_text_total)).setText(NumberFormat.getInstance().format(data.getTotalCoins().intValue()));
			((TextView) view.findViewById(R.id.main_text_height)).setText(NumberFormat.getInstance().format(data.getHeight()));
			((TextView) view.findViewById(R.id.main_text_reward)).setText(NumberFormat.getInstance().format(data.getReward()));
		}
		
		//get BitCoin's price in chosen currency
		if (DataContainer.getFixerIOData() != null && DataContainer.getCoinmarketcapBtcData() != null){
			btcPriceCurrency = DataContainer.getBTCPriceCurrency(chosenCurrency);

		((TextView) view.findViewById(R.id.main_text_btc_price_currency)).setText(NumberFormat.getInstance().format(btcPriceCurrency.intValue())+" "+currencySymbol);
		}
		
		//get MoneroBlocksParser's price in Btc
		if (DataContainer.getExchangeTickerData(chosenExchange) != null){
			xmrPriceBtc = DataContainer.getExchangeTickerData(chosenExchange).getPrice();

			((TextView) view.findViewById(R.id.main_text_price_btc)).setText(xmrPriceBtc+" "+getString(R.string.btc_symbol));
			((TextView) view.findViewById(R.id.main_text_exchange_change)).setText(String.format("%+.3f",DataContainer.getExchangeTickerData(chosenExchange).getChange()*100)+" %");
			
			if (btcPriceCurrency != 0)
				((TextView) view.findViewById(R.id.main_text_exchange_volume)).setText(NumberFormat.getInstance().format(Double.valueOf(DataContainer.getExchangeTickerData(chosenExchange).getVolume()*btcPriceCurrency).intValue())+" "+currencySymbol);
			else
				((TextView) view.findViewById(R.id.main_text_exchange_volume)).setText("");
		}
		else{
			((TextView) view.findViewById(R.id.main_text_price_btc)).setText("");
			((TextView) view.findViewById(R.id.main_text_exchange_change)).setText("");
			((TextView) view.findViewById(R.id.main_text_exchange_volume)).setText("");
		}
		
		//CoinMarketCap statistics
		if (DataContainer.getCoinmarketcapXmrData() != null){
			((TextView) view.findViewById(R.id.main_text_change)).setText(DataContainer.getCoinmarketcapXmrData().getChange()+"%");	
			((TextView) view.findViewById(R.id.main_text_position)).setText(NumberFormat.getInstance().format(DataContainer.getCoinmarketcapXmrData().getPosition()));
		}
		
		//Derived fields
		
		//Market capitalization in chosen currency
		if (xmrPriceBtc != 0 && totalCoins != 0 && btcPriceCurrency != 0)
			((TextView) view.findViewById(R.id.main_text_marketcap)).setText(NumberFormat.getInstance().format(Double.valueOf(btcPriceCurrency * xmrPriceBtc * totalCoins).intValue())+" "+currencySymbol);
		else
			((TextView) view.findViewById(R.id.main_text_marketcap)).setText("");
		
		//price in chosen currency
		if (xmrPriceBtc != 0 && btcPriceCurrency != 0)
			((TextView) view.findViewById(R.id.main_text_price_currency)).setText(String.format("%.3f",btcPriceCurrency * xmrPriceBtc)+" "+currencySymbol);
		else
			((TextView) view.findViewById(R.id.main_text_price_currency)).setText("");
			
		//total 24 hour volume
		if (DataContainer.getXMRVolumeCurrency(chosenCurrency) != null)
		    ((TextView) view.findViewById(R.id.main_text_volume_exchanges)).setText(NumberFormat.getInstance().format(DataContainer.getXMRVolumeCurrency(chosenCurrency).intValue())+" "+currencySymbol);
//		if (totalVolume != 0 && btcPriceCurrency != 0)
//			((TextView) view.findViewById(R.id.main_text_volume_exchanges)).setText(NumberFormat.getInstance().format(Double.valueOf(totalVolume*btcPriceCurrency).intValue())+" "+currencySymbol);
//		else
//			((TextView) view.findViewById(R.id.main_text_volume_exchanges)).setText("");


	}
	
	
	
	
}
