/**
 * Chartbeat Android API by Mike Dai Wang.
 * (c) Chartbeat 2016
 */
package com.chartbeat.androidsdk;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;

import java.lang.ref.WeakReference;

/**
 * Created by Mike Dai Wang on 2016-02-10.
 */
public class ChartbeatService extends Service {
    private static final String TAG = ChartbeatService.class.getSimpleName();
    private static final String TRACKER_THREAD = "TRACKER_THREAD";

    private static HandlerThread bgThread;
    private static ChartbeatServiceHandler handler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (bgThread == null || !bgThread.isAlive()) {
            bgThread = new HandlerThread(TRACKER_THREAD, Process.THREAD_PRIORITY_BACKGROUND);
            bgThread.start();
        }

        String userAgent = SystemUtils.getSystemUserAgent(this);

        handler = new ChartbeatServiceHandler(new WeakReference<Context>(this), bgThread.getLooper(), userAgent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Message msg = handler.obtainMessage();
            msg.arg1 = startId;
            msg.setData(intent.getExtras());
            handler.sendMessage(msg);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (bgThread.getLooper() != null) {
                bgThread.getLooper().quitSafely();
            }

            bgThread.quitSafely();
        } else {
            if (bgThread.getLooper() != null) {
                bgThread.getLooper().quit();
            }

            bgThread.quit();
        }

        bgThread = null;
    }
}
