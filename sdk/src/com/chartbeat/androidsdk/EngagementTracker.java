/**
 * Chartbeat Android API by Bjorn Roche.
 * (c) Chartbeat 2014
 */
package com.chartbeat.androidsdk;

import java.util.TimerTask;


/**
 * Tracks user engagement and calculates the necessary metrics.
 * 
 * @author bjorn
 */
final class EngagementTracker extends TimerTask {
	private int engagementWindow = 5;
	private boolean engaged, typed;
	private long totalEngagement;
	private final int INITIAL_ENGAGEMENT = 5; //user is always considered engaged for the first 5 seconds.
	private long lastEngaged;
	private final long startTime;
	private java.util.Timer timer = new java.util.Timer();

	EngagementTracker() {
		engaged = false;
		typed = false;
		lastEngaged = 0;
		
		startTime = System.currentTimeMillis();
		
		timer.schedule( this, 0, 1000 );
	}
	
	synchronized void setEngagementWindow( int window ) {
		engagementWindow = window;
	}
	
	synchronized void userEngaged() {
		engaged = true;
		lastEngaged = System.currentTimeMillis();
	}
	
	synchronized void userTyped() {
		typed = true;
		lastEngaged = System.currentTimeMillis();
	}
	
	synchronized void userEnteredView() {
		totalEngagement = 0;
		lastEngaged = 0;
	}

	synchronized void userLeftView() {
		//totalEngagement = 0;
		//lastEngaged = 0;
	}

	synchronized EngagementData ping() {
		EngagementData ret = new EngagementData( totalEngagement, engaged, typed );
		engaged = false;
		typed = false;
		return ret;
	}
	
	synchronized void lastPingFailed(EngagementData ed) {
		engaged |= ed.engaged;
		typed |= ed.typed;
	}

	@Override
	synchronized public void run() {
		long now = System.currentTimeMillis();
		if( now - lastEngaged <= engagementWindow*1000 || now - startTime <= INITIAL_ENGAGEMENT*1000 ) {
			++totalEngagement;
		}
	}
	static class EngagementData {
		final boolean engaged, typed, reading, idle;
		final long totalEngagement;
		private EngagementData( long totalEngagement, boolean engaged, boolean typed ) {
			this.engaged = engaged;
			this.typed = typed;
			this.reading = engaged && (!typed);
			this.idle = !engaged;
			this.totalEngagement = totalEngagement;
		}
	}
}
