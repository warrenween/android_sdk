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
	private final long ONE_MINUTE = 1 * 60 * 1000;
	private final long SUSPENTION_TIME = ONE_MINUTE; //FIXME: should be ten!
	private java.util.Timer timer;
	private long defaultInterval = 15; // in seconds
	private long backgroundInterval = 60;
	private long maxInterval = 300;
	private long currentInterval = defaultInterval;
	private boolean inBackground;
	private long suspendTime = 0;
	private boolean isSuspended = false;
	private boolean retryImmediately = false;
	
	private final Tracker tracker;

	/**
	 * 
	 */
	public Timer(Tracker tracker) {
		this.tracker = tracker;
	}
	
	public void start() {
		if( timer != null )
			return;
		restart();
	}
	
	public void restart() {
		stop();
		timer = new java.util.Timer("Chartbeat Timer");
		timer.schedule( new MyTimerTask(), 0 );
	}
	
	public void stop() {
		if( timer != null )
			timer.cancel();
		timer = null;
	}
	
	public void suspend() {
		suspendTime = System.currentTimeMillis();
		isSuspended = true;
		stop();
	}
	
	/** this actually only un-suspends if ten minutes have passed */
	public void unsuspend() {
		if( !isSuspended )
			return;
		long now = System.currentTimeMillis();
		if( now - suspendTime > SUSPENTION_TIME ) {
			isSuspended = false;
			restart();
		}
	}
	
//	public void changeTimeInterval( int code ) {
//		if( code == 500 ) {
//			currentInterval = 0;
//		} else if( code == 200 ) {
//			currentInterval = defaultInterval;
//		} else {
//			if( currentInterval >= maxInterval || currentInterval <= defaultInterval ) {
//				currentInterval = defaultInterval;
//			} else {
//				currentInterval *= 2;
//				if( currentInterval > maxInterval )
//					currentInterval = maxInterval;
//			}
//		}
//	}
	
	public void retryImmediately() {
		retryImmediately = true;
	}
	public int getCurrentInterval() {
		return (int) currentInterval;
	}
	public int expectedNextInterval( boolean inBackground ) {
		return (int) ( inBackground ? backgroundInterval : currentInterval );
	}
	
	private final class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			try {
				if( !isSuspended ) {
					tracker.ping();
				}
			} catch (Exception e) {
				//we catch all exceptions to ensure that we can reschedule the next run.
				Log.e(TAG, "Problem executing: " + e );
				e.printStackTrace();
			}
			// schedule next
			long interval = inBackground ? backgroundInterval : currentInterval ;
			if( retryImmediately ) {
				retryImmediately = false;
				interval = 0;
			}
			if( timer != null ) //timer might be null if we are suspended
				timer.schedule( new MyTimerTask(), interval*MILLISECONDS);
		}
	}

	public void isInBackground(boolean inBackground) {
		this.inBackground = inBackground;
	}
}
