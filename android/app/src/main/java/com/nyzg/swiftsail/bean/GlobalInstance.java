package com.nyzg.swiftsail.bean;

import android.os.Handler;
import android.os.Looper;

import com.nyzg.swiftsail.dbobj.User;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class GlobalInstance {
    public final static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(30))
            .build();
    public final static Handler mainHandler = new Handler(Looper.getMainLooper());

    static public final User LOCAL_USER = new User();

    static public final Executor EXECUTOR= Executors.newCachedThreadPool();

    static {
        LOCAL_USER.setId(0L);
        LOCAL_USER.setNickName("本地账户");
        LOCAL_USER.setToken("0-0");
    }
}
