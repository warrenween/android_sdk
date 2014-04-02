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
	private static final String TAG = "Chartbeat Timer";
	private static final long MILLISECONDS = 1000;
	private static final long ONE_MINUTE = 1 * 60 * 1000;
	private static final long ALIVE_FOR = 2 * 60 * ONE_MINUTE ; //after two hours of inactivity we shut down
	private static final long SUSPENSION_TIME = 10 * ONE_MINUTE; //suspension is after the server rejects us due to too many clients.
	private java.util.Timer timer;
	private long defaultInterval = 15; // in seconds
	private long backgroundInterval = 60;
	private long backgroundCount = 0;
	private long currentInterval = defaultInterval;
	private boolean inBackground;
	private long suspendTime = 0;
	private boolean isSuspended = false;
	private boolean retryImmediately = false;
	private volatile long lastAlive = 0;
	
	private final Tracker tracker;

	/**
	 * 
	 */
	Timer(Tracker tracker) {
		this.tracker = tracker;
	}
	
	synchronized void start() {
		if( timer != null )
			return;
		restart();
	}
	
	synchronized void restart() {
		stop();
		timer = new java.util.Timer("Chartbeat Timer");
		alive();
		timer.schedule( new MyTimerTask(), 0 );
	}
	
	synchronized void stop() {
		if( timer != null )
			timer.cancel();
		timer = null;
	}
	
	synchronized void suspend() {
		suspendTime = System.currentTimeMillis();
		isSuspended = true;
		stop();
	}
	
	/** this actually only un-suspends if ten minutes have passed */
	synchronized void unsuspend() {
		if( !isSuspended )
			return;
		long now = System.currentTimeMillis();
		if( now - suspendTime > SUSPENSION_TIME ) {
			isSuspended = false;
			restart();
		}
	}
	
	synchronized void retryImmediately() {
		retryImmediately = true;
	}
	synchronized int getCurrentInterval() {
		return (int) currentInterval;
	}
	synchronized int expectedNextInterval( boolean inBackground ) {
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
			if( timer != null && isAlive() ) //timer might be null if we are suspended
				timer.schedule( new MyTimerTask(), interval*MILLISECONDS);
			else
				stop();
		}
	}

	synchronized void setInBackground(boolean inBackground) {
		if( inBackground && !this.inBackground )
			backgroundCount = 0;
		this.inBackground = inBackground;
	}

	public synchronized void alive() {
		lastAlive = System.currentTimeMillis();
		if( timer == null )
			restart();
	}
	
	private synchronized boolean isAlive() {
		long now = System.currentTimeMillis();
		if( now - lastAlive > ALIVE_FOR )
			return false;
		return true;
	}
}
