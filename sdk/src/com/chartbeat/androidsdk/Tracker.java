/**
 * Chartbeat Android API by Bjorn Roche.
 * (c) Chartbeat 2014
 */
package com.chartbeat.androidsdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import com.chartbeat.androidsdk.EngagementTracker.EngagementData;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebView;

/**
 * This class is the main entry point into the Chartbeat SDK. All Chartbeat
 * android SDK functionality is accessible through this class's public static methods.
 * 
 * Generally speaking, all applications <strong>must</strong> call the following
 * methods as part of their normal operation:
 * <ul>
 * <li> <strong>startTrackerWithAccountId():</strong> call one of these methods once
 * at application launch. If this is called multiple times, only the first call will have
 * any effect -- subsequent calls will be ignored.
 * <li> <strong>trackView():</strong> call this every time the view changes. This can be done by
 * calling this function in the onResume() function of your activity. This not only updates the
 * view, if necessary, but also ensures that the tracker knows when the app is in the foreground
 * and engaged.
 * <li> <strong>userLeft():</strong> call this when the user leaves the app. This can be done
 * by calling the function in onPause() function of your activity.
 * <li> <strong>userInteracted():</strong> call this every time the user interacts with the
 * application and the application, such as touching the screen. This can be done by calling
 * the function in onUserInteracted() function of your activity.
 * </ul>
 * 
 * All other methods are optional.
 * 
 * 
 * @author bjorn
 */
public final class Tracker {
	public static final boolean DEBUG = true;
	private static final long ONE_HOUR = 1 * 60 * 60 * 1000 ;

	private enum PingMode {
		FIRST_PING,
		STANDARD_PING,
		FULL_PING,
		REPING_AFTER_500;
		
		private static final HashSet<String> EVERY_TIME = new HashSet<String>();
		static {
			EVERY_TIME.add("h");
			EVERY_TIME.add("d");
			EVERY_TIME.add("p");
			EVERY_TIME.add("t");
			EVERY_TIME.add("u");
			EVERY_TIME.add("g");
			EVERY_TIME.add("v");
			EVERY_TIME.add("c");
			EVERY_TIME.add("j");
			EVERY_TIME.add("E");
			EVERY_TIME.add("R");
			EVERY_TIME.add("W");
			EVERY_TIME.add("I");
			EVERY_TIME.add("_");
		}
		
		boolean includeParameter( final String parameter ) {
			switch( this ) {
			case FIRST_PING:
			case FULL_PING:
				return true;
			case STANDARD_PING:
				return EVERY_TIME.contains(parameter);
			case REPING_AFTER_500:
				return !parameter.equals("D");
			default:
				throw new RuntimeException( "Invalid Ping Mode." );
			}
		}
		PingMode next() {
			switch( this ) {
			case FIRST_PING:
			case FULL_PING:
			case STANDARD_PING:
				return STANDARD_PING;
			case REPING_AFTER_500:
				return STANDARD_PING;
			default:
				throw new RuntimeException( "Invalid Ping Mode." );
			}
		}
	} ;
	
	private class PingParams {
		HashSet<String> oneTimeKeys = new HashSet<String>();
		PingMode pingMode;
		
		PingParams() {
			pingMode = PingMode.FIRST_PING;
		}
		void addOneTimeParameter( String k ) {
			oneTimeKeys.add(k);
		}
		void newView() {
			pingMode = PingMode.FULL_PING;
		}
		boolean includeParameter( final String parameter ) {
			return oneTimeKeys.contains(parameter) || pingMode.includeParameter(parameter);
		}
		void pingComplete(int code) {
			if( code == 500 ) {
				pingMode = PingMode.REPING_AFTER_500;
			} else if( code == 400 ) {
				pingMode = PingMode.FULL_PING;
			} else {
				pingMode = pingMode.next();
				oneTimeKeys.clear();
			}
		}
		void pingError() {
			pingMode = PingMode.FULL_PING;
		}
		void pingReset() {
			oneTimeKeys.clear();
			pingMode = PingMode.FULL_PING;
		}
	}

