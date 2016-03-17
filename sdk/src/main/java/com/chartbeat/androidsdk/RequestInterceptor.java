package com.chartbeat.androidsdk;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Mike Dai Wang on 2016-02-05.
 */
class RequestInterceptor implements Interceptor {
    private static final String TAG = RequestInterceptor.class.getSimpleName();

    private final String host;
    private final String userAgent;

    RequestInterceptor(String host, String userAgent) {
        this.host = host;
        this.userAgent = userAgent;
    }

    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        // Request customization: add request headers
        Request.Builder requestBuilder = original.newBuilder()
                .header("HOST", host)
                .header("User-Agent", userAgent)
                .method(original.method(), original.body());

        Request request = requestBuilder.build();
        return chain.proceed(request);
    }
}
