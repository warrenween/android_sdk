package com.chartbeat.androidsdk;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Random;

import retrofit2.Response;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Mike Dai Wang on 2016-02-05.
 */
final class PingService {
    private static final String TAG = PingService.class.getSimpleName();

    private static final boolean TEST_RANDOM_FAILURES = false;

    private ChartbeatAPI api;

    PingService(String userAgent) {
        PingClient client = new PingClient(ChartbeatAPI.ENDPOINT, ChartbeatAPI.HOST, userAgent);
        api = client.createService(ChartbeatAPI.class);
    }

    Observable<Integer> ping(final LinkedHashMap<String, String> queries) {
        if( TEST_RANDOM_FAILURES ) {
            Random random = new Random();
            int r = random.nextInt(6);
            if( r == 0 ) {
                Logger.w(TAG, "Simulating a fake 400 response." );
                return Observable.just(400);
            }
            if( r > 2 ) {
                Logger.w(TAG, "Simulating a fake 503 response." );
                return Observable.just(503);
            }
        }

        return api.ping(queries)
                .compose(this.<Response<Void>>applySchedulers())
                .map(new Func1<Response<Void>, Integer>() {
                    @Override
                    public Integer call(Response<Void> response) {
                        return response.code();
                    }
                });
    }

    <T> Observable.Transformer<T, T> applySchedulers() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> observable) {
                return observable.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io());
            }
        };
    }
}
