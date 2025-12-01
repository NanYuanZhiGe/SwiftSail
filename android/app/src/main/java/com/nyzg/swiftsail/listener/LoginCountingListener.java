package com.nyzg.swiftsail.listener;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.nyzg.swiftsail.bean.GlobalFunction;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.netobj.HttpResp;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import okhttp3.Response;

public class LoginCountingListener<T> implements View.OnTouchListener {
    private final int SECOND_RESEND = 5 * 60;
    private Runnable counting;
    private int currentSecond = SECOND_RESEND;
    private volatile boolean cancelCount = false;
    Consumer<HttpResp> handleSuccess;
    Function<T, Optional<Response>> handleSubmit;
    Supplier<Optional<T>> handleVerify;

    public LoginCountingListener(TextView self, Supplier<Optional<T>> handleVerify, Function<T, Optional<Response>> handleSubmit, Consumer<HttpResp> handleSuccess) {
        this.handleSubmit = handleSubmit;
        this.handleSuccess = handleSuccess;
        this.handleVerify = handleVerify;
        counting = () -> {
            if (cancelCount || currentSecond == 0) {
                cancelCount = false;
                counting = null;
                currentSecond = SECOND_RESEND;
                self.setText("点击获取邮箱验证码");
                return;
            }
            --currentSecond;
            self.setText(String.format("%ss", currentSecond));
            GlobalInstance.mainHandler.postDelayed(counting, 1000);
        };
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                view.setPressed(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                break;
            case MotionEvent.ACTION_UP:
                view.setPressed(false);
                Optional<T> result = handleVerify.get();
                if (!result.isPresent()) {
                    break;
                }
                //发送网络请求
                CompletableFuture.supplyAsync(() -> handleSubmit.apply(result.get()))
                        .thenAccept(action ->
                                GlobalFunction.handleNetResp(
                                        action.orElse(null),
                                        () -> cancelCount = true,
                                        handleSuccess)
                        );
                GlobalInstance.mainHandler.post(counting);
        }
        return true;
    }
}
