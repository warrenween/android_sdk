/**
 * Chartbeat Android API by Bjorn Roche.
 * (c) Chartbeat 2014
 */
package com.chartbeat.androidsdk;

import java.util.TimerTask;

import android.util.Log;

/**
 * @author bjorn
 *
 */
public final class Timer {
	private final String TAG = "Chartbeat Timer";
	private final long MILLISECONDS = 1000;
	private final long ONE_MINUTE = 1 * 60 * 1000;
	private final long SUSPENTION_TIME = 10 * ONE_MINUTE; //FIXME: should be ten!
	private java.util.Timer timer;
	private long defaultInterval = 15; // in seconds
	private long backgroundInterval = 60;
	private long backgroundCount = 0;
	private long currentInterval = defaultInterval;
	private boolean inBackground;
	private long suspendTime = 0;
	private boolean isSuspended = false;
	private boolean retryImmediately = false;
	
	private final Tracker tracker;

	/**
	 * 
	 */
	Timer(Tracker tracker) {
		this.tracker = tracker;
	}
	
	void start() {
		if( timer != null )
			return;
		restart();
	}
	
	void restart() {
		stop();
		timer = new java.util.Timer("Chartbeat Timer");
		timer.schedule( new MyTimerTask(), 0 );
	}
	
	void stop() {
		if( timer != null )
			timer.cancel();
		timer = null;
	}
	
	void suspend() {
		suspendTime = System.currentTimeMillis();
		isSuspended = true;
		stop();
	}
	
	/** this actually only un-suspends if ten minutes have passed */
	void unsuspend() {
		if( !isSuspended )
			return;
		long now = System.currentTimeMillis();
		if( now - suspendTime > SUSPENTION_TIME ) {
			isSuspended = false;
			restart();
		}
	}
	
	void retryImmediately() {
		retryImmediately = true;
	}
	int getCurrentInterval() {
		return (int) currentInterval;
	}
	int expectedNextInterval( boolean inBackground ) {
		return (int) ( inBackground ? backgroundInterval : currentInterval );
	}
	
	private final class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			try {
				if( !isSuspended ) {
					if( !inBackground || backgroundCount == 0 || retryImmediately ) {
						retryImmediately = false;
						tracker.ping();
					}
					if( inBackground ) {
						++backgroundCount;
					}
					if( backgroundCount >= backgroundInterval/currentInterval )
						backgroundCount = 0;
				}
			} catch (Exception e) {
				//we catch all exceptions to ensure that we can reschedule the next run.
				Log.e(TAG, "Problem executing: " + e );
				e.printStackTrace();
			}
			// schedule next
			long interval = currentInterval ;
			if( retryImmediately ) {
				interval = 0;
			}
			if( timer != null ) //timer might be null if we are suspended
				timer.schedule( new MyTimerTask(), interval*MILLISECONDS);
		}
	}

	void setInBackground(boolean inBackground) {
		if( inBackground && !this.inBackground )
			backgroundCount = 0;
		this.inBackground = inBackground;
	}
}
