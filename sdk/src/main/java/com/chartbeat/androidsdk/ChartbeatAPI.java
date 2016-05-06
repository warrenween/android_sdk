package com.chartbeat.androidsdk;

import java.util.Map;

import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import rx.Observable;

/**
 * Created by Mike Dai Wang on 2016-02-05.
 */
interface ChartbeatAPI {
    String ENDPOINT = "http://ping.chartbeat.net";
    String HOST = "ping.chartbeat.net";

    @GET("ping")
    Observable<Response<Void>> ping(@QueryMap Map<String, String> parameters);
}
