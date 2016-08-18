package com.chartbeat.androidsdk;

import java.util.LinkedHashMap;

/**
 * Created by Mike Dai Wang on 2016-02-09.
 */
final class ViewInfo {
    private String viewID;
    private String viewTitle;
    private String internalReferrer;
    private String token;

    ViewInfo(String viewID, String viewTitle, String internalReferrer, String token) {
        this.viewID = viewID;
        this.viewTitle = viewTitle;
        this.internalReferrer = internalReferrer;
        this.token = token;
    }

    String getViewID() {
        return viewID;
    }

    String getViewTitle() {
        return viewTitle;
    }

    String getInternalReferrer() {
        return internalReferrer  == null ? "" : internalReferrer;
    }

    String getToken() {
        return token;
    }
}
