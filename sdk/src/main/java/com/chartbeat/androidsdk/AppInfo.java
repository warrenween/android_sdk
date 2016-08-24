package com.chartbeat.androidsdk;

import android.content.Context;
import android.graphics.Point;

/**
 * Created by Mike Dai Wang on 2016-02-04.
 */
final class AppInfo {
    private static final String TAG = AppInfo.class.getSimpleName();
    private static final String SDK_NAME = "android";

    private static String packageName;
    private static String referrer;
    private static int deviceScreenWidth = -1;

    private String accountID;
    private String domain;

    AppInfo(Context context, String accountID, String domain) {
        if (accountID == null) {
            throw new NullPointerException("Account ID cannot be null");
        }

        this.accountID = accountID;

        if (packageName == null) {
            packageName = context.getPackageName();
        }

        if (domain == null) {
            throw new NullPointerException("Domain cannot be null");
        } else {
            this.domain = domain;
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
        return "Chartbeat tracking SDK (" + getSdkVersion() + "): " + this.accountID + "|" + this.packageName + "|" + this.domain;
    }

    String getDomain() {
        return domain;
    }

    String getPackageName() {
        return packageName;
    }

    String getAccountID() {
        return accountID;
    }

    String getSdkVersion() {
        return SDK_NAME + "_" + BuildConfig.VERSION_CODE;
    }

    String getDeviceScreenWidth() {
        return String.valueOf(deviceScreenWidth);
    }
}
