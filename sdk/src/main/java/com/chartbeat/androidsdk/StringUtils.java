package com.chartbeat.androidsdk;

import java.util.Collection;

/**
 * Created by Mike Dai Wang on 2016-02-04.
 */
final class StringUtils {
    static String collectionToCommaString(Collection<String> col) {
        if (col == null || col.size() == 0) {
            return null;
        }
        // there shouldn't usually be too many elements in our collection,
        // so not using a string builder is probably appropriate here.
        String ret = "";
        int i = 0;
        for (String s : col) {
            ret += s.replaceAll(",", "");
            ++i;
            if (i != col.size())
                ret += ",";
        }
        return ret;
    }
}
