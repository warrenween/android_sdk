package com.chartbeat.androidsdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

import rx.Observer;

/**
 * Created by Mike Dai Wang on 2016-02-05.
 */
final class ChartBeatTracker {
    private static final String TAG = ChartBeatTracker.class.getSimpleName();
    public static final String CHARTBEAT_PREFS = "com.chartbeat.androidsdk.user";

    private static final long MILLISECONDS_IN_ONE_SECOND = 1000;

    private static Handler handler;
    private static boolean firstPing = true;

    private static PingService pingService;
    private static LocationService locationService;

    private final WeakReference<Context> context;
    private final AppInfo appInfo;
    private final UserInfo userInfo;
    private final EngagementTracker engagementTracker;

    private String previousToken;
    private ViewTracker currentViewTracker;

    private PingParams pingParams;
    
	public static boolean DEBUG = true;
	/** set to true to simulate a very long response to pings (10 seconds) */
	public static final boolean SIMULATE_VERY_SLOW_SERVER = false;

	private final PingManager pingManager;
	private long lastSuccessfulPingTime = 0;
	private long lastDecayTime = 0;
	private int sequentialErrors; // counts 503 errors.

    ChartBeatTracker(WeakReference<Context> context, String accountID, String customHost, String userAgent, Looper looper) {
        this.context = context;

        if (handler == null) {
            handler = new Handler(looper);
        }

        pingService = new PingService(userAgent);

        appInfo = new AppInfo(context.get(), accountID, customHost);
        locationService = new LocationService();
        this.userInfo = new UserInfo(context.get());

		this.currentViewTracker = null;
		this.pingManager = new PingManager(this, looper);

		this.engagementTracker = new EngagementTracker();

		this.pingParams = new PingParams();

		if (DEBUG) {
            Log.d(TAG, appInfo.toString());
        }
	}
    
    synchronized void stopTracker() {
        pingManager.stop();
        engagementTracker.stop();
    }
    
    synchronized void setExternalReferrer(String appReferrer) {
        appInfo.setExternalReferrer(appReferrer);
    }

    synchronized boolean isNotTrackingAnyView() {
        return currentViewTracker == null;
    }

    synchronized void trackViewImpl(final String viewId, final String viewTitle,
                                    final int x, final int w, final int y, final int o) {
        ForegroundTracker.activityStarted();
        engagementTracker.userEnteredView();
        userInfo.visited();

        if (currentViewTracker == null || !currentViewTracker.isSameView(viewId)) {
            trackNewView(viewId, viewTitle, x, w, y, o);
        } else {
            if (userInfo.isNewUser() && currentViewTracker.wasReferredFromAnotherView()) {
                userInfo.markUserAsOld();
            }
            pingManager.alive();
        }
	}

    private void trackNewView(String viewId, String viewTitle, int x, int w, int y, int o) {
        String internalReferral = "";

        if (currentViewTracker != null) {
            internalReferral = currentViewTracker.getViewID();
            previousToken = currentViewTracker.getToken();
        }

        String generatedToken = SecurityUtils.randomChars(28);
        ViewDimension viewDimension = new ViewDimension(x, w, y, o, x);

        currentViewTracker = new ViewTracker(viewId, viewTitle, internalReferral, generatedToken, viewDimension);
        pingParams.newView();

        if (DEBUG) {
            Log.d(TAG, appInfo.toString() + " :: TRACK VIEW :: " + viewId);
        }

        this.pingParams.addOneTimeParameter(QueryKeys.SCROLL_POSITION_TOP);
        this.pingParams.addOneTimeParameter(QueryKeys.CONTENT_HEIGHT);
        this.pingParams.addOneTimeParameter(QueryKeys.SCROLL_WINDOW_HEIGHT);
        this.pingParams.addOneTimeParameter(QueryKeys.DOCUMENT_WIDTH);
        this.pingParams.addOneTimeParameter(QueryKeys.MAX_SCROLL_DEPTH);

        updateLocation();

        pingManager.restart();
    }

