package com.nyzg.swiftsail.listener;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.nyzg.swiftsail.bean.GlobalFunction;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.netobj.HttpResp;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import okhttp3.Response;

public class LoginWaitingListener implements View.OnTouchListener {
    Consumer<HttpResp> handleSuccess;
    Supplier<Optional<Response>> handleNetwork;
    private volatile boolean isWaitingNetwork = false;
    private final String[] WAITING_TEXT = {"等待.", "等待..", "等待..."};
    private int pointer = 0;
    private volatile boolean cancelCount = false;
    Runnable r;

    public LoginWaitingListener(TextView self, Supplier<Optional<Response>> handleNetwork, Consumer<HttpResp> handleSuccess) {
        this.handleNetwork = handleNetwork;
        this.handleSuccess = handleSuccess;
        r = () -> {
            if (cancelCount || !isWaitingNetwork) {
                self.setText("登录");
                return;
            }
            self.setText(WAITING_TEXT[pointer]);
            ++pointer;
            if (pointer == WAITING_TEXT.length) {
                pointer = 0;
            }
            GlobalInstance.mainHandler.postDelayed(r, 1000);
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
                if (isWaitingNetwork) {
                    break;
                }
                isWaitingNetwork = true;
                cancelCount = false;
                CompletableFuture.supplyAsync(() -> handleNetwork.get()).thenAccept(action -> {
                    isWaitingNetwork = false;
                    GlobalFunction.handleNetResp(action.orElse(null), () -> cancelCount = true, handleSuccess);
                });
                GlobalInstance.mainHandler.post(r);
        }
        return true;
    }
}
