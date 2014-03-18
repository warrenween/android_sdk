/**
 * 
 */
package com.chartbeat.androidsdk;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * This class is responsible for actually sending data to Charbeat and processing the results.
 * 
 * @author bjorn
 *
 */
final class Pinger {
	private static final String URL = "http://ping.chartbeat.net/ping";
	private static final String TAG = "Chartbeat Pinger";
	DefaultHttpClient httpClient = new DefaultHttpClient();

	/**
	 * 
	 */
	public Pinger(String userAgent) {
		httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Custom user agent");
	}
	
	/** returns true on success, false if the server wants us to wait, and an exception in any other case.
	 * */
	int ping(HashMap<String,String> parameters) throws IOException {
	    HttpResponse response;
		while( true ) {
			// setup the call
			HttpGet get = new HttpGet(URL);
			BasicHttpParams params = new BasicHttpParams();
			for( HashMap.Entry<String,String> e : parameters.entrySet() )
				params.setParameter(e.getKey(), e.getValue());
			get.setParams(params);
			// execute the call
			response = httpClient.execute(get);
			response.getEntity().consumeContent();
			// results:
		    StatusLine statusLine = response.getStatusLine();
		    int code = statusLine.getStatusCode();
		    switch( code ) {
		    case 200:
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
}
