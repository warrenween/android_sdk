package com.chartbeat.androidsdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import rx.Observer;

/**
 * Created by Mike Dai Wang on 2016-02-05.
 */
final class ChartBeatTracker {
    private static final String TAG = ChartBeatTracker.class.getSimpleName();
    public static final String CHARTBEAT_PREFS = "com.chartbeat.androidsdk.user";

    private static final int SESSION_TOKEN_LENGTH = 30;

    private static final long MILLISECONDS_IN_ONE_SECOND = 1000;

    private static Handler handler;
    private static boolean firstPing = true;

    private static PingService pingService;

    private final WeakReference<Context> context;
    private final AppInfo appInfo;
    private final UserInfo userInfo;
    private final EngagementTracker engagementTracker;

    private String previousToken;
    private ViewTracker currentViewTracker;

    private PingParams pingParams;

	/** set to true to simulate a very long response to pings (10 seconds) */
	public static final boolean SIMULATE_VERY_SLOW_SERVER = false;

	private final PingManager pingManager;
	private long lastSuccessfulPingTime = 0;
	private long lastDecayTime = 0;
	private int sequentialErrors; // counts 503 errors.

    ChartBeatTracker(WeakReference<Context> context, String accountID, String domain, String userAgent, Looper looper) {
        this.context = context;

        if (handler == null) {
            handler = new Handler(looper);
        }

        pingService = new PingService(userAgent);

        this.appInfo = new AppInfo(context.get(), accountID, domain);
        this.userInfo = new UserInfo(context.get());

		this.currentViewTracker = null;
		this.pingManager = new PingManager(this, looper);

		this.engagementTracker = new EngagementTracker();

		this.pingParams = new PingParams();

        Logger.d(TAG, appInfo.toString());
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
        String domain = null;
        String subdomain = null;

        if (currentViewTracker != null) {
            domain = currentViewTracker.getDomain();
            subdomain = currentViewTracker.getSubdomain();
            internalReferral = currentViewTracker.getViewID();
            this.previousToken = currentViewTracker.getToken();
        }

        String generatedToken = SecurityUtils.randomChars(SESSION_TOKEN_LENGTH);
        ViewDimension viewDimension = new ViewDimension(x, w, y, o, x);

        currentViewTracker = new ViewTracker(viewId, viewTitle, domain, subdomain, internalReferral, generatedToken, viewDimension);
        pingParams.newView();

        Logger.d(TAG, appInfo.toString() + " :: TRACK VIEW :: " + viewId);

        this.pingParams.addOneTimeParameter(QueryKeys.FORCE_DECAY);
        this.pingParams.addOneTimeParameter(QueryKeys.SCROLL_POSITION_TOP);
        this.pingParams.addOneTimeParameter(QueryKeys.CONTENT_HEIGHT);
        this.pingParams.addOneTimeParameter(QueryKeys.SCROLL_WINDOW_HEIGHT);
        this.pingParams.addOneTimeParameter(QueryKeys.DOCUMENT_WIDTH);
        this.pingParams.addOneTimeParameter(QueryKeys.MAX_SCROLL_DEPTH);

        pingManager.restart();
    }

    synchronized void userInteractedImpl() {
        engagementTracker.userEngaged();
        userInfo.visited();
        pingManager.alive();

        Logger.d(TAG, appInfo.toString() + " :: USER INTERACTED");
	}

    synchronized void userTypedImpl() {
        engagementTracker.userTyped();
        userInfo.visited();
        pingManager.alive();
        Logger.d(TAG, appInfo.toString() + " :: USER TYPED");
	}

