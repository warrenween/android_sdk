package com.chartbeat.androidsdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebSettings;

import java.lang.reflect.Constructor;

/**
 * Created by Mike Dai Wang on 2016-02-04.
 */
final class SystemUtils {
    private static final String GENERIC_ANDROID_MOBILE_USER_AGENT = "Mozilla/5.0 (Android 6.0.1; Mobile; rv:50.0) Gecko/50.0 Firefox/50.0";
    private static final String GENERIC_ANDROID_TABLET_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 7.0; Pixel C Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/52.0.2743.98 Safari/537.36";
    private static final String USER_AGENT_SUFFIX = "/cbua/App";
    private static final String STATIC_MOBILE_USER_AGENT = GENERIC_ANDROID_MOBILE_USER_AGENT + USER_AGENT_SUFFIX;
    private static final String STATIC_TABLET_USER_AGENT = GENERIC_ANDROID_TABLET_USER_AGENT + USER_AGENT_SUFFIX;
    private static final String TAG = SystemUtils.class.getSimpleName();

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Checks if the device is a tablet (7" diagonal or greater).
     */
    private static boolean checkIsTablet(Context context) {
        boolean isTablet = false;

        try {
            Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();

            // This should maybe use display.getRealMetrics but that's only available in API 17+
            display.getMetrics(metrics);
            float widthInches = metrics.widthPixels / metrics.xdpi;
            float heightInches = metrics.heightPixels / metrics.ydpi;
            double diagonalInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));
            // Logger.d(TAG, "***** Screen Diagonal Size: " + diagonalInches);
            if (diagonalInches >= 7.0) {
                isTablet = true;
            }
        } catch(Exception e) {
            // If we catch an exception, just assume it's not a tablet since that's more likely.
            // Logger.e(TAG, "***** Caught Exception: " + e.getStackTrace());
        }

        return isTablet;
    }

    /**
     * Get a user agent that our backend will detect as Android and as mobile or tablet, depending
     * on screen size of this device.
     * @param context
     * @return
     */
    static String getSystemUserAgent(Context context) {
        boolean isTablet = SystemUtils.checkIsTablet(context);
        return isTablet ? STATIC_TABLET_USER_AGENT : STATIC_MOBILE_USER_AGENT;
    }

    @TargetApi(17)
    private static class NewApiWrapper {
        static String getDefaultUserAgent(Context context) {
            return WebSettings.getDefaultUserAgent(context);
        }
    }

    /**
     * Get screen size for API level < 17
     */
    static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();

        float density = context.getResources().getDisplayMetrics().density;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
        } else {
            size.x = display.getWidth();
            size.y = display.getHeight();
        }

        int width = (int)(size.x / density);
        int height = (int)(size.y / density);

        Point densityIndependentSize = new Point(width, height);

        return densityIndependentSize;
    }
}
