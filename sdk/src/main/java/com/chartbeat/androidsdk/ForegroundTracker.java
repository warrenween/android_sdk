package com.chartbeat.androidsdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * Created by Mike on 2017-11-02.
 */

class ForegroundTracker implements Application.ActivityLifecycleCallbacks {
    private static final long CHECK_STATUS_DELAY_MS = 500;
    private static final String TAG = ForegroundTracker.class.getSimpleName();

    public interface Listener {

        void onForegrounded();

        void onBackgrounded();

    }

    private static ForegroundTracker instance;

    private boolean isForeground = true;
    private boolean paused = false;

    private List<Listener> listeners = new CopyOnWriteArrayList();
    private Subscription enteredBackgroundSubscription;
    private static Object mutex = new Object();

    static ForegroundTracker init(Application application) {
        synchronized (mutex) {
            if (instance == null) {
                instance = new ForegroundTracker();
                application.registerActivityLifecycleCallbacks(instance);
            }
        }
        return instance;
    }

    static ForegroundTracker get(Application application) {
        if (instance == null) {
            init(application);
        }
        return instance;
    }

    static ForegroundTracker get(Context context) {
        if (instance == null) {
            Context appContext = context.getApplicationContext();
            if (appContext instanceof Application) {
                init((Application) appContext);
            }
            throw new IllegalStateException("ForegroundMonitor is not initialised and cannot obtain the Application context");
        }
        return instance;
    }

    static ForegroundTracker get() {
        if (instance == null) {
            throw new IllegalStateException("ForegroundMonitor is not initialised - " +
                    "invoke at least once with parameterised init/get");
        }
        return instance;
    }

    boolean appIsForeground() {
        return isForeground;
    }

    boolean isInBackground() {
        return !isForeground;
    }

    void addListener(Listener listener) {
        listeners.add(listener);
    }

    void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        paused = false;
        boolean wasBackground = !isForeground;
        isForeground = true;

        if (enteredBackgroundSubscription != null && !enteredBackgroundSubscription.isUnsubscribed()) {
            enteredBackgroundSubscription.unsubscribe();
            enteredBackgroundSubscription = null;
        }

        if (wasBackground) {
            Logger.d(TAG, "went foreground");
            for (Listener l : listeners) {
                try {
                    l.onForegrounded();
                } catch (Exception e) {
                    Logger.d(TAG, e.getMessage());
                }
            }
        } else {
            Logger.d(TAG, "still foreground");
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused = true;

        if (enteredBackgroundSubscription != null && !enteredBackgroundSubscription.isUnsubscribed()) {
            enteredBackgroundSubscription.unsubscribe();
        }

        enteredBackgroundSubscription = Observable.timer(CHECK_STATUS_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Logger.d(TAG, e.getMessage());
                    }

                    @Override
                    public void onNext(Long aLong) {
                        if (isForeground && paused) {
                            isForeground = false;
                            Logger.d(TAG, "went background");
                            for (Listener l : listeners) {
                                try {
                                    l.onBackgrounded();
                                } catch (Exception e) {
                                    Logger.d(TAG, e.getMessage());
                                }
                            }
                        } else {
                            Logger.d(TAG, "still appIsForeground");
                        }
                    }
                });
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }

    @Override
    public void onActivityStarted(Activity activity) { }

    @Override
    public void onActivityStopped(Activity activity) { }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

    @Override
    public void onActivityDestroyed(Activity activity) { }
}
