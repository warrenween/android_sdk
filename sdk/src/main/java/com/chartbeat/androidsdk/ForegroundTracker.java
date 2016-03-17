package com.chartbeat.androidsdk;

import java.util.TimerTask;

/**
 * It is very tricky to track if an app is in the foreground or not in android.
 * This class implements a solution inspired by this answer on stack overflow:
 * 
 * http://stackoverflow.com/questions/4414171/how-to-detect-when-an-android-app-goes-to-the-background-and-come-back-to-the-fo/15573121#15573121
 * 
 * 
 * @author bjorn
 *
 */
final class ForegroundTracker {
    private final static long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;
	private static java.util.Timer activityTransitionTimer;
    private static boolean background;

    private ForegroundTracker() {
    }
    
    static void activityEnded() {
        activityTransitionTimer = new java.util.Timer();
        TimerTask activityTransitionTimerTask = new TimerTask() {
            public void run() {
            	background = true;
            }
        };

        activityTransitionTimer.schedule(activityTransitionTimerTask, MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    static void activityStarted() {
        if (activityTransitionTimer != null) {
            activityTransitionTimer.cancel();
        }

        background = false;
    }
    
    static boolean isInBackground() {
    	return background;
    }
}
