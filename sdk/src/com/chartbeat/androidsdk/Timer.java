/**
 * 
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
	private java.util.Timer timer;
	private long defaultInterval = 15; // in seconds
	private long backgroundInterval = 60;
	private long maxInterval = 300;
	private long currentInterval = defaultInterval;
	private boolean inBackground;
	
	private final Tracker tracker;

	/**
	 * 
	 */
	public Timer(Tracker tracker) {
		this.tracker = tracker;
	}
	
	public void start() {
		stop();
		timer = new java.util.Timer("Chartbeat Timer");
		timer.schedule( new MyTimerTask(), 0 );
	}
	
	public void stop() {
		if( timer != null )
			timer.cancel();
		timer = null;
	}
	
	public void changeTimeInterval( int code ) {
		if( code == 500 ) {
			currentInterval = 0;
		} else if( code == 200 ) {
			currentInterval = defaultInterval;
		} else {
			if( currentInterval >= maxInterval || currentInterval <= defaultInterval ) {
				currentInterval = defaultInterval;
			} else {
				currentInterval *= 2;
				if( currentInterval > maxInterval )
					currentInterval = maxInterval;
			}
		}
	}
	
	public int getCurrentInterval() {
		return (int) currentInterval;
	}
	public int expectedNextSleep( boolean inBackground ) {
		return (int) ( inBackground ? backgroundInterval : currentInterval );
	}
	
	private final class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			try {
				tracker.ping();
			} catch (Exception e) {
				//we catch all exceptions to ensure that we can reschedule the next run.
				Log.e(TAG, "Problem executing: " + e );
			}
			// schedule next
			long interval = inBackground ? backgroundInterval : currentInterval ; 
			timer.schedule( new MyTimerTask(), interval*MILLISECONDS);
		}
	}

	public void isInBackground(boolean inBackground) {
		this.inBackground = inBackground;
	}
}
