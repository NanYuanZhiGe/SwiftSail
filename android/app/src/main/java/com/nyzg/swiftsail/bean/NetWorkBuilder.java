package com.nyzg.swiftsail.bean;

import com.nyzg.swiftsail.repository.LoginRepository;

import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetWorkBuilder {
    /**
     * 自动给请求加上必须的请求头
     * 请不要在用户未登录的时候使用这个函数
     */
    public static <T> Request buildJsonRequestJwt(URL url, String method, T obj) {
        assert LoginRepository.getInstance().getCurrentUser().getValue() != null;
        return new Request.Builder()
                .url(url)
                .method(method, RequestBody.create(
                        JsonSerializer.serialize(obj), ServerURL.APPLICATION_JSON
                ))
                .addHeader("jwt-token", LoginRepository.getInstance().getCurrentUser().getValue().token)
                .build();
    }

    public static Response doChunkRequest(Request request) {
        try {
            OkHttpClient client = GlobalInstance.OK_HTTP_NO_PROXY;
            return client.newCall(request).execute();
        } catch (Exception e) {
            return null;
        }
    }
}