	private synchronized void updateLocation() {
        locationService.updateLocation(context.get());

		// Log.w(TAG, "BEST: " + location );
		pingParams.addOneTimeParameter(QueryKeys.LONGITUDE);
		pingParams.addOneTimeParameter(QueryKeys.LATITUDE);
	}

    synchronized void userInteractedImpl() {
        engagementTracker.userEngaged();
        userInfo.visited();
        pingManager.alive();

        if (DEBUG) {
            Log.d(TAG, appInfo.toString() + " :: USER INTERACTED");
        }
	}

    synchronized void userTypedImpl() {
        engagementTracker.userTyped();
        userInfo.visited();
        pingManager.alive();
        if (DEBUG) {
            Log.d(TAG, appInfo.toString() + " :: USER TYPED");
        }
	}

    synchronized void userLeftViewImpl(String viewId) {
        ForegroundTracker.activityEnded();
        pingManager.setInBackground(true);
        engagementTracker.userLeftView();
        if (DEBUG) {
            Log.d(TAG, appInfo.toString() + " :: USER LEFT");
        }
	}

    synchronized void updateViewDimensions(final int scrollPositionTop,
                                           final int scrollWindowHeight,
                                           final int totalContentHeight,
                                           final int fullyRenderedDocWidth) {
        currentViewTracker.updateDimension(scrollPositionTop,
                scrollWindowHeight,
                totalContentHeight,
                fullyRenderedDocWidth);

        pingParams.addOneTimeParameter(QueryKeys.SCROLL_POSITION_TOP);
        pingParams.addOneTimeParameter(QueryKeys.CONTENT_HEIGHT);
        pingParams.addOneTimeParameter(QueryKeys.SCROLL_WINDOW_HEIGHT);
        pingParams.addOneTimeParameter(QueryKeys.DOCUMENT_WIDTH);
        pingParams.addOneTimeParameter(QueryKeys.MAX_SCROLL_DEPTH);
        pingManager.alive();
    }

    synchronized void updateZones(final String zones) {
        currentViewTracker.updateZones(zones);
        pingParams.addOneTimeParameter(QueryKeys.ZONE_G2);
        pingManager.alive();
    }

    synchronized void updateSections(final String sections) {
        currentViewTracker.updateSections(sections);
        pingParams.addOneTimeParameter(QueryKeys.SECTION_G0);
        pingManager.alive();
    }

    synchronized void updateAuthors(final String authors) {
        currentViewTracker.updateAuthors(authors);
        pingParams.addOneTimeParameter(QueryKeys.AUTHOR_G1);
        pingManager.alive();
    }

    synchronized void updatePageLoadingTime(final float pageLoadTime) {
        currentViewTracker.updatePageLoadingTime(pageLoadTime);
        pingParams.addOneTimeParameter(QueryKeys.PAGE_LOAD_TIME);
        pingManager.alive();
    }
    

