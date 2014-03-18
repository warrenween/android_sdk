/**
 * (c) Chartbeat 2013
 */
package com.chartbeat.androidsdk;

import java.io.IOException;
import java.util.HashMap;
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
 * This class is the main entry point into the Chartbeat SDK.
 * 
 * Generally speaking, all applications need to implement the following
 * methods:
 * <ul>
 * <li> <strong>startTrackerWithAccountId():</strong> call one of these methods once
 * at application launch. If this is called multiple times, only the first call will have
 * any effect -- subsequent calls will be ignored.
 * <li> <strong>trackView():</strong> call this every time the view changes. This can be done by
 * calling this function in the onResume() function of your activity.
 * <li> <strong>userInteracted():</strong> call this every time the user interacts with the
 * application and the application, such as touching the screen. This can be done by calling
 * the function in onUserInteraction() function of your activity.
 * <li> <strong>userLeft():</strong> call this when the user leaves the app. This can be done
 * by calling the function in onUserLeaveHint() function of your activity.
 * </ul>
 * 
 * All other methods are optional.
 * 
 * 
 * @author bjorn
 */
public final class Tracker {
	private static final String USER_AGENT_SUFFIX = "/App";
	private static final long SDK_MAJOR_VERSION = 0;
	private static final long SDK_MINOR_VERSION = 0;
	private static final String SDK_VERSION_SUFFIX = "PRE";
	private static final String SDK_VERSION = SDK_MAJOR_VERSION + "." + SDK_MINOR_VERSION + "_" + SDK_VERSION_SUFFIX;
	private static final String TAG = "ChartBeat Tracker";
	private static Tracker singleton;
	private static final boolean DEBUG = true;
	
	private final String accountId, host, packageId, userAgent;
	private String viewId, viewTitle, token;
	private String appReferrer = "";
	private boolean allParameters;
	private String internalReferrer = null;
	private long timeCurrentViewStarted;
	private final int screenWidth;
	private int windowHeight;
	private final UserInfo userInfo;
	private final Timer timer;
	private final Pinger pinger;
	private final EngagementTracker engagementTracker;
	private final Context context;
	
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
		this.engagementTracker = new EngagementTracker(); //FIXME: get and set engagement window
		this.context = context;
		this.allParameters = true;
		
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
		this.internalReferrer = viewId;
		this.viewId = viewId;
		this.viewTitle = viewTitle;
		this.token = UUID.randomUUID().toString();
		this.timeCurrentViewStarted = System.currentTimeMillis();
		ForegroundTracker.activityStarted();
		timer.start();
		userInfo.visited();
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
		if( DEBUG )
			Log.d(TAG, this.accountId + ":" + this.packageId + ":" + this.host + " :: USER LEFT" );
	}
	void ping() {
		//if( viewId == null )
		//	return;
		
		boolean isInBackground = ForegroundTracker.isInBackground();
		
		// this is called by the timer on an arbitrary thread.
		long now = System.currentTimeMillis();
		
		HashMap<String,String> parameters = new HashMap<String,String>();
		parameters.put("h", host);
		parameters.put("d", packageId);
		parameters.put("p", viewId);
		parameters.put("t", token);
		parameters.put("u", userInfo.getUserId());
		parameters.put("g", accountId);
		if( appReferrer != null )
			parameters.put("r", appReferrer);
		else if( internalReferrer != null )
			parameters.put("v", internalReferrer);

		if( !isInBackground || allParameters ) {
			long timeInCurrentView = now - this.timeCurrentViewStarted;
			if( timeInCurrentView < 0 ) //could happen if time is adjusting
				timeInCurrentView = 0;
			// calculate time in minutes:
			double cd = timeInCurrentView / 1000.0; //seconds
			cd = cd/60; //minutes
			// print with one decimal precision:
			parameters.put("c", String.format( Locale.US, "%.1f", cd ));
		}
		int decay = timer.expectedNextSleep(isInBackground);
		if( decay != timer.getCurrentInterval() || allParameters )
			parameters.put("j", String.valueOf( timer.expectedNextSleep(isInBackground)*2) );
		// FIXME: need r/v
		if( allParameters ) { //or is the given parameter has changed!!!
			//FIXME: g0 - sections
			//FIXME: g1 - authors
			//FIXME: g2 - zones
			if( viewTitle != null )
				parameters.put("i", viewTitle);
			parameters.put("n", userInfo.isNewUser() ? "1" : "0" ); //FIXME: set 1 just the first time, or every time?
			
			parameters.put("f", userInfo.getUserVisitFrequencyString());
			// c- handled elsewhere
			//parameters.put("W", String.valueOf(screenWidth) );
			//parameters.put("w", String.valueOf(windowHeight) );
			// j - handled elsewhere
			// FIXME: D
			//FIXME: b, lg, lt
			parameters.put("V", SDK_VERSION);
		}
		
		// engagement keys
		EngagementData ed = engagementTracker.ping();
		parameters.put("E", String.valueOf(ed.totalEngagement));
		parameters.put("R", ed.reading ? "1" : "0" );
		parameters.put("W", ed.typed ? "1" : "0" );
		parameters.put("I", ed.idle ? "1" : "0" );
		
		//FIXME position keys
		
		if( DEBUG ) {
			Log.d(TAG, "PING! User Data: " + parameters );
			Log.d(TAG, "PING! User agent " + userAgent );
		}
		if( Pinger.isConnected(context) ) {
			boolean exception = false;
			int code = 503;
			try {
				code = pinger.ping(parameters);
			} catch (IOException e) {
				exception = true;
				Log.w(TAG, "Error pining chartbeat: " + e );
			}
			timer.changeTimeInterval(code);
			timer.isInBackground( isInBackground );
			if( code == 500 || exception ) {
				allParameters = true;
				engagementTracker.lastPingFailed(ed);
			}
			if( code == 200 ) {
				internalReferrer = null;
				appReferrer = null;
			}
		} else {
			if( DEBUG ) {
				Log.d(TAG, "Not pinging: no network connection detected." );
			}
		}
	}
	
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
	 * @param activity the context.
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
	 * @param viewId the id of the view being displayed.
	 * @param viewTitle the title of the view. may be null if not required.
	 */
	public static void trackView( String viewId, String viewTitle ) {
		if( viewId == null )
			throw new NullPointerException("viewId cannot be null");
		if( singleton == null )
			return;
		singleton.trackViewImpl( viewId, viewTitle );
	}
	/** Call this whenever the user leaves your app. If the tracker has not been initialized,
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
	 * Call this to set the app referrer. This should be called immediately before calling trackView.
	 * If the tracker has not been initialized,
	 * this call will be ignored.
	 * 
	 * @param appReferrer the string representing the appReferrer.
	 */
	public static void setAppReferrer( String appReferrer ) {
		if( singleton == null )
			return;
		singleton.appReferrer = appReferrer;
	}
}