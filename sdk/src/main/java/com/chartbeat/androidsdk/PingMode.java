package com.chartbeat.androidsdk;

import java.util.HashSet;
import java.util.Set;

/**
 * @author bjorn
 * @author Mike Dai Wang
 */
enum PingMode {
    FIRST_PING,
    STANDARD_PING,
    FULL_PING,
    REPEAT_PING_AFTER_CODE_500;

    private static final Set<String> MANDATORY_PARAMETERS = new HashSet<String>();

    static {
        MANDATORY_PARAMETERS.add(QueryKeys.HOST);
        MANDATORY_PARAMETERS.add(QueryKeys.SUBDOMAIN);
        MANDATORY_PARAMETERS.add(QueryKeys.VIEW_ID);
        MANDATORY_PARAMETERS.add(QueryKeys.TOKEN);
        MANDATORY_PARAMETERS.add(QueryKeys.USER_ID);
        MANDATORY_PARAMETERS.add(QueryKeys.ACCOUNT_ID);
        MANDATORY_PARAMETERS.add(QueryKeys.TIME_ON_VIEW_IN_MINUTES);
        MANDATORY_PARAMETERS.add(QueryKeys.DECAY);
        MANDATORY_PARAMETERS.add(QueryKeys.ENGAGED_SECONDS);
        MANDATORY_PARAMETERS.add(QueryKeys.READING);
        MANDATORY_PARAMETERS.add(QueryKeys.WRITING);
        MANDATORY_PARAMETERS.add(QueryKeys.IDLING);
        MANDATORY_PARAMETERS.add(QueryKeys.SCROLL_POSITION_TOP);
        MANDATORY_PARAMETERS.add(QueryKeys.MAX_SCROLL_DEPTH);
        MANDATORY_PARAMETERS.add(QueryKeys.CONTENT_HEIGHT);
        MANDATORY_PARAMETERS.add(QueryKeys.SCROLL_WINDOW_HEIGHT);
        MANDATORY_PARAMETERS.add(QueryKeys.DOCUMENT_WIDTH);
        MANDATORY_PARAMETERS.add(QueryKeys.END_MARKER);
    }

    boolean includeParameter(final String parameter) {
        switch (this) {
            case FIRST_PING:
            case FULL_PING:
                return true;
            case STANDARD_PING:
                return MANDATORY_PARAMETERS.contains(parameter);
            case REPEAT_PING_AFTER_CODE_500:
                return !parameter.equals(QueryKeys.FORCE_DECAY);
            default:
                throw new RuntimeException("Invalid Ping Mode.");
        }
    }

    PingMode next() {
        switch (this) {
            case FIRST_PING:
            case FULL_PING:
            case STANDARD_PING:
                return STANDARD_PING;
            case REPEAT_PING_AFTER_CODE_500:
                return STANDARD_PING;
            default:
                throw new RuntimeException("Invalid Ping Mode.");
        }
    }
};
