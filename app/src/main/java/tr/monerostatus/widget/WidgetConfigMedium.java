package tr.monerostatus.widget;

import tr.monerostatus.R;
import tr.monerostatus.jsondata.BitcoinAverageParser;
import tr.monerostatus.jsondata.ExchangeDataParser;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class WidgetConfigMedium extends Activity{
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
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_HASHRATE);
	        prefsEd.remove(CoinWidgetTools.PREF_PREFIX_KEY + appWidgetId + CoinWidgetTools.PREF_TIMESTAMP);
		
            prefsEd.apply();

            // Update all widgets

            startService(new Intent(getApplicationContext(), WidgetUpdaterMedium.class));
            
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_config_medium);

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
        
    }

}
