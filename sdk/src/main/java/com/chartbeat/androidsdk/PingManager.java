package com.chartbeat.androidsdk;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;

/**
 * @author bjorn
 */
final class PingManager {
	private static final String TAG = PingManager.class.getSimpleName();

	private static final long SECOND = 1000;
	private static final long MINUTE = 60 * 1000;
    private static final long HOUR = 60 * MINUTE;
	private static final long MAX_SDK_RUN_TIME = 2 * HOUR; //after two hours of inactivity we shut down
	private static final long SERVER_BUSY_WAIT_WINDOW = 10 * MINUTE; //suspension is after the server rejects us due to too many clients.
    private static final int DEFAULT_PING_INTERVAL_IN_SECONDS = 15;
    private static final int DEFAULT_PING_DECAY = DEFAULT_PING_INTERVAL_IN_SECONDS * 2;
    private static final String DEFAULT_EXIT_VIEW_DECAY_TIME_IN_SECONDS = "90";

    private final ChartBeatTracker tracker;

    private static Handler handler;
    private Observer pingObserver;
    private Subscription pingSubscription;
	private int pingInterval;

	private boolean inBackground;
    private boolean wasInBackground;

    private volatile long lastActiveTimestamp = 0;
    private volatile boolean isSuspended;
    private volatile long suspensionTime;

	PingManager(ChartBeatTracker tracker, Looper looper){
        this.tracker = tracker;
        pingInterval = DEFAULT_PING_INTERVAL_IN_SECONDS;

        isSuspended = false;
        suspensionTime = 0;

        wasInBackground = false;
        inBackground = false;

        handler = new Handler(looper);

        pingObserver = new Observer() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Object o) {
                        if (handler.getLooper().getThread().isAlive()) {
                            // Only ping on an alive thread
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    runTask();
                                }
                            });
                        }
                    }
                };
    }
	
    void restart() {
        inBackground = false;
		stop();
        start(pingInterval);
	}

    private void restartForBackground() {
        stop();
        alive();
        start(pingInterval * 2);
    }

    void stop() {
        if (pingSubscription != null) {
            pingSubscription.unsubscribe();
        }
    }

    void retryImmediately() {
        restart();
    }

    long getPingInterval() {
        return pingInterval;
    }

    int expectedNextIntervalInSeconds() {
        return pingInterval;
    }

    void setInBackground(boolean isInBackground) {
        this.inBackground = isInBackground;
    }

    void suspendDueToServerBusy() {
        suspensionTime = System.currentTimeMillis();
        isSuspended = true;
    }

    void alive() {
        lastActiveTimestamp = System.currentTimeMillis();

        if (inBackground) {
            restart();
        }
    }

    private boolean isServerSuspended() {
        if (isSuspended) {
            long timePassedSinceSuspension = System.currentTimeMillis() - suspensionTime;

            if(timePassedSinceSuspension > SERVER_BUSY_WAIT_WINDOW) {
                isSuspended = false;
            }
        }

        return isSuspended;
    }
	
	private void start(int interval) {
        pingSubscription = Observable.interval(0, interval, TimeUnit.SECONDS)
                .filter(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        if (isServerSuspended()) {
                            return false;
                        }
                        return true;
                    }
                })
                .filter(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        if (isDormant()) {
                            return false;
                        }
                        return true;
                    }
                })
                .subscribe(pingObserver);
	}

    private void runTask() {
        try {
            if(inBackground) { // now in background
                wasInBackground = true;
                return;
            }

            int decayTime = DEFAULT_PING_DECAY;
            tracker.ping(wasInBackground, String.valueOf(decayTime));
            wasInBackground = false;
        } catch (Exception e) {
            //we catch all exceptions to ensure that we can reschedule the next run.
            Logger.e(TAG, "Problem executing: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
	
	private boolean isDormant() {
        long idlingTime = System.currentTimeMillis() - lastActiveTimestamp;

        if(idlingTime > MAX_SDK_RUN_TIME) {
            return true;
        }

		return false;
	}
}