	private static final String USER_AGENT_SUFFIX = "/App";
	private static final long SDK_MAJOR_VERSION = 0;
	private static final long SDK_MINOR_VERSION = 0;
	private static final String SDK_VERSION_SUFFIX = "PRE";
	private static final String SDK_VERSION = SDK_MAJOR_VERSION + "." + SDK_MINOR_VERSION + "_" + SDK_VERSION_SUFFIX;
	private static final String TAG = "ChartBeat Tracker";
	private static Tracker singleton;
	
	private final String accountId, host, packageId, userAgent;
	private String viewId, viewTitle, token, priorToken;
	private String appReferrer = "";
	private PingParams pingParams;
	private String internalReferrer = null;
	private long timeCurrentViewStarted;
	private final int screenWidth;
	private int windowHeight;
	private final UserInfo userInfo;
	private final Timer timer;
	private final Pinger pinger;
	private final EngagementTracker engagementTracker;
	private final Context context;
	private long lastSuccessfulPingTime = 0;
	private long lastDecayTime = 0;
	private int sequentialErrors; //counts 503 errors.
	
	private String sections, authors, zones;
	private Float pageLoadTime;
	private Location location;
	private int x = -1, y = -1, w = -1, o = -1, m = -1;
		
	private Tracker( String accountId, String host, Context context ) {
		this.accountId = accountId;
		this.packageId = context.getPackageName();
		Point size = new Point();
		getScreenSize(context,size);
		screenWidth = size.x;
		windowHeight = size.y;
		
		this.userInfo = new UserInfo(context);
		if( host == null || host.length() == 0 ) {
			host = packageId;
		}
		this.host = host;
		this.viewId = null;
		this.timer = new Timer(this);
		//This takes about 100ms on older android phones
		this.userAgent = (new WebView(context)).getSettings().getUserAgentString() + USER_AGENT_SUFFIX;
		this.pinger = new Pinger(userAgent);
		this.engagementTracker = new EngagementTracker();
		this.context = context;
		this.pingParams = new PingParams();
		
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host );
	}
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private static void getScreenSize(Context context, Point size) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			display.getSize(size);
		} else {
			size.x = display.getWidth();
			size.y = display.getHeight();
		}
	}
	
	private void trackViewImpl( String viewId, String viewTitle ) {
		ForegroundTracker.activityStarted();
		timer.setInBackground( false );
		engagementTracker.userEnteredView();
		userInfo.visited();
		if( this.internalReferrer != null )
			userInfo.markUserAsOld();
		try {
			// are we hitting the same view again?
			if( this.viewId != null && this.viewId.equals( viewId ) )
				return;
			
			this.internalReferrer = this.viewId;
			this.viewId = viewId;
			this.viewTitle = viewTitle;
			this.priorToken = this.token;
			this.token = UUID.randomUUID().toString().replace("-", "");
			this.timeCurrentViewStarted = System.currentTimeMillis();
			pingParams.newView();
//			pingParams.addOneTimeParameter( "i" );
//			pingParams.addOneTimeParameter( "D" );
			resetViewSpecificData();
			if( DEBUG )
				Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: TRACK VIEW :: " + this.viewId );
		} finally {
			updateLocation();
			timer.start();
			timer.alive();
			timer.unsuspend();
		}
	}
	
	private void resetViewSpecificData() {
		sections = null;
		authors = null;
		zones = null;
		pageLoadTime = null;
		x = -1;
		y = -1;
		w = -1;
		o = -1;
		m = -1;
	}
	
	private void updateLocation() {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		Location lastKnownNetworkLocation = null, lastKnownGPSLocation = null;
		try {
			lastKnownNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		} catch( SecurityException se ) {
			Log.w(TAG, "Network location unavailable. Try requesting ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION");
		}
		try {
			lastKnownGPSLocation     = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} catch( SecurityException se ) {
			Log.w(TAG, "GPS location unavailable. Try requesting ACCESS_FINE_LOCATION" );
		}
		
//		Log.w(TAG, "GPS: " + lastKnownGPSLocation );
//		Log.w(TAG, "NETWORK: " + lastKnownNetworkLocation );
		
		// ignore any location info that's over an hour old.
		long now = System.currentTimeMillis();
		if( lastKnownNetworkLocation != null && lastKnownNetworkLocation.getTime() < now - ONE_HOUR ) {
			lastKnownNetworkLocation = null; //too old ignore it.
		}
		if( lastKnownGPSLocation != null && lastKnownGPSLocation.getTime() < now - ONE_HOUR ) {
			lastKnownGPSLocation = null; //too old ignore it.
		}
		
//		Log.w(TAG, "GPS: " + lastKnownGPSLocation );
//		Log.w(TAG, "NETWORK: " + lastKnownNetworkLocation );
		
		// now, try to pick the best, which is either the only, or the one that
		// reports the best accuracy:
		Location bestLocation = null;
		if( lastKnownNetworkLocation == null && lastKnownGPSLocation == null ) {
			bestLocation = null;
		} else if( lastKnownNetworkLocation == null ) {
			bestLocation = lastKnownGPSLocation;
		} else if( lastKnownGPSLocation == null ) {
			bestLocation = lastKnownNetworkLocation;
		} else {
			// both locations are non-null
			if( lastKnownGPSLocation.getAccuracy() < lastKnownNetworkLocation.getAccuracy() ) {
				bestLocation = lastKnownGPSLocation;
			} else {
				bestLocation = lastKnownNetworkLocation;
			}
		}
		
		// at this point, bestLocation is either null, or has reasonable data:
		location = bestLocation;
		
//		Log.w(TAG, "BEST: " + location );
		pingParams.addOneTimeParameter("lg");
		pingParams.addOneTimeParameter("lt");
	}
	
	private void userInteractedImpl() {
		engagementTracker.userEngaged();
		userInfo.visited();
		timer.alive();
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: USER INTERACTED" );
	}
	private void userTypedImpl() {
		engagementTracker.userTyped();
		userInfo.visited();
		timer.alive();
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: USER TYPED" );
	}
	private void userLeftViewImpl(String viewId) {
		ForegroundTracker.activityEnded();
		timer.setInBackground( true );
		engagementTracker.userLeftView();
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: USER LEFT" );
	}

	void ping() {
		//if( viewId == null )
		//	return;
		
		boolean isInBackground = ForegroundTracker.isInBackground();
		
		// this is called by the timer on an arbitrary thread.
		long now = System.currentTimeMillis();

		ArrayList<Pinger.KeyValuePair> parameters = new ArrayList<Pinger.KeyValuePair>(30);
		addParameterIfRequired( parameters, "h", "Host", host );
		addParameterIfRequired( parameters, "d", "Real Domain", packageId );
		addParameterIfRequired( parameters, "p", "Path", viewId );
		addParameterIfRequired( parameters, "t", "Token", token );
		addParameterIfRequired( parameters, "u", "User Id", userInfo.getShortUserId() );
		addParameterIfRequired( parameters, "g", "Account Id", accountId );
		
		if( appReferrer != null )
			addParameterIfRequired( parameters, "r", "External Referrer", appReferrer );
		else if( internalReferrer != null )
			addParameterIfRequired( parameters, "v", "Internal Referrer", internalReferrer );
		
		if( sections != null )
			addParameterIfRequired( parameters, "g0", "Sections", sections);
		if( authors != null )
			addParameterIfRequired( parameters, "g1", "Authors", authors);
		if( zones != null )
			addParameterIfRequired( parameters, "g2", "Zones", zones);

		if( viewTitle != null )
			addParameterIfRequired( parameters, "i", "View Title", viewTitle);
		
		addParameterIfRequired( parameters, "n", "New User?", userInfo.isNewUser() ? "1" : "0");
		
		addParameterIfRequired( parameters, "f", "Visit Frequency", userInfo.getUserVisitFrequencyString());
		
		{
			long timeInCurrentView = now - this.timeCurrentViewStarted;
			if( timeInCurrentView < 0 ) //could happen if time is adjusting
				timeInCurrentView = 0;
			// calculate time in minutes:
			double cd = timeInCurrentView / 1000.0; //seconds
			cd = cd/60; //minutes
			// print with one decimal precision:
			addParameterIfRequired( parameters, "c", "Time on View (m)", String.format( Locale.US, "%.1f", cd ));
		}
		addParameterIfRequired( parameters, "S", "Screen Width", String.valueOf(screenWidth) );
		addParameterIfRequired( parameters, "w", "Window Height", String.valueOf(windowHeight) );
		
		//although j is listed as "optional", when I only included it at the start and when it changed,
		// I got frequent, spurious 500's.
		//int decay = timer.expectedNextInterval(isInBackground);
		//if( decay != timer.getCurrentInterval() || allParameters )
		addParameterIfRequired( parameters, "j", "Decay", String.valueOf( timer.expectedNextInterval(isInBackground)*2) );
		
		// only include this if the j, decay time, has not passed since the last ping.
		if( priorToken != null && lastSuccessfulPingTime + lastDecayTime > System.currentTimeMillis() )
			addParameterIfRequired( parameters, "D", "Force Decay", priorToken );
		lastDecayTime = timer.expectedNextInterval(isInBackground) * 2 * 1000;
		
		if( pageLoadTime != null )
			addParameterIfRequired( parameters, "b", "Page Load Time", String.valueOf(pageLoadTime.floatValue()) );
		if( location != null ) {
			addParameterIfRequired( parameters, "lg", "Longitude", String.valueOf(location.getLongitude()) );
			addParameterIfRequired( parameters, "lt", "Latitude",  String.valueOf(location.getLatitude()) );
		}
		addParameterIfRequired( parameters, "V", "SDK Version", SDK_VERSION);
		
		// engagement keys
		EngagementData ed = engagementTracker.ping();
		parameters.add(new Pinger.KeyValuePair("E", "Engaged Seconds", String.valueOf(ed.totalEngagement)));
		parameters.add(new Pinger.KeyValuePair("R", "Reading", ed.reading ? "1" : "0" ));
		parameters.add(new Pinger.KeyValuePair("W", "Writing", ed.typed ? "1" : "0" ));
		parameters.add(new Pinger.KeyValuePair("I", "Idle", ed.idle ? "1" : "0" ));
		
		//position keys, x, y, w, o, m
		if( x != -1 )
			addParameterIfRequired( parameters, "x", "Scroll Position Top", String.valueOf(x));
		if( y != -1 )
			addParameterIfRequired( parameters, "y", "Scroll Window Height", String.valueOf(y));
		if( w != -1 )
			addParameterIfRequired( parameters, "w", "Height of currently viewable window", String.valueOf(w));
		if( o != -1 )
			addParameterIfRequired( parameters, "o", "Width of document fully rendered", String.valueOf(o));
		if( m != -1 )
			addParameterIfRequired( parameters, "m", "Max scroll depth durring session", String.valueOf(m));
		
		
		// last key must be an empty underscore
		parameters.add(new Pinger.KeyValuePair("_", "End Marker", ""));

		if( DEBUG ) {
			Log.d(TAG, "PING! User Data: " + parameters );
//			Log.d(TAG, "PING! User agent " + userAgent );
		}
		if( Pinger.isConnected(context) ) {
			boolean exception = false;
			int code = 0;
			try {
				code = pinger.ping(parameters);
				pingParams.pingComplete(code);
				if( DEBUG )
					Log.d(TAG, "ping returned with: " + code );
			} catch (IOException e) {
				pingParams.pingError();
				exception = true;
				Log.w(TAG, "Error pining chartbeat: " + e );
			}
			if( code == 503 ) {
				++sequentialErrors;
			} else {
				sequentialErrors = 0;
			}
			//System.out.println( sequentialErrors );
			if( sequentialErrors == 3 ) {
				sequentialErrors = 0;
				pingParams.pingError();
				timer.suspend();
			}
			timer.setInBackground( isInBackground );
			if( code == 500 || exception || code == 400 ) {
				engagementTracker.lastPingFailed(ed);
				if( code == 400 || code == 500 ) {
					timer.retryImmediately();
				}
			}
			if( code == 200 ) {
				internalReferrer = null;
				appReferrer = null;
				lastSuccessfulPingTime = System.currentTimeMillis();
			}
		} else {
			if( DEBUG ) {
				Log.d(TAG, "Not pinging: no network connection detected." );
			}
			pingParams.pingReset();
			engagementTracker.lastPingFailed(ed);
		}
	}
	private void addParameterIfRequired( ArrayList<Pinger.KeyValuePair> parameters, String key, String note, String value ) {
		addParameterIfRequired( parameters, pingParams, key, note, value );
	}
	private void addParameterIfRequired( ArrayList<Pinger.KeyValuePair> parameters, PingParams pingInfo, String key, String note, String value ) {
		if( pingInfo.includeParameter(key) ) {
			parameters.add(new Pinger.KeyValuePair(key, note, value));
		}
	}
	
	
	/** ----------- Public static functions -------------- */

	/** initializes the tracker. If the tracker has already been initialized, this call will be ignored.
	 * 
	 * @param accountId your account id on the Chartbeat system.
	 * @param suffix if not null and not an empty string, this is alternative package name.
	 * @param context the context.
	 */
	public static void startTrackerWithAccountId( String accountId, String suffix, Context context ) {
		if( accountId == null )
			throw new NullPointerException("viewId cannot be null");
		if( context == null )
			throw new NullPointerException("activity cannot be null");
		if( singleton != null )
			return;
		singleton = new Tracker(accountId,suffix,context);
	}
	/** initializes the tracker. If the tracker has already been initialized, this call will be ignored.
	 * 
	 * @param accountId your account id on the Chartbeat system.
	 * @param context the context.
	 */
	public static void startTrackerWithAccountId( String accountId, Context context ) {
		if( accountId == null )
			throw new NullPointerException("viewId cannot be null");
		if( context == null )
			throw new NullPointerException("activity cannot be null");
		if( singleton != null )
			return;
		singleton = new Tracker(accountId,null,context);
	}
	/** Stops the tracker if one has been previously started.
	 * Most apps will not need to call this function.
	 */
	public static void stopTracker() {
		if( singleton != null ) {
			singleton.timer.stop();
			singleton.engagementTracker.stop();
			singleton = null;
		}
	}
	
	/** Call this whenever you display a new view. If the tracker has not been initialized,
	 * this call will be ignored.
	 * 
	 * @param viewId the id of the view being displayed. Must not be null.
	 * @param viewTitle the title of the view. may be null.
	 */
	public static void trackView( String viewId, String viewTitle ) {
		if( viewId == null )
			throw new NullPointerException("viewId cannot be null");
		if( singleton == null )
			return;
		singleton.trackViewImpl( viewId, viewTitle );
	}
	/** Call this whenever the user leaves an activity. This will be used as a hint that the user might
	 * have left the app. If the tracker has not been initialized,
	 * this call will be ignored.
	 */
	public static void userLeftView( String viewId ) {
		if( singleton == null )
			return;
		singleton.userLeftViewImpl( viewId );
	}
	/** Call this whenever the user interacts with your app. If the tracker has not been initialized,
	 * this call will be ignored.
	 */
	public static void userInteracted() {
		if( singleton == null )
			return;
		singleton.userInteractedImpl();
	}
	/** Call this whenever the user is writing/typing. If the tracker has not been initialized,
	 * this call will be ignored.
	 */
	public static void userTyped() {
		if( singleton == null )
			return;
		singleton.userTypedImpl();
	}
	/**
	 * Call this to set the app referrer. This is a referrer that is external to the app, such as
	 * another app or website.
	 * This should be called immediately before calling trackView.
	 * If the tracker has not been initialized,
	 * this call will be ignored.
	 * 
	 * @param appReferrer the string representing the appReferrer.
	 */
	public static void setAppReferrer( String appReferrer ) {
		if( singleton == null )
			return;
		//FIXME: should we do something to prevent issues if the user calls this function after the first ping?
		// we could either ignore it, throw and exception, or force the app to reping with all parameters.
		singleton.appReferrer = appReferrer;
	}
	
	/**
	 * Call this method to set the zone(s) for the current view. This data will be purged when changing the view,
	 * so be sure not to call this before calling trackView().
	 * @param zones a comma-delimited list of zones.
	 */
	public static void setZones( String zones ) {
		if( singleton == null )
			return;
		singleton.zones = zones;
		singleton.pingParams.addOneTimeParameter("g2");
		singleton.timer.alive();
	}
	/**
	 * Call this method to set the zone(s) for the current view. Note that any commas found in the zone strings will be
	 * removed because that is the delimiter.
	 * 
	 * @param zones
	 */
	public static void setZones( Collection<String> zones ) {
		if( singleton == null )
			return;
		singleton.zones = collectionToCommaString(zones);
		singleton.pingParams.addOneTimeParameter("g2");
		singleton.timer.alive();
	}
	
	/**
	 * Call this method to set the author(s) for the current view. This data will be purged when changing the view,
	 * so be sure not to call this before calling trackView().
	 * @param zones a comma-delimited list of zones.
	 */
	public static void setAuthors( String authors ) {
		if( singleton == null )
			return;
		singleton.authors = authors;
		singleton.pingParams.addOneTimeParameter("g1");
		singleton.timer.alive();
	}
	/**
	 * Call this method to set the authors(s) for the current view. Note that any commas found in the author strings will be
	 * removed because that is the delimiter.
	 * 
	 * @param authors
	 */
	public static void setAuthors( Collection<String> authors ) {
		if( singleton == null )
			return;
		singleton.authors = collectionToCommaString(authors);
		singleton.pingParams.addOneTimeParameter("g1");
		singleton.timer.alive();
	}
	
	/**
	 * Call this method to set the section(s) for the current view. This data will be purged when changing the view,
	 * so be sure not to call this before calling trackView().
	 * @param sections a comma-delimited list of sections.
	 */
	public static void setSections( String sections ) {
		if( singleton == null )
			return;
		singleton.sections = sections;
		singleton.pingParams.addOneTimeParameter("g0");
		singleton.timer.alive();
	}
	/**
	 * Call this method to set the sections(s) for the current view. Note that any commas found in the section strings will be
	 * removed because that is the delimiter.
	 * 
	 * @param authors
	 */
	public static void setSections( Collection<String> sections ) {
		if( singleton == null )
			return;
		singleton.sections = collectionToCommaString(sections);
		singleton.pingParams.addOneTimeParameter("g0");
		singleton.timer.alive();
	}
	/** call this to set the load time of the current page/view. This data will be purged when changing the view,
	 * so be sure not to call this before calling trackView().
	 * */
	public static void setViewLoadTime( float pageLoadTime ) {
		if( singleton == null )
			return;
		singleton.pageLoadTime = pageLoadTime;
		singleton.pingParams.addOneTimeParameter("b");
		singleton.timer.alive();
	}
	/** sets the position of the current view, assuming it scrolls. If it does not scroll, don't call this function. Negative values will
	 * not be passed to the server.
	 * 
	 * @param x Scroll Position Top
	 * @param y Scroll Window Height
	 * @param w Height of the currently viewable window
	 * @param o Width of the document fully rendered
	 * @param m Max scroll depth during the session
	 */
	public static void setPosition( int x, int y, int w, int o ) {
		if( singleton == null )
			return;
		singleton.x = x;
		singleton.y = y;
		singleton.w = w;
		singleton.o = o;
		singleton.m = Math.max(singleton.m,x);
		singleton.pingParams.addOneTimeParameter("x");
		singleton.pingParams.addOneTimeParameter("y");
		singleton.pingParams.addOneTimeParameter("w");
		singleton.pingParams.addOneTimeParameter("o");
		singleton.pingParams.addOneTimeParameter("m");
		singleton.timer.alive();
	}
	
	
	private static String collectionToCommaString( Collection<String> col ) {
		if( col == null || col.size() == 0 ) {
			return null;
		}
		// there shouldn't usually be too many elements in our collection,
		// so not using a string builder is probably appropriate here.
		String ret = "";
		int i = 0;
		for( String s : col ) {
			ret += s.replaceAll(",","");
			++i;
			if( i != col.size() )
				ret += ",";
		}
		return ret;
	}
}