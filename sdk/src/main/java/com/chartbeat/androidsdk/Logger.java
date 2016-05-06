package com.chartbeat.androidsdk;

import android.util.Log;

/**
 * Created by Mike Dai Wang on 2016-05-06.
 */
public class Logger {
    public static void log(String TAG, String logMessage) {
        if (Tracker.DEBUG) {
            Log.d(TAG, logMessage);
        }
    }
}
