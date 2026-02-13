package com.nyzg.swiftsail.bean;

import android.os.Handler;
import android.os.Looper;

import com.nyzg.swiftsail.dbobj.User;

import java.net.Proxy;
import java.time.Duration;

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
    public final static Handler mainHandler = new Handler(Looper.getMainLooper());

    static public final User LOCAL_USER = new User();

    static {
        LOCAL_USER.id = 0L;
        LOCAL_USER.nickName = "本地账户";
        LOCAL_USER.token = "0.0.0";
    }
}