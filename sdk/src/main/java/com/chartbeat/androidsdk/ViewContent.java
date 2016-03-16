package com.chartbeat.androidsdk;

import java.util.LinkedHashMap;

/**
 * Created by Mike Dai Wang on 2016-02-09.
 */
final class ViewContent {
    private static final float INVALID_LOAD_TIME = -1.0f;

    private String sections;
    private String authors;
    private String zones;
    private float pageLoadTime = INVALID_LOAD_TIME;

    ViewContent() {

    }

    ViewContent(String sections, String authors, String zones, float pageLoadTime) {
        this.sections = sections;
        this.authors = authors;
        this.zones = zones;
        this.pageLoadTime = pageLoadTime;
    }

    String getSections() {
        return sections;
    }

    String getAuthors() {
        return authors;
    }

    String getZones() {
        return zones;
    }

    float getPageLoadTime() {
        return pageLoadTime;
    }

    LinkedHashMap<String, String> toPingParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (sections != null) {
            params.put(QueryKeys.SECTION_G0, sections);
        }

        if (authors != null) {
            params.put(QueryKeys.AUTHOR_G1, authors);
        }

        if (zones != null) {
            params.put(QueryKeys.ZONE_G2, zones);
        }

        if (pageLoadTime != INVALID_LOAD_TIME) {
            params.put(QueryKeys.PAGE_LOAD_TIME, String.valueOf(pageLoadTime));
        }

        return params;
    }
}
