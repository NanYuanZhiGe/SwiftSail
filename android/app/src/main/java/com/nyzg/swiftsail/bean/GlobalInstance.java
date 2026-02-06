package com.nyzg.swiftsail.bean;

import android.os.Handler;
import android.os.Looper;

import com.nyzg.swiftsail.dbobj.User;

import java.net.Proxy;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class GlobalInstance {
    /**
     * 普通请求必须禁止使用系统代理，不然Cloudfare会阻止请求
     */
    public final static OkHttpClient OK_HTTP_NO_PROXY = new OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(60))
            .build();
    /**
     * 对于所有外面的请求，比如fitbit就需要使用有代理情况的http客户端
     */
    public final static OkHttpClient OK_HTTP_PROXY = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(30))
            .build();

    public final static Handler mainHandler = new Handler(Looper.getMainLooper());

    static public final User LOCAL_USER = new User();

    static {
        LOCAL_USER.id = 0L;
        LOCAL_USER.nickName = "本地账户";
        LOCAL_USER.token = "0:0:0";
    }
}