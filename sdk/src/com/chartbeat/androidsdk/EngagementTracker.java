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
	private int totalEngagement;
	private final int INITIAL_ENGAGEMENT = 5; //user is always considered engaged for the first 5 seconds.
	private long lastEngaged;
	private final long startTime;
	private java.util.Timer timer = new java.util.Timer();

	public EngagementTracker() {
		engaged = false;
		typed = false;
		lastEngaged = 0;
		
		startTime = System.currentTimeMillis();
		
		timer.schedule( this, 0, 1000 );
	}
	
	synchronized public void setEngagementWindow( int window ) {
		engagementWindow = window;
	}
	
	synchronized public void userEngaged() {
		engaged = true;
		lastEngaged = System.currentTimeMillis();
	}
	
	synchronized public void userTyped() {
		typed = true;
		lastEngaged = System.currentTimeMillis();
	}

	synchronized public EngagementData ping() {
		EngagementData ret = new EngagementData( totalEngagement, engaged, typed );
		totalEngagement = 0;
		engaged = false;
		typed = false;
		return ret;
	}
	
	synchronized void lastPingFailed(EngagementData ed) {
		totalEngagement += ed.totalEngagement;
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
		final int totalEngagement;
		private EngagementData( int totalEngagement, boolean engaged, boolean typed ) {
			this.engaged = engaged;
			this.typed = typed;
			this.reading = engaged && (!typed);
			this.idle = !engaged;
			this.totalEngagement = totalEngagement;
		}
	}
}
