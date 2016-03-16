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

    String getInternalReferrer() {
        return internalReferrer;
    }

    String getToken() {
        return token;
    }

    LinkedHashMap<String, String> toPingParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        if (viewID != null) {
            params.put(QueryKeys.VIEW_ID, viewID);
        }

        if (viewTitle != null) {
            params.put(QueryKeys.VIEW_TITLE, viewTitle);
        }

        if (internalReferrer != null) {
            params.put(QueryKeys.INTERNAL_REFERRER, internalReferrer);
        } else {
            params.put(QueryKeys.INTERNAL_REFERRER, "");
        }

        if (token != null) {
            params.put(QueryKeys.TOKEN, token);
        }

        return params;
    }
}
