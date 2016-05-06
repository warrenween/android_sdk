package com.chartbeat.androidsdk;

import android.util.Log;

/**
 * Created by Mike Dai Wang on 2016-05-06.
 */
class Logger {
    static void d(String TAG, String logMessage) {
        if (Tracker.DEBUG_MODE) {
            Log.d(TAG, logMessage);
        }
    }

    static void e(String TAG, String logMessage) {
        if (Tracker.DEBUG_MODE) {
            Log.e(TAG, logMessage);
        }
    }

    static void w(String TAG, String logMessage) {
        if (Tracker.DEBUG_MODE) {
            Log.w(TAG, logMessage);
        }
    }

    static void v(String TAG, String logMessage) {
        if (Tracker.DEBUG_MODE) {
            Log.v(TAG, logMessage);
        }
    }
}
