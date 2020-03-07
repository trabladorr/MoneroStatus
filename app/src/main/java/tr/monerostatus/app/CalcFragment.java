package tr.monerostatus.app;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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

public class CalcFragment extends Fragment implements Refreshable {
	public static String FRAGMENT_TAG = "calc_fragment";
	
	public static final String PREFS_NAME = "XMRStatus_calc";
    public static final String PREF_USER_COINS = "user_coins";
    public static final String PREF_USER_BTC = "user_btc";
	public static final String PREF_USER_HASHRATE = "user_hashrate";

    private Float userCoinsValue = 0f;
    private Float userBtcValue = 0f;
	private Float userHashrateValue = 0f;

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
	
	OnItemSelectedListener currencySpinnerListener = new OnItemSelectedListener(){
		@Override
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	    	chosenCurrency = BitcoinAverageParser.currency_codes[pos];
	    	
	        SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(InfoFragment.PREFS_NAME, 0).edit();
    		prefsEd.putString(InfoFragment.PREF_USER_CURRENCY, chosenCurrency);
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
			
	        SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(InfoFragment.PREFS_NAME, 0).edit();
    		prefsEd.putString(InfoFragment.PREF_USER_EXCHANGE, chosenExchange);
    		prefsEd.commit();
			
	    	updateValues(getView());
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
	};
	