	void ping(boolean needsFullPingHint, String decay) {
		LinkedHashMap<String, String> parameters = new LinkedHashMap<>(30);
		final EngagementTracker.EngagementData engagementData;

		// setup parameters in a synchronized block:
		synchronized (this) {
			if (currentViewTracker == null)
				return;

			// decrease the likelihood of getting a 500 back
			if (needsFullPingHint) {
                pingParams.pingReset();
            }

            if (appInfo != null) {
                addParametersIfRequired(parameters, appInfo.toPingParams());

                if (firstPing) {
                    addParameterIfRequired(parameters, QueryKeys.EXTERNAL_REFERRER, appInfo.getExternalReferrer());
                }
            }

            if (currentViewTracker != null) {
                addParametersIfRequired(parameters, currentViewTracker.toPingParams());
            }

            if (userInfo != null) {
                addParametersIfRequired(parameters, userInfo.toPingParams());
            }

            if (locationService != null) {
                addParametersIfRequired(parameters, locationService.toPingParams());
            }

			addParameterIfRequired(parameters, QueryKeys.DECAY, decay);

			if (previousToken != null && lastSuccessfulPingTime + lastDecayTime > System.currentTimeMillis()) {
                addParameterIfRequired(parameters, QueryKeys.FORCE_DECAY, previousToken);
            }

			lastDecayTime = pingManager.expectedNextIntervalInSeconds() * 2 * MILLISECONDS_IN_ONE_SECOND;

			// engagement keys
			engagementData = engagementTracker.getEngagementSnapshot();
			parameters.put(QueryKeys.ENGAGED_SECONDS, String.valueOf(engagementData.totalEngagement));
			parameters.put(QueryKeys.READING, engagementData.reading ? "1" : "0");
			parameters.put(QueryKeys.WRITING, engagementData.typed ? "1" : "0");
			parameters.put(QueryKeys.IDLING, engagementData.idle ? "1" : "0");

			// last key must be an empty underscore
			parameters.put(QueryKeys.END_MARKER, "");

			if (DEBUG) {
				Log.d(TAG, "PING! User Data: " + parameters);
			}
        }
		// out of synchronized block, do the actual ping:
		if (SystemUtils.isNetworkAvailable(context.get())) {
            pingService.ping(parameters)
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(final Throwable e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    handlePingError(e.getLocalizedMessage(), engagementData);
                                }
                            });
                        }

                        @Override
                        public void onNext(final Integer code) {
                            if (SIMULATE_VERY_SLOW_SERVER) {
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException io) {
                                }
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    handlePingResponseCode(code, engagementData);
                                }
                            });
                        }
                    });
		} else {
			synchronized( this ) {
				if (DEBUG) {
					Log.d(TAG, "Not pinging: no network connection detected.");
				}
				pingParams.pingReset();
				engagementTracker.lastPingFailed(engagementData);
			}
		}
	}

    private void handlePingResponseCode(int code, EngagementTracker.EngagementData engagementData) {
        synchronized( this ) {
            final boolean isInBackground = ForegroundTracker.isInBackground();
            pingParams.pingComplete(code);
            if (code == 503) {
                ++sequentialErrors;
            } else {
                sequentialErrors = 0;
            }
            // System.out.println( sequentialErrors );
            if (sequentialErrors == 3) {
                sequentialErrors = 0;
                pingParams.pingError();
                pingManager.suspendDueToServerBusy();
            }
            pingManager.setInBackground(isInBackground);
            if (code == 500 || code == 400) {
                engagementTracker.lastPingFailed(engagementData);
                pingManager.retryImmediately();
            }
            if (code == 200) {
                lastSuccessfulPingTime = System.currentTimeMillis();
                if (firstPing) {
                    firstPing = false;
                }
            }
        }
    }

    private void handlePingError(String errorMessage, EngagementTracker.EngagementData engagementData) {
        pingParams.pingError();
        Log.w(TAG, "Error pinging Chartbeat: " + errorMessage);
        engagementTracker.lastPingFailed(engagementData);
    }

    private synchronized void addParametersIfRequired(LinkedHashMap<String, String> parameters, LinkedHashMap<String, String> additionalParameters) {
        for (String key : additionalParameters.keySet()) {
            addParameterIfRequired(parameters, key, additionalParameters.get(key));
        }
    }

	private synchronized void addParameterIfRequired(LinkedHashMap<String, String> parameters, String key, String value) {
		addParameterIfRequired(parameters, pingParams, key, value);
	}

	private synchronized void addParameterIfRequired(LinkedHashMap<String, String> parameters, PingParams pingInfo, String key, String value) {
		//System.out.println( "--" + key );
		if (pingInfo.includeParameter(key)) {
			//System.out.println( "----" + key + "-" + value);
			parameters.put(key, value);
		}
	}
}