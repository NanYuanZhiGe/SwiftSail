package com.nyzg.swiftsail.bean;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nyzg.swiftsail.repository.LoginRepository;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http.HttpHeaders;

public class NetWorkBuilder {
    /**
     * 自动给请求加上必须的请求头
     * 请不要在用户未登录的时候使用这个函数
     */
    public static <T> Request buildJsonRequestJwt(URL url, String method, T obj) {
        assert LoginRepository.getInstance().getCurrentUser().getValue() != null;
        if (method.equals(ServerURL.GET)){
            return new Request.Builder()
                    .url(url)
                    .addHeader("jwt-token", LoginRepository.getInstance().getCurrentUser().getValue().token)
                    .get()
                    .build();
        }
        return new Request.Builder()
                .url(url)
                .method(method, obj==null?RequestBody.create("{}",ServerURL.APPLICATION_JSON):RequestBody.create(
                        MyJsonSerializer.serialize(obj), ServerURL.APPLICATION_JSON
                ))
                .addHeader("jwt-token", LoginRepository.getInstance().getCurrentUser().getValue().token)
                .build();
    }

    public static Response doChunkRequest(@NonNull Request request) {
        try {
            OkHttpClient client = GlobalInstance.OK_HTTP_NO_PROXY;
            return client.newCall(request).execute();
        } catch (Exception e) {
            return null;
        }
    }


    public static CompletableFuture<Response> doChunkRequestAsync(@NonNull Request request){
        return CompletableFuture.supplyAsync(()-> doChunkRequest(request));
    }
}