	TextWatcher coinWatcher= new TextWatcher() {
        public void afterTextChanged(Editable s) {
        	try {
        		if (s.length() == 0)
        			userCoinsValue = 0f;
        		else
        			userCoinsValue = Float.valueOf(s.toString());
        		SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(PREFS_NAME, 0).edit();
        		prefsEd.putFloat(PREF_USER_COINS, userCoinsValue);
        		prefsEd.commit();
        	} 
			catch (Exception e) {
				Log.e(this.getClass().getSimpleName(),e.getLocalizedMessage());
			}
        	
        	updateValues(getView());
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {            
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    TextWatcher btcWatcher= new TextWatcher() {
        public void afterTextChanged(Editable s) {
            try {
                if (s.length() == 0)
                    userBtcValue = 0f;
                else
                    userBtcValue = Float.valueOf(s.toString());
                SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(PREFS_NAME, 0).edit();
                prefsEd.putFloat(PREF_USER_BTC, userBtcValue);
                prefsEd.commit();
            }
            catch (Exception e) {
                Log.e(this.getClass().getSimpleName(),e.getLocalizedMessage());
            }

            updateValues(getView());
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };
    
    TextWatcher hashrateWatcher= new TextWatcher() {
        public void afterTextChanged(Editable s) {
        	try {
        		if (s.length() == 0)
        			userHashrateValue = 0f;
        		else
        			userHashrateValue = Float.valueOf(s.toString());
        		SharedPreferences.Editor prefsEd = getActivity().getSharedPreferences(PREFS_NAME, 0).edit();
        		prefsEd.putFloat(PREF_USER_HASHRATE, userHashrateValue);
        		prefsEd.commit();
        	} 
			catch (Exception e) {
				Log.e(this.getClass().getSimpleName(),e.getLocalizedMessage());
			}
        	
        	updateValues(getView());
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {            
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.calc_fragment, container,false);
		
        Spinner currencySpinner = (Spinner) rootView.findViewById(R.id.calc_select_currency_spinner);
        ArrayAdapter<CharSequence> currencyAdapter = ArrayAdapter.createFromResource(rootView.getContext(), R.array.bitcoinaverage_currencies, android.R.layout.simple_spinner_item);
        
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);
        currencySpinner.setOnItemSelectedListener(currencySpinnerListener);
        
        setSpinnerCurrency(getActivity().getSharedPreferences(InfoFragment.PREFS_NAME, 0).getString(InfoFragment.PREF_USER_CURRENCY, BitcoinAverageParser.currency_codes[0]), currencySpinner);
        
        
        Spinner exchangeSpinner = (Spinner) rootView.findViewById(R.id.calc_select_exchange_spinner);
        ArrayAdapter<CharSequence> exchangeAdapter = ArrayAdapter.createFromResource(rootView.getContext(), R.array.exchanges, android.R.layout.simple_spinner_item);
        
        exchangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exchangeSpinner.setAdapter(exchangeAdapter);
        exchangeSpinner.setOnItemSelectedListener(exchangeSpinnerListener);
        
        setSpinnerExchange(getActivity().getSharedPreferences(InfoFragment.PREFS_NAME, 0).getString(InfoFragment.PREF_USER_EXCHANGE, ExchangeDataParser.exchangeCodes[0]), exchangeSpinner);
        
        
        SharedPreferences pref = rootView.getContext().getSharedPreferences(PREFS_NAME, 0);
        userCoinsValue = pref.getFloat(PREF_USER_COINS, 0f);
        userBtcValue = pref.getFloat(PREF_USER_BTC, 0f);
        userHashrateValue = pref.getFloat(PREF_USER_HASHRATE, 0f);
        
        EditText userCoins = (EditText) rootView.findViewById(R.id.calc_user_coins);
        if (userCoinsValue != 0)
        	userCoins.setText(userCoinsValue.toString());
        userCoins.addTextChangedListener(coinWatcher);

        EditText userBtc = (EditText) rootView.findViewById(R.id.calc_user_btc);
        if (userBtcValue != 0)
            userBtc.setText(userBtcValue.toString());
        userBtc.addTextChangedListener(btcWatcher);
        
        EditText userHashrate = (EditText) rootView.findViewById(R.id.calc_user_hashrate);
        if (userHashrateValue != 0)
        	userHashrate.setText(userHashrateValue.toString());
        userHashrate.addTextChangedListener(hashrateWatcher);
        
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

	public void setSpinnerCurrency(String currency, Spinner spinner) {
		if (currency == null)
			return;
		
		if (spinner == null){
			CalcFragment frag = (CalcFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
	        spinner = (Spinner) frag.getView().findViewById(R.id.calc_select_currency_spinner);
		}
		
        spinner.setSelection(Arrays.asList(BitcoinAverageParser.currency_codes).indexOf(currency));
   
		this.chosenCurrency = currency;
	}
	
	public void setSpinnerExchange(String exchange, Spinner spinner) {
		if (exchange == null)
			return;
		
		if (spinner == null){
			CalcFragment frag = (CalcFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
	        spinner = (Spinner) frag.getView().findViewById(R.id.calc_select_exchange_spinner);
		}
		
        spinner.setSelection(Arrays.asList(ExchangeDataParser.exchangeCodes).indexOf(exchange));
        
		this.chosenExchange = exchange;
	}
	
	protected void updateValues(View view){
		if (view == null){
			Log.w("InfoFragment","updateUserValues: View null");
			return;
		}

		Float xmrPriceBtc = 0f;
        Double btcPriceCurrency = 0d;
		Double projection = 0d;
		
		String currencySymbol = getResources().getString(getResources().getIdentifier(chosenCurrency+"_symbol", "string", MainActivity.PACKAGE_NAME));
		
		//get Bitcoin's price in chosen currency
		if (DataContainer.getFixerIOData() != null && DataContainer.getCoinmarketcapBtcData() != null)
            btcPriceCurrency = DataContainer.getBTCPriceCurrency(chosenCurrency);
		
		//get MoneroBlocksParser's price in Btc
    	if (DataContainer.getExchangeTickerData(chosenExchange) != null)
    		xmrPriceBtc = DataContainer.getExchangeTickerData(chosenExchange).getPrice();
    	
    	//calculate mining projection
    	if (DataContainer.getBlockExplorerData() != null && DataContainer.getBlockExplorerData().getReward() != null){
			projection = DataContainer.getBlockExplorerData().getReward()*Double.valueOf(30*24)*(userHashrateValue/(Double.valueOf(DataContainer.getBlockExplorerData().getHashrate()*1000000)));
			projection *= 0.9; //10% orphan
    	}
    	
    	//User's coins' price in Btc
    	if (xmrPriceBtc != 0)
			((TextView) view.findViewById(R.id.calc_text_user_price_btc)).setText(String.format("%.3f",userCoinsValue * xmrPriceBtc)+" "+getString(R.string.btc_symbol));		
    	else
    		((TextView) view.findViewById(R.id.calc_text_user_price_btc)).setText("");
    	
    	//User's coins' price in chosen currency
    	if (xmrPriceBtc != 0 && btcPriceCurrency != 0) {
            ((TextView) view.findViewById(R.id.calc_text_user_price_currency)).setText(String.format("%.3f", userCoinsValue * xmrPriceBtc * btcPriceCurrency) + " " + currencySymbol);
            ((TextView) view.findViewById(R.id.calc_text_user_btc_price_currency)).setText(String.format("%.3f", userBtcValue * btcPriceCurrency) + " " + currencySymbol);
            ((TextView) view.findViewById(R.id.calc_text_user_total_price_currency)).setText(String.format("%.3f", (userCoinsValue * xmrPriceBtc + userBtcValue) * btcPriceCurrency) + " " + currencySymbol);
        }
    	else
    		((TextView) view.findViewById(R.id.calc_text_user_price_currency)).setText("");
    	
    	//mining projection
    	((TextView) view.findViewById(R.id.calc_text_user_mine_projection)).setText(String.format("%.4f", projection));
    	
    	//mining projection's value in chosen currency
		if (xmrPriceBtc != 0 && btcPriceCurrency != 0)
			((TextView) view.findViewById(R.id.calc_text_user_mine_value_projection)).setText(String.format("%.3f", projection * xmrPriceBtc * btcPriceCurrency)+" "+currencySymbol);
		else
			((TextView) view.findViewById(R.id.calc_text_user_mine_value_projection)).setText("");
	}
	
	
	
}
