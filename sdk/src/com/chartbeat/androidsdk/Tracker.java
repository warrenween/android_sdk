/**
 * Chartbeat Android API by Bjorn Roche.
 * (c) Chartbeat 2014
 */
package com.chartbeat.androidsdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import com.chartbeat.androidsdk.EngagementTracker.EngagementData;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
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
	} ;

	private static final String USER_AGENT_SUFFIX = "/App";
	private static final long SDK_MAJOR_VERSION = 0;
	private static final long SDK_MINOR_VERSION = 0;
	private static final String SDK_VERSION_SUFFIX = "PRE";
	private static final String SDK_VERSION = SDK_MAJOR_VERSION + "." + SDK_MINOR_VERSION + "_" + SDK_VERSION_SUFFIX;
	private static final String TAG = "ChartBeat Tracker";
	private static Tracker singleton;
	
	private final String accountId, host, packageId, userAgent;
	private String viewId, viewTitle, token;
	private String appReferrer = "";
	private PingMode pingMode;
	private String internalReferrer = null;
	private long timeCurrentViewStarted;
	private final int screenWidth;
	private int windowHeight;
	private final UserInfo userInfo;
	private final Timer timer;
	private final Pinger pinger;
	private final EngagementTracker engagementTracker;
	private final Context context;
	private int sequentialErrors; //counts 503 errors.
	
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
		this.engagementTracker = new EngagementTracker(); //FIXME: get engagement window from the server and set it.
		this.context = context;
		this.pingMode = PingMode.FIRST_PING;
		
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
		this.internalReferrer = this.viewId;
		this.viewId = viewId;
		this.viewTitle = viewTitle;
		this.token = UUID.randomUUID().toString();
		this.timeCurrentViewStarted = System.currentTimeMillis();
		ForegroundTracker.activityStarted();
		engagementTracker.userEnteredView();
		userInfo.visited();
		if( this.internalReferrer != null )
			userInfo.markUserAsOld();
		timer.start();
		timer.unsuspend();
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: TRACK VIEW :: " + this.viewId );
	}
	
	private void userInteractedImpl() {
		engagementTracker.userEngaged();
		userInfo.visited();
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: USER INTERACTED" );
	}
	private void userTypedImpl() {
		engagementTracker.userTyped();
		userInfo.visited();
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: USER TYPED" );
	}
	private void userLeftViewImpl(String viewId) {
		ForegroundTracker.activityEnded();
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
		addParameterIfRequired( parameters, "u", "User Id", userInfo.getUserId() );
		addParameterIfRequired( parameters, "g", "Account Id", accountId );
		
		if( appReferrer != null )
			addParameterIfRequired( parameters, "r", "External Referrer", appReferrer );
		else if( internalReferrer != null )
			addParameterIfRequired( parameters, "v", "Internal Referrer", internalReferrer );
		
		//FIXME: g0 - sections
		//FIXME: g1 - authors
		//FIXME: g2 - zones

		if( viewTitle != null )
			addParameterIfRequired( parameters, "i", "View Title", viewTitle);
		
		addParameterIfRequired( parameters, "n", "New User?", userInfo.isNewUser() ? "1" : "0");
		
		addParameterIfRequired( parameters, "f", "Visit Frequency", userInfo.getUserVisitFrequencyString());
		
		if( !isInBackground ) {
			long timeInCurrentView = now - this.timeCurrentViewStarted;
			if( timeInCurrentView < 0 ) //could happen if time is adjusting
				timeInCurrentView = 0;
			// calculate time in minutes:
			double cd = timeInCurrentView / 1000.0; //seconds
			cd = cd/60; //minutes
			// print with one decimal precision:
			addParameterIfRequired( parameters, "c", "Time on View (m)", String.format( Locale.US, "%.1f", cd ));
		}
		addParameterIfRequired( parameters, "W", "Device Width", String.valueOf(screenWidth) );
		addParameterIfRequired( parameters, "w", "Window Height", String.valueOf(windowHeight) );
		
		//although j is listed as "optional", when I only included it at the start and when it changed,
		// I got frequent, spurious 500's.
		//int decay = timer.expectedNextInterval(isInBackground);
		//if( decay != timer.getCurrentInterval() || allParameters )
		addParameterIfRequired( parameters, "j", "Decay", String.valueOf( timer.expectedNextInterval(isInBackground)*2) );
		
		// FIXME: D
		// FIXME: b
		// FIXME: lg/lt
		addParameterIfRequired( parameters, "V", "SDK Version", SDK_VERSION);
		
		// engagement keys
		EngagementData ed = engagementTracker.ping();
		parameters.add(new Pinger.KeyValuePair("E", "Engaged Seconds", String.valueOf(ed.totalEngagement)));
		parameters.add(new Pinger.KeyValuePair("R", "Reading", ed.reading ? "1" : "0" ));
		parameters.add(new Pinger.KeyValuePair("W", "Writing", ed.typed ? "1" : "0" ));
		parameters.add(new Pinger.KeyValuePair("I", "Idle", ed.idle ? "1" : "0" ));
		
		//FIXME position keys, x, y, w, o, m
		
		
		// last key must be an empty underscore
		parameters.add(new Pinger.KeyValuePair("_", "End Marker", ""));

		if( DEBUG ) {
			Log.d(TAG, "PING! User Data: " + parameters );
			Log.d(TAG, "PING! User agent " + userAgent );
		}
		if( Pinger.isConnected(context) ) {
			boolean exception = false;
			int code = 0;
			try {
				code = pinger.ping(parameters);
				if( DEBUG )
					Log.d(TAG, "ping returned with: " + code );
			} catch (IOException e) {
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
				pingMode = PingMode.FULL_PING;
				timer.suspend();
			}
			timer.isInBackground( isInBackground );
			if( code == 500 || exception || code == 400 ) {
				if( code == 500 ) {
					pingMode = PingMode.REPING_AFTER_500;
				} else {
					pingMode = PingMode.FULL_PING;
				}
				engagementTracker.lastPingFailed(ed);
				if( code == 400 || code == 500 ) {
					timer.retryImmediately();
				}
			}
			if( code == 200 ) {
				pingMode = PingMode.STANDARD_PING;
				internalReferrer = null;
				appReferrer = null;
			}
		} else {
			if( DEBUG ) {
				Log.d(TAG, "Not pinging: no network connection detected." );
			}
			pingMode = PingMode.FULL_PING;
			engagementTracker.lastPingFailed(ed);
		}
	}
	private void addParameterIfRequired( ArrayList<Pinger.KeyValuePair> parameters, String key, String note, String value ) {
		addParameterIfRequired( parameters, pingMode, key, note, value );
	}
	private void addParameterIfRequired( ArrayList<Pinger.KeyValuePair> parameters, PingMode pingMode, String key, String note, String value ) {
		if( pingMode.includeParameter(key) ) {
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
}