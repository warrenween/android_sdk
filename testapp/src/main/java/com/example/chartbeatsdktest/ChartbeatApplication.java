package com.example.chartbeatsdktest;

import android.app.Application;

import com.chartbeat.androidsdk.Tracker;

/**
 * Created by Mike Dai Wang on 2016-02-05.
 */
public class ChartbeatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Tracker.setupTracker("54876", "androidsdktest.chartbeat.com", this);
    }
}
