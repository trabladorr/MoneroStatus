package tr.monerostatus.widget;

import tr.monerostatus.R;
import tr.monerostatus.app.CalcFragment;
import tr.monerostatus.jsondata.BitcoinAverageParser;
import tr.monerostatus.jsondata.ExchangeDataParser;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class WidgetConfigLarge extends Activity{
    private int appWidgetId = 0;
    String currency = null;
    String exchange = null;
    private Spinner currencySpinner;
    private Spinner exchangeSpinner;
    
    OnClickListener okClickListener  = new OnClickListener() {
        @Override
        public void onClick(View v) {
            
            // Setting the widget data in shared storage
            SharedPreferences.Editor prefsEd = getSharedPreferences(CoinWidgetTools.PREFS_NAME, 0).edit();
            prefsEd.putString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CURRENCY, currency);
            prefsEd.putString(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE, exchange);
            
            prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_PRICE_BTC);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_CHANGE);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_BTC_PRICE_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_PRICE_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_VOLUME_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_EXCHANGE_VOLUME_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_HASHRATE);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_TIMESTAMP);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_COINS_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_USER_PROJECTION_CURRENCY);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_MARKET_CAP);
	        
            prefsEd.apply();

            // Update all widgets

            startService(new Intent(getApplicationContext(), WidgetUpdaterLarge.class));
            
            // Return RESULT_OK from this activity
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };
    
    OnItemSelectedListener currencySpinnerListener = new OnItemSelectedListener(){
    	@Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        	currency = BitcoinAverageParser.currency_codes[pos];
        }

    	@Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    
    OnItemSelectedListener exchangeSpinnerListener = new OnItemSelectedListener(){
    	@Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        	exchange = ExchangeDataParser.exchangeCodes[pos];
        }

    	@Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    
    TextWatcher coinWatcher= new TextWatcher() {
        public void afterTextChanged(Editable s) {
        	try {
        		Float userCoinsValue;
        		
        		if (s.length() == 0)
        			userCoinsValue = 0f;
        		else
        			userCoinsValue = Float.valueOf(s.toString());
        		SharedPreferences.Editor prefsEd = getSharedPreferences(CalcFragment.PREFS_NAME, 0).edit();
        		prefsEd.putFloat(CalcFragment.PREF_USER_COINS, userCoinsValue);
        		prefsEd.apply();
        	} 
			catch (Exception e) {
				Log.e(this.getClass().getSimpleName(),e.getLocalizedMessage());
			}
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {            
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };
    
    TextWatcher hashrateWatcher= new TextWatcher() {
        public void afterTextChanged(Editable s) {
        	try {
        		Float userHashrateValue;
        		
        		if (s.length() == 0)
        			userHashrateValue = 0f;
        		else
        			userHashrateValue = Float.valueOf(s.toString());
        		SharedPreferences.Editor prefsEd = getSharedPreferences(CalcFragment.PREFS_NAME, 0).edit();
        		prefsEd.putFloat(CalcFragment.PREF_USER_HASHRATE, userHashrateValue);
        		prefsEd.apply();
        	} 
			catch (Exception e) {
				Log.e(this.getClass().getSimpleName(),e.getLocalizedMessage());
			}
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {            
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_config_large);

        //get widgetId
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        
        if (extras != null)
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        
        //Button
        Button btnOk = (Button) findViewById(R.id.config_btn_ok);
        
        btnOk.setOnClickListener(okClickListener);
        
        
        //Currency Spinner
        currencySpinner = (Spinner) findViewById(R.id.config_select_currency_spinner);
 
        ArrayAdapter<CharSequence> currencySpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.bitcoinaverage_currencies, android.R.layout.simple_spinner_item);
        
        currencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencySpinnerAdapter);
        currencySpinner.setOnItemSelectedListener(currencySpinnerListener);
        
        //Exchange Spinner
        exchangeSpinner = (Spinner) findViewById(R.id.config_select_exchange_spinner);
 
        ArrayAdapter<CharSequence> exchangeSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.exchanges, android.R.layout.simple_spinner_item);
        
        exchangeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exchangeSpinner.setAdapter(exchangeSpinnerAdapter);
        exchangeSpinner.setOnItemSelectedListener(exchangeSpinnerListener);
     
        SharedPreferences pref = getSharedPreferences(CalcFragment.PREFS_NAME, 0);
        Float userCoinsValue = pref.getFloat(CalcFragment.PREF_USER_COINS, 0f);
        Float userHashrateValue = pref.getFloat(CalcFragment.PREF_USER_HASHRATE, 0f);
        
        EditText userCoins = (EditText) findViewById(R.id.config_user_coins);
        if (userCoinsValue != 0)
        	userCoins.setText(userCoinsValue.toString());
        userCoins.addTextChangedListener(coinWatcher);
        
        EditText userHashrate = (EditText) findViewById(R.id.config_user_hashrate);
        if (userHashrateValue != 0)
        	userHashrate.setText(userHashrateValue.toString());
        userHashrate.addTextChangedListener(hashrateWatcher);
    }

}
