package com.chartbeat.androidsdk;

import java.util.LinkedHashMap;

/**
 * Created by Mike Dai Wang on 2016-02-09.
 */
final class ViewDimension {
    private int scrollPositionTop = -1;
    private int totalContentHeight = -1;
    private int scrollWindowHeight = -1;
    private int fullyRenderedDocWidth = -1;
    private int maxScrollDepth = -1;

    ViewDimension() {

    }

    ViewDimension(int scrollPositionTop,
                  int scrollWindowHeight,
                  int totalContentHeight,
                  int fullyRenderedDocWidth,
                  int maxScrollDepth) {
        this.scrollPositionTop = scrollPositionTop;
        this.totalContentHeight = totalContentHeight;
        this.scrollWindowHeight = scrollWindowHeight;
        this.fullyRenderedDocWidth = fullyRenderedDocWidth;
        this.maxScrollDepth = maxScrollDepth;
    }

    int getMaxScrollDepth() {
        return maxScrollDepth;
    }

    LinkedHashMap<String, String> toPingParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        
        if (scrollPositionTop != -1) {
            params.put(QueryKeys.SCROLL_POSITION_TOP, String.valueOf(scrollPositionTop));
        }

        if (maxScrollDepth != -1) {
            params.put(QueryKeys.MAX_SCROLL_DEPTH, String.valueOf(maxScrollDepth));
        }

        if (totalContentHeight != -1) {
            params.put(QueryKeys.CONTENT_HEIGHT, String.valueOf(totalContentHeight));
        }

        if (fullyRenderedDocWidth != -1) {
            params.put(QueryKeys.DOCUMENT_WIDTH, String.valueOf(fullyRenderedDocWidth));
        }

        if (scrollWindowHeight != -1) {
            params.put(QueryKeys.SCROLL_WINDOW_HEIGHT, String.valueOf(scrollWindowHeight));
        }

        return params;
    }
}
