package tr.monerostatus.jsondata;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONTokener;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

public class JSONRetriever extends AsyncTask<Void, Void, Object>{
	private static TrustManager[] trustAllCerts = new TrustManager[] {
		new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
		    }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
		    }
		}
	};

    // Create all-trusting host name verifier
	static {
		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		}
		catch (NoSuchAlgorithmException e) {
		}
		catch (KeyManagementException e) {
		}
	    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	    HostnameVerifier allHostsValid = new HostnameVerifier() {
	        public boolean verify(String hostname, SSLSession session) {
	          return true;
	        }
	    };
	    //HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	private static int active = 0;
	private static Object activeLock = new Object();

    private final static ThreadPoolExecutor executor = new ThreadPoolExecutor(15, 30, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(200));
	
	private final int RETRIES = 3;
	public final String url;
	private final JSONParser middleware;
	private final JSONUpdatable updatable;
	public final Resources resources;
	
	public JSONRetriever(String url, JSONParser middleware, JSONUpdatable updatable, Resources resources){
		
		this.url = url;
		this.middleware = middleware;
		this.updatable = updatable;
		this.resources = resources;
		synchronized(activeLock){
			active ++;
		}
	}

    public void execute(){
        this.executeOnExecutor(executor);
    }

    @Override
    protected Object doInBackground(Void... params) {
		for (int i=0; i<RETRIES; i++){
			JSONTokener tok = getData();
			if (tok == null){
    			Log.e("JSONRetriever", "Retrying api request on "+url); 
    			continue;
			}
			return middleware.parseJSONData(this, tok); 
    	}
		Log.e("JSONRetriever", "Third consecutive request failed on "+url);
		return middleware.parseJSONData(this, null);
    }

    @Override
    protected void onPostExecute(Object result) {
		synchronized(activeLock){
			if (active>0)
				active -= 1;
		}
		updatable.updateFromJSONData(this, result);
    }
    
    @Override
    protected void onPreExecute() {
    }
	
	public JSONTokener getData() {
		
		InputStream inputStream = null;
		String result = null;
		try {
			HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
			
			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(10000);
			
		    inputStream = urlConnection.getInputStream();
		    
		    // json is UTF-8 by default
		    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
		    StringBuilder sb = new StringBuilder();

		    String line = null;
		    while ((line = reader.readLine()) != null){
		        sb.append(line + "\n");
		    }
		    result = sb.toString();
		}
		catch (Exception e) {
	        Log.e("JSONRetriever","HttpURLConnection failed ("+url+"): "+e.getLocalizedMessage());
		}
		finally {
		    try{
		    	if(inputStream != null)
		    		inputStream.close();
		    	}
		    catch(Exception e){
		    }
		}
		
		if (result != null)
			return new JSONTokener(result);
		
		return null;
		
	}
	
	public static boolean isFetching(){
		synchronized(activeLock){
			return active > 0;
		}
	}
	
	public interface JSONParser {
		public Object parseJSONData(JSONRetriever retriever, JSONTokener data);
	}
	
	public interface JSONUpdatable {
		public void updateFromJSONData(JSONRetriever retriever, Object data);
	}


}
