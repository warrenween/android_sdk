package com.chartbeat.androidsdk;

import android.content.Context;
import android.graphics.Point;

import java.util.LinkedHashMap;

/**
 * Created by Mike Dai Wang on 2016-02-04.
 */
final class AppInfo {
    private static final String TAG = AppInfo.class.getSimpleName();
    private static final String SDK_NAME = "android";

    public static final int VERSION_CODE = 5;
    public static final String SDK_VERSION = SDK_NAME + "_" + VERSION_CODE;

    private static String packageName;
    private static String referrer;
    private static int deviceScreenWidth = -1;

    private String accountID;
    private String host;

    AppInfo(Context context, String accountID, String customHost) {
        if (accountID == null) {
            throw new NullPointerException("Account ID cannot be null");
        }

        this.accountID = accountID;

        if (packageName == null) {
            packageName = context.getPackageName();
        }

        if (customHost == null) {
            host = packageName;
        } else {
            host = customHost;
        }

        if (deviceScreenWidth == -1) {
            Point size = SystemUtils.getScreenSize(context);
            deviceScreenWidth = size.x;
        }
    }

    void setExternalReferrer(String externalReferrer) {
        referrer = externalReferrer;
    }

    String getExternalReferrer() {
        return referrer == null ? "" : referrer;
    }

    public String toString() {
        return this.accountID + ":" + this.packageName + ":" + this.host;
    }

    LinkedHashMap<String, String> toPingParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        params.put(QueryKeys.SDK_VERSION, SDK_VERSION);

        if (accountID != null) {
            params.put(QueryKeys.ACCOUNT_ID, accountID);
        }

        if (host != null) {
            params.put(QueryKeys.HOST, host);
        }

        if (packageName != null) {
            params.put(QueryKeys.REAL_DOMAIN_PACKAGE_NAME, packageName);
        }

        if (deviceScreenWidth != -1) {
            params.put(QueryKeys.SCREEN_WIDTH, String.valueOf(deviceScreenWidth));
        }

        return params;
    }
}
