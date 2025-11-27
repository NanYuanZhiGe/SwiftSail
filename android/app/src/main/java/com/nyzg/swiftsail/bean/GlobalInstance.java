package com.nyzg.swiftsail.bean;

import java.time.Duration;

import okhttp3.OkHttpClient;

public class GlobalInstance {
    public final static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(15))
            .build();

}
