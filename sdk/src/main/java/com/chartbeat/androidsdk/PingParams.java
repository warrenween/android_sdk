package com.chartbeat.androidsdk;

import java.util.HashSet;
import java.util.Set;

/**
 * @author bjorn
 * @author Mike Dai Wang
 */
final class PingParams {
    private static final String TAG = PingParams.class.getSimpleName();

    Set<String> oneTimeKeys = new HashSet<String>();
    PingMode pingMode;

    PingParams() {
        pingMode = PingMode.FIRST_PING;
    }

    void addOneTimeParameter(String k) {
        oneTimeKeys.add(k);
    }

    void newView() {
        pingMode = PingMode.FULL_PING;
    }

    boolean includeParameter(final String parameter) {
        return oneTimeKeys.contains(parameter) || pingMode.includeParameter(parameter);
    }

    void pingComplete(int code) {
        if (code == 500) {
            pingMode = PingMode.REPEAT_PING_AFTER_CODE_500;
        } else if (code == 400) {
            pingMode = PingMode.FULL_PING;
        } else {
            pingMode = pingMode.next();
            oneTimeKeys.clear();
        }
    }

    void pingError() {
        pingMode = PingMode.FULL_PING;
    }

    void pingReset() {
        oneTimeKeys.clear();
        pingMode = PingMode.FULL_PING;
    }
}
