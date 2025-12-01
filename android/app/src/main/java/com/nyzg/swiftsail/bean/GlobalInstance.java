package com.nyzg.swiftsail.bean;

import android.os.Handler;
import android.os.Looper;

import com.nyzg.swiftsail.dbobj.User;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;

public class GlobalInstance {
    public final static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(15))
            .build();
    public final static Handler mainHandler = new Handler(Looper.getMainLooper());

    static public AtomicReference<String> token = new AtomicReference<>(null);
    static public AtomicReference<User> currentUser = new AtomicReference<>(null);
}
