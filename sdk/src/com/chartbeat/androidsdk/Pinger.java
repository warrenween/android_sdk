/**
 * 
 */
package com.chartbeat.androidsdk;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;

/**
 * This class is responsible for actually sending data to Charbeat and processing the results.
 * 
 * @author bjorn
 *
 */
final class Pinger {
	private static final boolean TEST_RANDOM_FAILURES = false;
	private static final String SCHEME = "http";
	private static final String AUTHORITY = "ping.chartbeat.net";
	private static final String PATH = "ping";
//	private static final String URL = "http://ping.chartbeat.net/ping";
	private static final String TAG = "Chartbeat Pinger";
	DefaultHttpClient httpClient = new DefaultHttpClient();
	
	private final Random random = new Random(0); //used only for testing

	/**
	 * 
	 */
	public Pinger(String userAgent) {
		httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Custom user agent");
	}
	
	/** returns true on success, false if the server wants us to wait, and an exception in any other case.
	 * */
	int ping(List<KeyValuePair> parameters) throws IOException {
	    HttpResponse response;
		while( true ) {
			// setup the call
			Uri.Builder builder = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH);
			for( KeyValuePair e : parameters )
				builder.appendQueryParameter(e.getKey(), e.getValue());
			HttpGet get = new HttpGet( builder.build().toString() );
			if( Tracker.DEBUG )
				System.out.println( get.getURI() );
			
			// execute the call
			response = httpClient.execute(get);
		    StatusLine statusLine = response.getStatusLine();
		    int code = statusLine.getStatusCode();
			if( Tracker.DEBUG && ( code < 200 || code >= 300 ) ) {
				System.out.println( EntityUtils.toString(response.getEntity()) );
			} else {
				response.getEntity().consumeContent();
			}
			
			// results:
		    switch( code ) {
		    case 200:
		    	if( TEST_RANDOM_FAILURES ) {
		    		int r = random.nextInt(6);
		    		if( r == 0 ) {
		    			Log.w(TAG, "Simulating a fake 400 response." );
		    			return 400;
		    		}
		    		if( r > 2 ) {
		    			Log.w(TAG, "Simulating a fake 503 response." );
		    			return 503;
		    		}
		    	}
		    case 500:
		    case 503:
		    	return code;
		    default:
		    	//standard responses:
		    	if( code < 200 || code >= 600 ) {
		    		throw new IOException("Unkown response code: " + code);
		    	}
		    	if( code >= 200 && code < 300 ) {
		    		//this is considered a success, but we'll log it b/c it's not documented.
		    		Log.w(TAG, "Unexpected success response code: " + code );
		    		return 200;
		    	}
		    	if( code >= 300 && code < 400 ) {
		    		//misc redirect. should have been handled by httpclient, since this is a GET
		    		throw new IOException("HttpClient did not handle redirect: " + code );
		    	}
		    	if( code >= 400 && code < 500 ) {
		    		//client error.
		    		throw new IOException("HttpClient error: " + code );
		    	}
		    	if( code >= 500 && code < 600 ) {
		    		Log.w(TAG, "Unexpected server response code: " + code );
		    		return 503; // retry later
		    	}
		    	throw new IOException("Unknown error: " + code );
		    }
		}
	}
	
	public static boolean isConnected(Context context)
	{
	    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    android.net.NetworkInfo info = cm.getActiveNetworkInfo();
	    if( info == null )
	    	return false;
	    return info.isConnectedOrConnecting();
	}
	
	public static class KeyValuePair {
		public final String key, note, value;
		public KeyValuePair(String key, String note, String value) {
			this.key = key;
			this.note = note;
			this.value = value;
		}
		public String getKey() {
			return key;
		}
		public String getValue() {
			return value;
		}
		public String toString() {
			return key + " (" + note + ")=" + value;
		}
	}
}
