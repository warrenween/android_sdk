package com.chartbeat.androidsdk;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

/**
 * Created by Mike Dai Wang on 2016-02-05.
 */
class PingClient {
    private static String TAG  = PingClient.class.getSimpleName();

    private Retrofit retrofit;

    PingClient(final String endpoint, final String host, final String userAgent) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        clientBuilder.interceptors().add(getLoggingInterceptor());
        clientBuilder.interceptors().add(new RequestInterceptor(host, userAgent));
        OkHttpClient httpClient = clientBuilder.build();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(endpoint)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create());

        retrofit = builder.client(httpClient).build();
    }

    <S> S createService(Class<S> serviceClass) {
        return retrofit.create(serviceClass);
    }

    private static HttpLoggingInterceptor getLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Logger.d(TAG, message);
            }
        });

        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return httpLoggingInterceptor;
    }
}
