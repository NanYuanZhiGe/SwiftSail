package com.nyzg.swiftsail.bean;


import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.nyzg.swiftsail.GlobalApplication;

import java.util.function.Consumer;


public class GlobalToast {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    public static final Runnable SERVER_NOT_RESPONSE = () -> handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), "服务器无响应", Toast.LENGTH_LONG).show());

    public static final Runnable SERVER_RESP_UNACCEPTABLE = () -> handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), "服务端返回的数据不符合API规范", Toast.LENGTH_LONG).show());
    public static final Consumer<Integer> RESPONSE_ERROR = code -> handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), "响应错误，状态码：" + code, Toast.LENGTH_LONG).show());
    public static final Consumer<String> RESPONSE_NOT_SUCCESS = msg -> handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), String.format("请求不被允许：%s", msg), Toast.LENGTH_LONG).show());
    public static final Runnable CONTENT_UNACCEPTABLE = () -> handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), "无法解析服务端返回的内容", Toast.LENGTH_LONG).show());

    public static final Consumer<String> COMMON_TOAST = msg -> {
        if (msg == null) {
            return;
        }
        handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), msg, Toast.LENGTH_LONG).show());
    };
    public static final Consumer<String> RESPONSE_SUCCESS = msg -> {
        if (msg == null) {
            handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), "获取验证码成功", Toast.LENGTH_LONG).show());
            return;
        }
        handler.post(() -> Toast.makeText(GlobalApplication.getAppContext(), String.format("获取验证码成功，信息：%s", msg), Toast.LENGTH_LONG).show());
    };
}
