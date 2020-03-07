package tr.monerostatus.experimental;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import tr.monerostatus.DataContainer;
import tr.monerostatus.R;
import tr.monerostatus.jsondata.ExchangeDataParser;

public class AlarmFragment extends Fragment implements DataContainer.Refreshable {
	public static String FRAGMENT_TAG = "alarm_fragment";
	public static String SENDER_ID = "350840654464";

	private static final String PREFS_NAME = "XMRStatus_alarm";
    private static final String PREF_REG_ID = "registration_id";
    private static final String PREF_MSG_ID = "message_id";
    private static final String PREF_EXCHANGE = "exchange";
    private static final String PREF_LOWER_ON = "lower_on";
    private static final String PREF_LOWER = "lower";
    private static final String PREF_HIGHER_ON = "higher_on";
    private static final String PREF_HIGHER = "higher";

    public static final Boolean LOWER = true;
    public static final Boolean HIGHER = false;

    private static final String MESSAGE_STATE_LOWER = "Lower";
    private static final String MESSAGE_STATE_HIGHER = "Higher";
    private static final String MESSAGE_VALUE_LOWER = "ValueL";
    private static final String MESSAGE_VALUE_HIGHER = "ValueH";
    private static final String MESSAGE_EXCHANGE = "Exchange";

    private static final String TAG = "GoogleCloudMessaging";
    private static final String SERVER = "@gcm.googleapis.com";

    private final static long MESSAGEDELAYTIMER = 5000;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static RequestSender sendRequest = null;
    private volatile GoogleCloudMessaging gcm;
    private String chosenExchange;
    private static Set<Class<?>> baseRequirements = new HashSet<Class<?>>();
    static{
        baseRequirements.add(ExchangeDataParser.ExchangeTickerData.class);
        baseRequirements.addAll(Arrays.asList(ExchangeDataParser.exchangeClasses));
    }

    private Spinner exchangeSpinner;
    private Switch lowerSwitch;
    private Switch higherSwitch;
    private EditText editLower;
    private EditText editHigher;

    private volatile boolean internetOn = false;
    private volatile boolean gcmOn = false;

    SpinnerInteractionListener exchangeSpinnerListener = new SpinnerInteractionListener();

    private final SwitchListener lowerHigherListener = new SwitchListener();

    TextWatcher lowerEditWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            try {
                Float lower = 0f;
                if (s.length() != 0)
                    lower = Float.valueOf(s.toString());
                setAlert(getActivity(), gcm, LOWER, lower, null, null);
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

    TextWatcher higherEditWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            try {
                Float higher = 0f;
                if (s.length() != 0)
                    higher = Float.valueOf(s.toString());
                setAlert(getActivity(), gcm, HIGHER, higher, null, null);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.alarm_fragment, container,false);
        Context context = getActivity();

        exchangeSpinner = (Spinner)rootView.findViewById(R.id.alarm_select_exchange_spinner);
        lowerSwitch = (Switch)rootView.findViewById(R.id.alarm_lower_set);
        higherSwitch = (Switch)rootView.findViewById(R.id.alarm_higher_set);
        editLower = (EditText)rootView.findViewById(R.id.alarm_lower_edit);
        editHigher = (EditText)rootView.findViewById(R.id.alarm_higher_edit);


        ArrayAdapter<CharSequence> exchangeAdapter = ArrayAdapter.createFromResource(rootView.getContext(), R.array.exchanges, android.R.layout.simple_spinner_item);

        exchangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exchangeSpinner.setAdapter(exchangeAdapter);
        exchangeSpinner.setOnTouchListener(exchangeSpinnerListener);
        exchangeSpinner.setOnItemSelectedListener(exchangeSpinnerListener);

        String exchange = getAlertExchange(context);
        if (exchange == null) {
            setAlertExchange(ExchangeDataParser.exchangeCodes[0]);
            exchange = ExchangeDataParser.exchangeCodes[0];
        }

        chosenExchange = exchange;
        exchangeSpinner.setSelection(Arrays.asList(ExchangeDataParser.exchangeCodes).indexOf(exchange));

		
		if (!checkPlayServices()) {
            setControlsEnabled(false);
            Toast.makeText(context,R.string.alarm_no_play_services,Toast.LENGTH_SHORT).show();
            return rootView;
        }

        register(context);
        checkInternetConnection(context);

        lowerSwitch.setChecked(isAlertLowerOn(context));
        lowerSwitch.setOnTouchListener(lowerHigherListener);
        lowerSwitch.setOnCheckedChangeListener(lowerHigherListener);

        higherSwitch.setChecked(isAlertHigherOn(context));
        higherSwitch.setOnTouchListener(lowerHigherListener);
        higherSwitch.setOnCheckedChangeListener(lowerHigherListener);

        Float lower = getAlertLower(context);
        editLower.setText(new BigDecimal(lower.toString()).toPlainString());
        editLower.addTextChangedListener(lowerEditWatcher);

        Float higher = getAlertHigher(context);
        editHigher.setText(new BigDecimal(higher.toString()).toPlainString());
        editHigher.addTextChangedListener(higherEditWatcher);

        setControlsEnabled(false);

		return rootView;
	}
	
