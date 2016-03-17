package com.chartbeat.androidsdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.reflect.Constructor;

/**
 * Created by Mike Dai Wang on 2016-02-04.
 */
final class SystemUtils {
    private static final String USER_AGENT_SUFFIX = "/App";

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Get the default user agent string for webviews
     * @param context
     * @return
     */
    public static String getSystemUserAgent(Context context) {
        String userAgent;

        if (Build.VERSION.SDK_INT >= 17) {
            userAgent = NewApiWrapper.getDefaultUserAgent(context);
        } else {
            try {
                Constructor<WebSettings> constructor = WebSettings.class.getDeclaredConstructor(Context.class, WebView.class);
                constructor.setAccessible(true);
                try {
                    WebSettings settings = constructor.newInstance(context, null);
                    userAgent = settings.getUserAgentString();
                } finally {
                    constructor.setAccessible(false);
                }
            } catch (Exception e) {
                userAgent = new WebView(context).getSettings().getUserAgentString();
            }
        }

        return userAgent + USER_AGENT_SUFFIX;
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
    public static Point getScreenSize(Context context) {
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
