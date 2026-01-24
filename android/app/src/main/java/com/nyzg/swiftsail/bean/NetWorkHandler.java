package com.nyzg.swiftsail.bean;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.fragment.login.LoginFragment;
import com.nyzg.swiftsail.netobj.HttpResp;

import java.util.function.Consumer;

import okhttp3.Response;

public class NetWorkHandler {
    public static void handleNetRespBeforeLogin(Response response, Runnable onFail, Consumer<HttpResp> onSuccess) {
        if (response == null) {
            GlobalToast.SERVER_NOT_RESPONSE.run();
            onFail.run();
            return;
        }
        if (!response.isSuccessful()) {
            GlobalToast.RESPONSE_ERROR.accept(response.code());
            onFail.run();
            response.close();
            return;
        }
        try {
            HttpResp httpResp = JsonSerializer.deSerialize(response.body() != null ? response.body().string() : null, HttpResp.class);
            if (httpResp == null || !httpResp.isSuccess()) {
                GlobalToast.RESPONSE_NOT_SUCCESS.accept(httpResp == null ? null : httpResp.getMessage());
                onFail.run();
                response.close();
                return;
            }
            onSuccess.accept(httpResp);
        } catch (Exception e) {
            GlobalToast.CONTENT_UNACCEPTABLE.run();
        } finally {
            onFail.run();
            response.close();
        }
    }

    /**
     * 在当前线程处理网络请求
     * 如果用户的登录失效，弹出登录界面让用户登录/弹出提示告诉用户重新登录
     * 只有完全成功才会执行onSuccess，包括网络请求成功，状态码成功，content反序列化成功
     *
     * @param fragmentManager 设置为空就是消息提示
     * @param response        返回体，空表示请求没能够完成
     * @param onFail          请求失败的做法，包括内部的success字样为false
     * @param onSuccess       请求成功的做法
     * @param clz             content的类型，用于Json的反序列化
     * @param <T>             content的类型
     */
    public static <T> void handleNetRespAfterLogin(
            FragmentManager fragmentManager,
            Response response,
            @NonNull Runnable onFail,
            @NonNull Consumer<T> onSuccess,
            @NonNull Class<T> clz) {
        if (response == null) {
            GlobalToast.SERVER_NOT_RESPONSE.run();
            onFail.run();
            return;
        }
        //处理各种服务器异常
        //对于客户端异常，主要是401和403，403就提醒用户没有权限使用
        //401的话就提醒用户token过期，需要重新登录
        //如果是500类型或其他的错误，就Toast一下提醒用户就行了
        int statusCode = response.code();
        if (statusCode == 403) {
            GlobalToast.COMMON_TOAST.accept("您没有权限使用该功能");
            onFail.run();
            response.close();
            return;
        } else if (statusCode == 401) {
            GlobalToast.COMMON_TOAST.accept("登录已过期，请重新登录");
            onFail.run();
            response.close();
            if (fragmentManager == null) {
                GlobalToast.COMMON_TOAST.accept("登录已过期，请重新登录");
            } else {
                MainActivity.addFragmentToStackTop(fragmentManager, LoginFragment.newInstance());
            }
            return;
        } else if (statusCode == 404) {
            GlobalToast.COMMON_TOAST.accept("找不到服务404");
            onFail.run();
            response.close();
        } else if (statusCode >= 500) {
            GlobalToast.RESPONSE_ERROR.accept(statusCode);
            onFail.run();
            response.close();
            return;
        }
        //下面的都是合法请求
        //对于合法请求，也要处理里面的成功和失败情况
        T content;
        try {
            HttpResp httpResp = JsonSerializer.deSerialize(response.body() != null ? response.body().string() : null, HttpResp.class);
            if (httpResp == null || !httpResp.isSuccess()) {
                GlobalToast.RESPONSE_NOT_SUCCESS.accept(httpResp == null ? statusCode + "" : httpResp.getMessage());
                onFail.run();
                response.close();
                return;
            }
            content = JsonSerializer.mapToObject(httpResp.getContent(), clz).orElse(null);
            if (content == null && clz != Void.class) {
                GlobalToast.CONTENT_UNACCEPTABLE.run();
                onFail.run();
                response.close();
                return;
            }
        } catch (Exception e) {
            GlobalToast.CONTENT_UNACCEPTABLE.run();
            onFail.run();
            response.close();
            return;
        }
        onSuccess.accept(content);
        response.close();
    }
}