	@Override public void onResume(){
		super.onResume();

        if (!checkPlayServices())
            setControlsEnabled(false);

        DataContainer.registerRefreshable(this);

        updateValues(getView());
	}
	
	@Override public void onPause(){
		super.onPause();

        DataContainer.unregisterRefreshable(this);
	}
	
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
	    if (resultCode != ConnectionResult.SUCCESS) {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            Log.w(TAG, "This device is not supported by Google Play Services.");
	        }
	        return false;
	    }
	    return true;
	}

    public void disableAlerts(){
        lowerSwitch.setChecked(isAlertLowerOn(getActivity()));
        higherSwitch.setChecked(isAlertHigherOn(getActivity()));
    }
	
	private void register(final Context context) {

        new AsyncTask<Void, Void, GoogleCloudMessaging>() {

            @Override
            protected GoogleCloudMessaging doInBackground(Void... args) {
                try {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                    String token = InstanceID.getInstance(context).getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    storeRegistrationId(token, context);
                    return gcm;
                }
                catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(GoogleCloudMessaging gcm) {
                if (gcm != null) {
                    AlarmFragment.this.gcm = gcm;
                    gcmOn = true;
                    if (internetOn)
                        setControlsEnabled(true);
                }
                else
                    Toast.makeText(context,R.string.alarm_no_internet,Toast.LENGTH_SHORT).show();
            }
        }.execute(null, null, null);

	}

    private void checkInternetConnection(final Context context) {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... args) {
                return connectedToInternet();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    internetOn = true;
                    if (gcmOn)
                        setControlsEnabled(true);
                }
                else
                    Toast.makeText(context,R.string.alarm_no_internet,Toast.LENGTH_SHORT).show();


            }
        }.execute(null, null, null);
    }

    private static void sendUpstream(final Context context, final GoogleCloudMessaging gcm, Boolean lowerOnTmp, Boolean higherOnTmp, Float valueLowerTmp, Float valueHigherTmp, String exchangeTmp){

        final Boolean lowerOn = lowerOnTmp==null?isAlertLowerOn(context):lowerOnTmp;
        final Boolean higherOn = higherOnTmp==null?isAlertHigherOn(context):higherOnTmp;
        final Float valueLower = valueLowerTmp==null?getAlertLower(context):valueLowerTmp;
        final Float valueHigher = valueHigherTmp==null?getAlertHigher(context):valueHigherTmp;
        final String exchange = exchangeTmp==null?getAlertExchange(context):exchangeTmp;

        final RequestSender tmpSendRequest = new RequestSender(context, gcm, lowerOn, higherOn, valueLower, valueHigher, exchange);

        if (sendRequest != null)
            sendRequest.cancel(false);
        sendRequest = tmpSendRequest;
        sendRequest.execute();
    }
	
	private static void storeRegistrationId(String regId, Context context) {
		final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	    
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PREF_REG_ID, regId);
        editor.apply();
	}

    private static String getRegistrationId(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_REG_ID, "");
    }

    private void setControlsEnabled(boolean toggle){
        exchangeSpinner.setEnabled(toggle);
        lowerSwitch.setEnabled(toggle);
        higherSwitch.setEnabled(toggle);
        editLower.setEnabled(toggle);
        editHigher.setEnabled(toggle);
    }

    public static void resendAlerts(Context context, GoogleCloudMessaging gcm){
        sendUpstream(context, gcm, null, null, null, null, null);
    }

    public static void setAlert(Context context, GoogleCloudMessaging gcm, Boolean type, Float value, Boolean on, String exchange){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        String prefValue, prefOn;

        if (type == LOWER){
            prefValue = PREF_LOWER;
            prefOn = PREF_LOWER_ON;
        }
        else{
            prefValue = PREF_HIGHER;
            prefOn = PREF_HIGHER_ON;
        }

        if (value != null)
            editor.putFloat(prefValue, value);
        else
            value = prefs.getFloat(prefValue, 0f);
        if (on != null)
            editor.putBoolean(prefOn, on);
        else
            on = prefs.getBoolean(prefOn, false);

        editor.apply();
        sendUpstream(context, gcm, type == LOWER ? on : null, type == HIGHER ? on : null, type == LOWER ? value : null, type == HIGHER ? value : null, exchange);
    }

    private static String getMessageId(Context context){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int msgId = prefs.getInt(PREF_MSG_ID, 0);
        String regId = getRegistrationId(context);
        msgId ++;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_MSG_ID, msgId);
        editor.apply();
        return regId + "+" + msgId;
    }

    private void setAlertExchange(String exchange) {
        Context context = getActivity();
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_EXCHANGE, exchange);
        editor.apply();
        setAlert(context, gcm, LOWER, null, null, exchange);
        setAlert(context, gcm, HIGHER, null, null, exchange);
    }

    public static Float getAlertLower(Context context){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(PREF_LOWER, 0f);
    }

    public static Float getAlertHigher(Context context){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(PREF_HIGHER, 0f);
    }

    public static Boolean isAlertLowerOn(Context context){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_LOWER_ON, Boolean.FALSE);
    }

    public static Boolean isAlertHigherOn(Context context){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_HIGHER_ON, Boolean.FALSE);
    }

    public static String getAlertExchange(Context context){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String exchange = prefs.getString(PREF_EXCHANGE, "");
        if (exchange.isEmpty())
            return null;
        return exchange;
    }

    private static boolean connectedToInternet() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ping = runtime.exec("/system/bin/ping -c 1 gcm.googleapis.com");
            if(ping.waitFor() == 0)
                return true;
            else
                return false;
        }
        catch (Exception e){
            return false;
        }
    }

    private class SpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

        boolean userSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (userSelect) {
                chosenExchange = ExchangeDataParser.exchangeCodes[pos];
                setAlertExchange(chosenExchange);
                userSelect = false;

            }
            updateValues(getView());
        }

        public void onNothingSelected(AdapterView<?> parent) {
        }

    }

    private class SwitchListener implements CompoundButton.OnCheckedChangeListener, View.OnTouchListener {

        boolean userSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (userSelect) {
                if (isChecked) {
                    if (buttonView.equals(lowerSwitch)) {
                        setAlert(getActivity(), gcm, LOWER, null, true, null);
                    } else {
                        setAlert(getActivity(), gcm, HIGHER, null, true, null);
                    }
                } else {
                    if (buttonView.equals(lowerSwitch)) {
                        setAlert(getActivity(), gcm, LOWER, null, false, null);
                    } else {
                        setAlert(getActivity(), gcm, HIGHER, null, false, null);
                    }
                }
                userSelect = false;
            }
        }
    }

    private static class RequestSender extends AsyncTask<Void, Void, Void>{
        private volatile boolean sleeping = false;
        private volatile Thread backgroundThread = null;
        private final Context context;
        private final GoogleCloudMessaging gcm;
        private final Boolean lowerOn;
        private final Boolean higherOn;
        private final Float valueLower;
        private final Float valueHigher;
        private final String exchange;

        public RequestSender(Context contextTmp, GoogleCloudMessaging gcmTmp, Boolean lowerOnTmp, Boolean higherOnTmp, Float valueLowerTmp, Float valueHigherTmp, String exchangeTmp){
            context = contextTmp;
            gcm = gcmTmp;
            lowerOn = lowerOnTmp;
            higherOn = higherOnTmp;
            valueLower = valueLowerTmp;
            valueHigher = valueHigherTmp;
            exchange = exchangeTmp;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (sleeping)
                backgroundThread.interrupt();
        }

        @Override
        protected Void doInBackground(Void... params) {
            backgroundThread = Thread.currentThread();
            if (gcm == null)
                return null;
            try{
                sleeping = true;
                Thread.sleep(MESSAGEDELAYTIMER);
                sleeping = false;
            }
            catch(Exception e){
                sleeping = false;
                return null;
            }

            if (isCancelled())
                return null;

            try {

                final String id = getMessageId(context);

                Bundle data = new Bundle();
                data.putString(MESSAGE_STATE_LOWER, lowerOn.toString());
                data.putString(MESSAGE_STATE_HIGHER, higherOn.toString());
                data.putString(MESSAGE_VALUE_LOWER, valueLower.toString());
                data.putString(MESSAGE_VALUE_HIGHER, valueHigher.toString());
                data.putString(MESSAGE_EXCHANGE, exchange);

                if (!isCancelled()) {
                    gcm.send(SENDER_ID + SERVER, id, data);
                }
            }
            catch (IOException ex) {
                return null;
            }
            return null;
        }
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
            Log.w("AlarmFragment","updateValues: View null");
            return;
        }

        Float xmrPriceBtc = 0f;

        if (DataContainer.getExchangeTickerData(chosenExchange) != null){
            xmrPriceBtc = DataContainer.getExchangeTickerData(chosenExchange).getPrice();

            ((TextView) view.findViewById(R.id.alarm_text_price_btc)).setText(xmrPriceBtc+" "+getString(R.string.btc_symbol));
        }
        else{
            ((TextView) view.findViewById(R.id.alarm_text_price_btc)).setText("");
        }

    }
}
