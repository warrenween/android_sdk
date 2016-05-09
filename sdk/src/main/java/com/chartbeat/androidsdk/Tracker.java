/**
 * Chartbeat Android API by Mike Dai Wang.
 * (c) Chartbeat 2016
 */
package com.chartbeat.androidsdk;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;

/**
 * This class is the main entry point into the Chartbeat SDK. All Chartbeat
 * android SDK functionality is accessible through this class's public static
 * methods.
 * 
 * Generally speaking, all applications <strong>must</strong> call the following
 * methods as part of their normal operation:
 * <ul>
 * <li><strong>startTrackerWithAccountId():</strong> call one of these methods
 * once at application launch. If this is called multiple times, only the first
 * call will have any effect -- subsequent calls will be ignored.
 * <li><strong>trackView():</strong> call one of these methods every time the view changes. This
 * can be done by calling this function in the onResume() function of your
 * activity. This not only updates the view, if necessary, but also ensures that
 * the tracker knows when the app is in the foreground and engaged.
 * <li><strong>userLeftView():</strong> call this when the user leaves the view. This
 * can be done by calling the function in the onPause() function of your activity.
 * <li><strong>userInteracted():</strong> call this every time the user
 * interacts with the application and the application, such as touching the
 * screen. This can be done by calling the function in onUserInteracted()
 * function of your activity.
 * </ul>
 * 
 * All other methods are optional, and allow you to, for example, track scroll position,
 * authors, when the user types and so on.
 * 
 * 
 * @author bjorn
 * @author Mike Dai Wang
 */
public final class Tracker {
    private static final String TAG = "ChartBeat Tracker";

    public static boolean DEBUG_MODE = false;

    private static Context appContext;
    private static String accountID;

    static final String KEY_SDK_ACTION_TYPE = "KEY_SDK_ACTION_TYPE";

    static final String ACTION_INIT_TRACKER = "ACTION_INIT_TRACKER";
    static final String ACTION_SET_APP_REFERRER = "ACTION_SET_APP_REFERRER";
    static final String ACTION_STOP_TRACKER = "ACTION_STOP_TRACKER";
    static final String ACTION_TRACK_VIEW = "ACTION_TRACK_VIEW";
    static final String ACTION_LEFT_VIEW = "ACTION_LEFT_VIEW";
    static final String ACTION_USER_INTERACTED = "ACTION_USER_INTERACTED";
    static final String ACTION_USER_TYPED = "ACTION_USER_TYPED";
    static final String ACTION_SET_ZONES = "ACTION_SET_ZONES";
    static final String ACTION_SET_AUTHORS = "ACTION_SET_AUTHORS";
    static final String ACTION_SET_SECTIONS = "ACTION_SET_SECTIONS";
    static final String ACTION_SET_VIEW_LOADING_TIME = "ACTION_SET_VIEW_LOADING_TIME";
    static final String ACTION_SET_POSITION = "ACTION_SET_POSITION";

    static final String KEY_ACCOUNT_ID = "KEY_ACCOUNT_ID";
    static final String KEY_CUSTOM_HOST = "KEY_CUSTOM_HOST";
    static final String KEY_APP_REFERRER = "KEY_APP_REFERRER";
    static final String KEY_VIEW_ID = "KEY_VIEW_ID";
    static final String KEY_VIEW_TITLE = "KEY_VIEW_TITLE";
    static final String KEY_ZONES = "KEY_ZONES";
    static final String KEY_AUTHORS = "KEY_AUTHORS";
    static final String KEY_SECTIONS = "KEY_SECTIONS";
    static final String KEY_VIEW_LOADING_TIME = "KEY_VIEW_LOADING_TIME";

    static final String KEY_POSITION_TOP = "KEY_POSITION_TOP";
    static final String KEY_WINDOW_HEIGHT = "KEY_WINDOW_HEIGHT";
    static final String KEY_CONTENT_HEIGHT = "KEY_CONTENT_HEIGHT";
    static final String KEY_DOC_WIDTH = "KEY_DOC_WIDTH";


    /** ----------- Public static functions -------------- */

    /**
     * initializes the tracker. If the tracker has already been initialized,
     * this call will be ignored.
     *
     * @param accountId
     *            your account id on the Chartbeat system.
     * @param suffix
     *            if not null and not an empty string, this is alternative
     *            package name.
     * @param context
     *            the context.
     */
    public static void startTrackerWithAccountId(String accountId, String suffix, Context context) {
        if (accountId == null) {
            throw new NullPointerException("accountId cannot be null");
        }

        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }

