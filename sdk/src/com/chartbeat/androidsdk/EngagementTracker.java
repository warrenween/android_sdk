/**
 * Chartbeat Android API by Bjorn Roche.
 * (c) Chartbeat 2014
 */
package com.chartbeat.androidsdk;

import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Log;


/**
 * Tracks user engagement and calculates the necessary metrics.
 * 
 * @author bjorn
 */
final class EngagementTracker extends TimerTask {
	private int engagementWindow = 5; //default to 5 until we get info from the server otherwise.
	private boolean engaged, typed;
	private long totalEngagement;
	private final int INITIAL_ENGAGEMENT = 5; //user is always considered engaged for the first 5 seconds.
	private long lastEngaged;
	private final long startTime;
	private final java.util.Timer timer = new java.util.Timer();

	EngagementTracker() {
		engaged = false;
		typed = false;
		lastEngaged = 0;
		
		startTime = System.currentTimeMillis();
		
		timer.schedule( this, 0, 1000 );
		
		timer.schedule( new EngagementWindowFetcher(), 0 );
	}
	
	synchronized void stop() {
		timer.cancel();
	}
	
	synchronized void setEngagementWindow( int window ) {
		engagementWindow = window;
	}
	
	synchronized void userEngaged() {
		engaged = true;
		lastEngaged = System.currentTimeMillis();
	}
	
	synchronized void userTyped() {
		typed = true;
		lastEngaged = System.currentTimeMillis();
	}
	
	synchronized void userEnteredView() {
		totalEngagement = 0;
		lastEngaged = 0;
	}

	synchronized void userLeftView() {
		//totalEngagement = 0;
		//lastEngaged = 0;
	}

	synchronized EngagementData ping() {
		EngagementData ret = new EngagementData( totalEngagement, engaged, typed );
		engaged = false;
		typed = false;
		return ret;
	}
	
	synchronized void lastPingFailed(EngagementData ed) {
		engaged |= ed.engaged;
		typed |= ed.typed;
	}

	@Override
	synchronized public void run() {
		long now = System.currentTimeMillis();
		if( now - lastEngaged <= engagementWindow*1000 || now - startTime <= INITIAL_ENGAGEMENT*1000 ) {
			++totalEngagement;
		}
	}
	static class EngagementData {
		final boolean engaged, typed, reading, idle;
		final long totalEngagement;
		private EngagementData( long totalEngagement, boolean engaged, boolean typed ) {
			this.engaged = engaged;
			this.typed = typed;
			this.reading = engaged && (!typed);
			this.idle = !engaged;
			this.totalEngagement = totalEngagement;
		}
	}
	class EngagementWindowFetcher extends TimerTask {
		public static final String TAG = "Engagement data Fetcher";
		public static final String SCHEME = "http";
		public static final String AUTHORITY = "static.chartbeat.com";
		public static final String PATH = "data/config.json";

		EngagementWindowFetcher() {
		}
		@Override
		public void run() {
			DefaultHttpClient httpClient = new DefaultHttpClient();

			// setup the call
			Uri.Builder builder = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(PATH);

			HttpGet get = new HttpGet( builder.build().toString() );
			get.setHeader("Content-type", "application/json");
			if( Tracker.DEBUG )
				Log.d(TAG,"Fetching engagement window from: " + get.toString() );
			
			// execute the call
			String result = null;
			try {
			    HttpResponse response = httpClient.execute(get);
			    result = EntityUtils.toString(response.getEntity());
				JSONObject jObject = new JSONObject(result);
				int ew = jObject.getInt("engagement_window") ;
				if( Tracker.DEBUG )
					Log.d(TAG,"Got engagement window: " + ew );
				setEngagementWindow( ew );
			} catch (Exception e) {
				//Something went wrong. Try again in 10 min.
			    timer.schedule(new EngagementWindowFetcher(), 10*60*1000);
			}
		}
	}
}
