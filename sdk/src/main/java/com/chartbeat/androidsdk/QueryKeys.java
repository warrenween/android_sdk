package com.chartbeat.androidsdk;

/**
 * Created by Mike Dai Wang on 2016-02-08.
 */
final class QueryKeys {
    /* Mandatory params */
    /* One time */
    public static final String SDK_VERSION = "V";
    public static final String HOST = "h";
    public static final String REAL_DOMAIN_PACKAGE_NAME = "d";
    public static final String ACCOUNT_ID = "g";
    public static final String USER_ID = "u";

    // May get random 500 error, set to 2 * ping interval
    public static final String DECAY = "j";

    /* Per view visited */
    public static final String TOKEN = "t";

    public static final String VIEW_ID = "p";
    public static final String TIME_ON_VIEW_IN_MINUTES = "c";
    public static final String SCREEN_WIDTH = "S";
    public static final String PAGE_LOAD_TIME = "b";

    // Set to the last token if time passed < decay time since past successful ping
    public static final String FORCE_DECAY = "D";

    // From EngagementTracker
    public static final String ENGAGED_SECONDS = "E";
    public static final String READING = "R";
    public static final String WRITING = "W";
    public static final String IDLING = "I";

    // Position keys, x, y, w, o, m
    public static final String SCROLL_POSITION_TOP = "x";
    public static final String MAX_SCROLL_DEPTH = "m";
    public static final String CONTENT_HEIGHT = "y";
    public static final String SCROLL_WINDOW_HEIGHT = "w";
    public static final String DOCUMENT_WIDTH = "o";


    public static final String END_MARKER = "_";

    /* Optional params */
    /* One time */
    // Only include if there is no internal referrer
    public static final String EXTERNAL_REFERRER = "r";

    /* Per view visited */
    // Do not include external referrer if this is not blank
    public static final String INTERNAL_REFERRER = "v";

    public static final String VIEW_TITLE = "i";
    public static final String SECTION_G0 = "g0";
    public static final String AUTHOR_G1 = "g1";
    public static final String ZONE_G2 = "g2";

    public static final String LONGITUDE = "lg";
    public static final String LATITUDE = "lt";


    // User becomes old user after visiting an new internal view (has an internal referrer)
    public static final String IS_NEW_USER = "n";

    // Updates daily after visiting at least 1 view
    public static final String VISIT_FREQUENCY = "f";
}