        startSDK(accountId, suffix, context);
    }

    /**
     * initializes the tracker. If the tracker has already been initialized,
     * this call will be ignored.
     *
     * @param accountId
     *            your account id on the Chartbeat system.
     * @param context
     *            the context.
     */
    public static void startTrackerWithAccountId(String accountId, Context context) {
        if (accountId == null) {
            throw new NullPointerException("accountId cannot be null");
        }

        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }

        startSDK(accountId, null, context);
    }

    private static void startSDK(String accountID, String customHost, Context context) {
        appContext = context.getApplicationContext();
        Tracker.accountID = accountID;

        Intent intent = new Intent(context.getApplicationContext(), ChartbeatService.class);
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_INIT_TRACKER);
        intent.putExtra(KEY_ACCOUNT_ID, accountID);
        if (customHost != null) {
            intent.putExtra(KEY_CUSTOM_HOST, customHost);
        }

        appContext.startService(intent);
    }

    /**
     * Call this to set the app referrer. This is a referrer that is external to
     * the app, such as another app or website. This should be called
     * immediately before calling trackView. If the tracker has not been
     * initialized, this call will be ignored.
     *
     * @param appReferrer
     *            the string representing the appReferrer.
     */
    public static void setAppReferrer(String appReferrer) {
        didInit();

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_SET_APP_REFERRER);
        intent.putExtra(KEY_APP_REFERRER, appReferrer);
        appContext.startService(intent);
    }

    /**
     * Stops the tracker if one has been previously started. Most apps will not
     * need to call this function.
     */
    public static void stopTracker() {
        if (appContext == null) {
            return;
        }

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_STOP_TRACKER);

        appContext.startService(intent);
    }

    /**
     * Call this whenever you display a new view. Use this in views where you are not tracking position.
     *  If the tracker has not been initialized, this call will be ignored.
     *
     * @param context
     *            the context of the current view being tracked
     * 
     * @param viewId
     *            the id of the view being displayed. Must not be null.
     * @param viewTitle
     *            the title of the view. may be null.
     */
    public static void trackView(Context context, String viewId, String viewTitle) {
        didInit();

        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }

        if (viewId == null) {
            throw new NullPointerException("viewId cannot be null");
        }

        if (viewTitle == null) {
            viewTitle = viewId;
        }
        
        appContext = context.getApplicationContext();
        
        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_TRACK_VIEW);
        intent.putExtra(KEY_VIEW_ID, viewId);
        intent.putExtra(KEY_VIEW_TITLE, viewId);
        appContext.startService(intent);
    }

    /**
     * Call this whenever you display a new view. Use this in views where you are tracking position.
     *  If the tracker has not been
     * initialized, this call will be ignored.
     *
     * @param context
     *            the context of the current view being tracked
     *            
     * @param viewId
     *            the id of the view being displayed. Must not be null.
     * @param viewTitle
     *            the title of the view. may be null.
     * @param scrollPositionTop
     *            Scroll Position Top
     * @param scrollWindowHeight
     *            Scroll Window Height
     * @param totalContentHeight
     *            Total Content Height
     * @param fullyRenderedDocWidth
     *            Width of the document fully rendered
     */
    public static void trackView(Context context, String viewId, String viewTitle,
                                 int scrollPositionTop, int scrollWindowHeight,
                                 int totalContentHeight, int fullyRenderedDocWidth) {
        didInit();

        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }

        if (viewId == null) {
            throw new NullPointerException("viewId cannot be null");
        }

        if (viewTitle == null) {
            viewTitle = viewId;
        }

        appContext = context.getApplicationContext();

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_TRACK_VIEW);
        intent.putExtra(KEY_VIEW_ID, viewId);
        intent.putExtra(KEY_VIEW_TITLE, viewId);
        intent.putExtra(KEY_POSITION_TOP, scrollPositionTop);
        intent.putExtra(KEY_WINDOW_HEIGHT, scrollWindowHeight);
        intent.putExtra(KEY_CONTENT_HEIGHT, totalContentHeight);
        intent.putExtra(KEY_DOC_WIDTH, fullyRenderedDocWidth);

        appContext.startService(intent);
    }

    /**
     * Call this whenever the user leaves an activity. This will be used as a
     * hint that the user might have left the app. If the tracker has not been
     * initialized, this call will be ignored.
     */
    public static void userLeftView(String viewId) {
        didInit();
        didStartTracking();

        if (viewId == null) {
            throw new NullPointerException("viewId cannot be null");
        }

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_LEFT_VIEW);
        intent.putExtra(KEY_VIEW_ID, viewId);

        appContext.startService(intent);
    }

    /**
     * Call this whenever the user interacts with your app. If the tracker has
     * not been initialized, this call will be ignored. You will likely want to
     * put this in your onUserInteraction() function of your activity.
     */
    public static void userInteracted() {
        didInit();
        didStartTracking();

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_USER_INTERACTED);

        appContext.startService(intent);
    }

    /**
     * Call this whenever the user is writing/typing. If the tracker has not
     * been initialized, this call will be ignored.
     */
    public static void userTyped() {
        didInit();
        didStartTracking();

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_USER_TYPED);

        appContext.startService(intent);
    }

    /**
     * Call this method to set the zone(s) for the current view. This data will
     * be purged when changing the view, so be sure to call this after
     * calling trackView().
     *
     * @param zones
     *            a comma-delimited list of zones.
     */
    public static void setZones(String zones) {
        didInit();
        didStartTracking();

        setZonesImpl(zones);
    }

    /**
     * Call this method to set the zone(s) for the current view. Note that any
     * commas found in the zone strings will be removed because that is the
     * delimiter.
     *
     * @param zones
     */
    public static void setZones(Collection<String> zones) {
        didInit();
        didStartTracking();

        setZonesImpl(StringUtils.collectionToCommaString(zones));
    }

    private static void setZonesImpl(String zones) {
        didInit();

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_SET_ZONES);
        intent.putExtra(KEY_ZONES, zones);
        appContext.startService(intent);
    }

    /**
     * Call this method to set the author(s) for the current view. This data
     * will be purged when changing the view, so be sure to call this after
     * calling trackView().
     *
     * @param authors
     *            a comma-delimited list of authors.
     */
    public static void setAuthors(String authors) {
        didInit();
        didStartTracking();

        setAuthorsImpl(authors);
    }

    /**
     * Call this method to set the authors(s) for the current view. Note that
     * any commas found in the author strings will be removed because that is
     * the delimiter.
     *
     * @param authors
     */
    public static void setAuthors(Collection<String> authors) {
        didInit();
        didStartTracking();

        setAuthorsImpl(StringUtils.collectionToCommaString(authors));
    }

    private static void setAuthorsImpl(String authors) {
        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_SET_AUTHORS);
        intent.putExtra(KEY_AUTHORS, authors);
        appContext.startService(intent);
    }

    /**
     * Call this method to set the section(s) for the current view. This data
     * will be purged when changing the view, so be sure to call this after
     * calling trackView().
     *
     * @param sections
     *            a comma-delimited list of sections.
     */
    public static void setSections(String sections) {
        didInit();
        didStartTracking();

        setSectionsImpl(sections);
    }

    /**
     * Call this method to set the sections(s) for the current view. Note that
     * any commas found in the section strings will be removed because that is
     * the delimiter.
     *
     * @param sections
     */
    public static void setSections(Collection<String> sections) {
        didInit();
        didStartTracking();

        setSectionsImpl(StringUtils.collectionToCommaString(sections));
    }

    private static void setSectionsImpl(String sections) {
        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_SET_SECTIONS);
        intent.putExtra(KEY_SECTIONS, sections);
        appContext.startService(intent);
    }

    /**
     * Call this to set the load time of the current page/view. This data will
     * be purged when changing the view, so be sure to call this after
     * calling trackView().
     * */
    public static void setViewLoadTime(float pageLoadTime) {
        didInit();
        didStartTracking();

        if (pageLoadTime < 0.0f) {
            Logger.e(TAG, "Page load time cannot be negative");
            return;
        }

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_SET_VIEW_LOADING_TIME);
        intent.putExtra(KEY_VIEW_LOADING_TIME, pageLoadTime);
        appContext.startService(intent);
    }

    /**
     * sets the position of the current view, assuming it scrolls. If it does
     * not scroll, don't call this function. Negative values will not be passed
     * to the server.
     *
     * @param scrollPositionTop
     *            Scroll Position Top
     * @param scrollWindowHeight
     *            Scroll Window Height
     * @param totalContentHeight
     *            Total Content Height
     * @param fullyRenderedDocWidth
     *            Width of the document fully rendered
     */
    public static void setPosition(int scrollPositionTop, int scrollWindowHeight, int totalContentHeight, int fullyRenderedDocWidth) {
        didInit();
        didStartTracking();

        Intent intent = new Intent(appContext, ChartbeatService.class);
        
        intent.putExtra(KEY_SDK_ACTION_TYPE, ACTION_SET_POSITION);

        intent.putExtra(KEY_POSITION_TOP, scrollPositionTop);
        intent.putExtra(KEY_WINDOW_HEIGHT, scrollWindowHeight);
        intent.putExtra(KEY_CONTENT_HEIGHT, totalContentHeight);
        intent.putExtra(KEY_DOC_WIDTH, fullyRenderedDocWidth);
        appContext.startService(intent);
    }

    public static void didInit() {
        if (appContext == null && TextUtils.isEmpty(accountID)) {
            throw new RuntimeException("Chartbeat: SDK has not been initialized");
        }
    }

    public static void didStartTracking() {
        if (appContext == null) {
            throw new RuntimeException("Chartbeat: View tracking hasn't started, please call Tracker.trackView() in onResume() first");
        }
    }
}