    synchronized void userLeftViewImpl(String viewId) {
        pingManager.setInBackground(true);
        engagementTracker.userLeftView();
        Logger.d(TAG, appInfo.toString() + " :: USER LEFT");
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

    synchronized void updateSubdomain(final String subdomain) {
        currentViewTracker.updateSubdomain(subdomain);
        pingManager.alive();
    }

    synchronized void updateDomain(final String domain) {
        currentViewTracker.updateDomain(domain);
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
        /* Not needed at the moment, may need it later on
        pingParams.addOneTimeParameter(QueryKeys.PAGE_LOAD_TIME);
        */
        pingManager.alive();
    }

	void ping(boolean needsFullPingHint, String decay) {
		LinkedHashMap<String, String> parameters = new LinkedHashMap<>(30);
		final EngagementTracker.EngagementSnapshot engagementSnapshot;

		// setup parameters in a synchronized block:
		synchronized (this) {
			if (currentViewTracker == null)
				return;

			// decrease the likelihood of getting a 500 back
			if (needsFullPingHint) {
                pingParams.pingReset();
            }

            if (currentViewTracker.getDomain() != null) {
                addParameterIfRequired(parameters, QueryKeys.HOST, currentViewTracker.getDomain());
            } else {
                addParameterIfRequired(parameters, QueryKeys.HOST, appInfo.getDomain());
            }

            addParameterIfRequired(parameters, QueryKeys.VIEW_ID, currentViewTracker.getViewID());
            addParameterIfRequired(parameters, QueryKeys.USER_ID, userInfo.getUserID());

            if (currentViewTracker.getSubdomain() != null) {
                addParameterIfRequired(parameters, QueryKeys.SUBDOMAIN, currentViewTracker.getSubdomain());
            } else {
                addParameterIfRequired(parameters, QueryKeys.SUBDOMAIN, appInfo.getDomain());
            }

            addParameterIfRequired(parameters, QueryKeys.ACCOUNT_ID, appInfo.getAccountID());

            // sections, authors, zones
            addParametersIfRequired(parameters, currentViewTracker.getContentParams());

            addParameterIfRequired(parameters, QueryKeys.IS_NEW_USER, userInfo.isNewUser() ? "1" : "0");
            addParameterIfRequired(parameters, QueryKeys.VISIT_FREQUENCY, userInfo.getUserVisitFrequencyString());
            addParameterIfRequired(parameters, QueryKeys.TIME_ON_VIEW_IN_MINUTES, currentViewTracker.getMinutesInView());
            addParametersIfRequired(parameters, currentViewTracker.getDimensionParams());

            addParameterIfRequired(parameters, QueryKeys.DECAY, decay);

            // engagement keys
            engagementSnapshot = engagementTracker.getEngagementSnapshot();
            parameters.put(QueryKeys.READING, engagementSnapshot.reading ? "1" : "0");
            parameters.put(QueryKeys.WRITING, engagementSnapshot.typed ? "1" : "0");
            parameters.put(QueryKeys.IDLING, engagementSnapshot.idle ? "1" : "0");
            parameters.put(QueryKeys.ENGAGED_SECONDS, String.valueOf(engagementSnapshot.totalEngagement));
            parameters.put(QueryKeys.ENGAGED_SECONDS_SINCE_LAST_PING, String.valueOf(engagementSnapshot.engagementSinceLastPing));

            // referrer keys
            addParameterIfRequired(parameters, QueryKeys.INTERNAL_REFERRER, currentViewTracker.getInternalReferrer());
            if (firstPing) {
                addParameterIfRequired(parameters, QueryKeys.EXTERNAL_REFERRER, appInfo.getExternalReferrer());
            }

            addParameterIfRequired(parameters, QueryKeys.TOKEN, currentViewTracker.getToken());
            addParameterIfRequired(parameters, QueryKeys.SDK_VERSION, appInfo.getSdkVersion());

            if (previousToken != null) {
                addParameterIfRequired(parameters, QueryKeys.FORCE_DECAY, previousToken);
            }
            lastDecayTime = pingManager.expectedNextIntervalInSeconds() * 2 * MILLISECONDS_IN_ONE_SECOND;

            addParameterIfRequired(parameters, QueryKeys.VIEW_TITLE, currentViewTracker.getViewTitle());

            int timezoneOffset = -(TimeZone.getDefault().getOffset(new Date().getTime()) / 1000 / 60);
            parameters.put(QueryKeys.TIME_ZONE, String.valueOf(timezoneOffset));

            addParameterIfRequired(parameters, QueryKeys.SCREEN_WIDTH, appInfo.getDeviceScreenWidth());

//            if (locationService != null) {
//                addParameterIfRequired(parameters, QueryKeys.LONGITUDE, locationService.getLongitude());
//                addParameterIfRequired(parameters, QueryKeys.LATITUDE, locationService.getLatitude());
//            }

			// last key must be an empty underscore
			parameters.put(QueryKeys.END_MARKER, "");

            Logger.d(TAG, "PING! User Data: " + parameters);
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
                                    handlePingError(e.getLocalizedMessage(), engagementSnapshot);
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
                                    handlePingResponseCode(code, engagementSnapshot);
                                }
                            });
                        }
                    });
		} else {
			synchronized( this ) {
                Logger.e(TAG, "Not pinging: no network connection detected.");
				pingParams.pingReset();
				engagementTracker.lastPingFailed(engagementSnapshot);
			}
		}
	}

    private void handlePingResponseCode(int code, EngagementTracker.EngagementSnapshot engagementSnapshot) {
        synchronized( this ) {
            // Only process ping response when able to
            if (handler.getLooper().getThread().isAlive()) {
                boolean isInBackground;
                try {
                    isInBackground = ForegroundTracker.get().isInBackground();
                } catch (IllegalStateException e) {
                    return;
                }

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
                if (code == 500 || code == 400 || code == 202) {
                    engagementTracker.lastPingFailed(engagementSnapshot);
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
    }

    private void handlePingError(String errorMessage, EngagementTracker.EngagementSnapshot engagementSnapshot) {
        pingParams.pingError();
        Logger.e(TAG, "Error pinging Chartbeat: " + errorMessage);
        engagementTracker.lastPingFailed(engagementSnapshot);
    }

    private synchronized void addParametersIfRequired(LinkedHashMap<String, String> parameters, LinkedHashMap<String, String> additionalParameters) {
        for (String key : additionalParameters.keySet()) {
            addParameterIfRequired(parameters, key, additionalParameters.get(key));
        }
    }

	private synchronized void addParameterIfRequired(LinkedHashMap<String, String> parameters, String key, String value) {
        if (key != null && value != null) {
            addParameterIfRequired(parameters, pingParams, key, value);
        }
	}

	private synchronized void addParameterIfRequired(LinkedHashMap<String, String> parameters, PingParams pingInfo, String key, String value) {
		//System.out.println( "--" + key );
		if (pingInfo.includeParameter(key)) {
			//System.out.println( "----" + key + "-" + value);
			parameters.put(key, value);
		}
	}
}
