package com.chartbeat.androidsdk;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks user engagement and calculates the necessary metrics.
 * 
 * @author bjorn
 */
final class EngagementTracker {
    private static final String TAG = EngagementTracker.class.getSimpleName();

    private static final String ENGAGEMENT_TRACKING_THREAD_NAME = "Engagement_Timer";

    private static final int SECOND = 1000;
    private static final int INITIAL_DELAY = 0;
    private static final int ENGAGEMENT_CHECK_PERIOD = 1 * SECOND;

    private boolean engaged, typed;

    private java.util.Timer timer;
    private EngagementTask engagementTask;

    EngagementTracker() {
        engaged = false;
        typed = false;
        engagementTask = new EngagementTask();
    }

    synchronized void userEnteredView() {
        stop();
        engagementTask = new EngagementTask();
        timer = new Timer(ENGAGEMENT_TRACKING_THREAD_NAME);
        timer.schedule(engagementTask, INITIAL_DELAY, ENGAGEMENT_CHECK_PERIOD);
    }

    synchronized void userLeftView() {
        stop();
    }

    synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    synchronized void userEngaged() {
        engaged = true;
        engagementTask.engage();
    }

    synchronized void userTyped() {
        typed = true;
        engagementTask.engage();
    }

    synchronized void lastPingFailed(EngagementData ed) {
        engaged |= ed.engaged;
        typed |= ed.typed;
    }

    synchronized EngagementData getEngagementSnapshot() {
        EngagementData data = new EngagementData(engagementTask.getTotalEngagementCount(), engaged, typed );
        engaged = false;
        typed = false;
        return data;
    }

    private static final class EngagementTask extends TimerTask {
        private static final int SECOND = 1000;
        private static final int ENGAGEMENT_WINDOW = 5 * SECOND; //default to 5 until we get info from the server otherwise.
        private static final int INITIAL_ENGAGEMENT_WINDOW = 5 * SECOND; //user is always considered engaged for the first 5 seconds.

        private final long startTime;
        private long lastEngagedTime = 0;
        private AtomicLong totalEngagementCount;

        public EngagementTask() {
            startTime = System.currentTimeMillis();
            lastEngagedTime = 0;
            totalEngagementCount = new AtomicLong(0);
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long engagedPeriod = now - lastEngagedTime;
            long engagedTimeSinceEnteringView = now - startTime;
            if (engagedPeriod < ENGAGEMENT_WINDOW || engagedTimeSinceEnteringView < INITIAL_ENGAGEMENT_WINDOW) {
                totalEngagementCount.incrementAndGet();
            }
        }

        public void engage() {
            this.lastEngagedTime = System.currentTimeMillis();
        }

        public long getTotalEngagementCount() {
            return totalEngagementCount.longValue();
        }
    }

    final static class EngagementData {
        final boolean engaged, typed, reading, idle;
        final long totalEngagement;

        public EngagementData(long totalEngagement, boolean engaged, boolean typed) {
            this.engaged = engaged;
            this.typed = typed;
            this.reading = engaged && (!typed);
            this.idle = !engaged;
            this.totalEngagement = totalEngagement;
        }
    }
}
