package com.nyzg.swiftsail.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.adapter.AccountListAdapter;
import com.nyzg.swiftsail.bean.GlobalConf;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.bean.MatchUtils;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.HttpResp;
import com.nyzg.swiftsail.netobj.MailVerifyReq;
import com.nyzg.swiftsail.netobj.RegisterVerifyMailReq;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    List<User> userList;

    //！！！！！！！！
    //这个代码这里式有问题的，需要用静态方法+工厂方法来保证它的正确
    //不然用户旋转了屏幕之后应用及进行重建，这里就会有问题
    public LoginFragment(List<User> userList) {
        this.userList = userList;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您还没有登录，要回到主界面吗")
                        .setPositiveButton("确定", (dialog, which) -> {
                            setEnabled(false);//禁用自己，避免无限递归
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        })
                        .setNegativeButton("取消", null) // 取消则什么也不做
                        .show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        if (userList == null) {
            switchToRegister(view);
        } else {
            putUserIntoRecycleView(view.findViewById(R.id.accountList), userList);
            switchToLogin(view);
        }
        onClickGetVerifyCode(view);
        onClickSubmitRegister(view);
        onClickChangeMainPage(view);
        return view;
    }


    @SuppressLint("ClickableViewAccessibility")
    private void onClickSubmitRegister(View father) {
        //点击按钮注册用户点击事件逻辑
        TextView registerButton = father.findViewById(R.id.registerButton);
        registerButton.setOnTouchListener(new View.OnTouchListener() {
            private Runnable waiting = null;
            private final AtomicReference<Boolean> stopWaiting = new AtomicReference<>(false);
            private int pointer = 0;
            private final String[] waitingStirs = {"等待中.", "等待中..", "等待中..."};

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                    return true;
                }
                if (waiting != null) {
                    return true;
                }
                RegisterVerifyMailReq req = new RegisterVerifyMailReq();
                TextInputEditText nickNameInput = father.findViewById(R.id.registerNickName);
                String nickName = nickNameInput.getText() != null ? nickNameInput.getText().toString() : null;
                if (nickName == null || nickName.isEmpty() || nickName.length() > 32) {
                    Toast.makeText(GlobalApplication.getAppContext(), "昵称不能为空，最大长度为32个字符", Toast.LENGTH_LONG).show();
                    return true;
                }
                TextInputEditText emailInput = father.findViewById(R.id.registerEmail);
                String email = emailInput.getText() != null ? emailInput.getText().toString() : null;
                if (MatchUtils.isMailAddrIllegal(email)) {
                    Toast.makeText(GlobalApplication.getAppContext(), "邮箱不合法", Toast.LENGTH_LONG).show();
                    return true;
                }
                TextInputEditText passwordInput = father.findViewById(R.id.registerPassword);
                String password = passwordInput.getText() != null ? passwordInput.getText().toString() : null;
                if (password == null || password.isEmpty()) {
                    Toast.makeText(GlobalApplication.getAppContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
                    return true;
                }
                TextInputEditText verifyInput = father.findViewById(R.id.registerVerifyCode);
                String verifyCode = verifyInput.getText() != null ? verifyInput.getText().toString() : null;
                if (verifyCode == null || verifyCode.length() != 6) {
                    Toast.makeText(GlobalApplication.getAppContext(), "请输入6位数验证码", Toast.LENGTH_LONG).show();
                    return true;
                }
                req.setNickName(nickName);
                req.setEmail(email);
                try {
                    MessageDigest digest = MessageDigest.getInstance("sha-256");
                    req.setSecretWord(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
                } catch (Exception e) {
                    Toast.makeText(GlobalApplication.getAppContext(), "您的手机无法运行该应用，不支持sha256算法", Toast.LENGTH_LONG).show();
                    return true;
                }
                req.setCode(verifyCode);
                waiting = () -> {
                    if (stopWaiting.get()) {
                        registerButton.setText("点击进行注册");
                        waiting = null;
                        return;
                    }
                    registerButton.setText(waitingStirs[pointer]);
                    ++pointer;
                    if (pointer == waitingStirs.length) {
                        pointer = 0;
                    }
                    handler.postDelayed(waiting, 1000);
                };
                handler.post(waiting);
                CompletableFuture.supplyAsync(() -> {
                    try {
                        OkHttpClient httpClient = GlobalInstance.okHttpClient;
                        Request request = new Request.Builder()
                                .url(new URL(GlobalConf.URL_REGISTER_SUBMIT))
                                .method("POST", RequestBody.create(JsonSerializer.serialize(req), MediaType.get("application/json;charset=utf-8")))
                                .build();
                        return Optional.of(httpClient.newCall(request).execute());
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }).thenAccept(result -> {
                    stopWaiting.set(true);
                    handleNetResp((Response) result.orElse(null), resp -> {
                        GlobalToast.RESPONSE_SUCCESS.accept(resp.getMessage());
                        //保存JWT的信息

                    });
                });
                return true;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onClickGetVerifyCode(View view) {
        //点击按钮获取验证码点击事件逻辑
        TextView verifyCodeButton = view.findViewById(R.id.getVerifyNumber);
        TextInputEditText mailAddr = view.findViewById(R.id.registerEmail);
        verifyCodeButton.setOnTouchListener(new View.OnTouchListener() {
            private final AtomicReference<Runnable> counting = new AtomicReference<>(null);
            private final int MAX_TIME = 5;//5*60
            private int timeLeft = MAX_TIME;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                    return true;
                }
                view.setPressed(true);
                String mail = mailAddr.getText() != null ? mailAddr.getText().toString() : null;
                if (mail == null || mail.isEmpty() || MatchUtils.isMailAddrIllegal(mail)) {
                    Toast.makeText(GlobalApplication.getAppContext(), "邮箱格式不正确", Toast.LENGTH_LONG).show();
                    view.setPressed(false);
                    return true;
                }
                if (counting.get() != null) {
                    return true;
                }
                CompletableFuture.supplyAsync(() -> {
                    OkHttpClient okHttpClient = GlobalInstance.okHttpClient;
                    Request request = new Request.Builder()
                            .url(GlobalConf.URL_REGISTER_VERIFY_CODE)
                            .method("POST", RequestBody.create(JsonSerializer.serialize(new MailVerifyReq(mail)), MediaType.get("application/json;charset=utf-8")))
                            .build();
                    try {
                        return Optional.of(okHttpClient.newCall(request).execute());
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }).thenAccept(result -> handleNetResp(((Response) result.orElse(null)), resp -> GlobalToast.RESPONSE_SUCCESS.accept(resp.getMessage())));
                counting.set(() -> {
                    if (timeLeft == 0) {
                        timeLeft = MAX_TIME;
                        verifyCodeButton.setText("点我获取验证码");
                        counting.set(null);
                        verifyCodeButton.setPressed(false);
                        return;
                    }
                    --timeLeft;
                    verifyCodeButton.setText(String.format("%ss", timeLeft));
                    handler.postDelayed(counting.get(), 1000);
                });
                handler.post(counting.get());
                return true;
            }
        });
    }

    private void onClickChangeMainPage(View view) {
        //点击切换登录和注册的点击事件逻辑
        View registerLayout = view.findViewById(R.id.registerLayout);
        View accountList = view.findViewById(R.id.accountList);
        TextView loginText = view.findViewById(R.id.loginText);
        view.findViewById(R.id.changeLoginText).setOnClickListener(v -> {
            if (loginText.getText().equals("注册")) {
                LoginFragment.this.switchToLogin(view);
                registerLayout.setVisibility(View.GONE);
                accountList.setVisibility(View.VISIBLE);
                return;
            }
            LoginFragment.this.switchToRegister(view);
            accountList.setVisibility(View.GONE);
            registerLayout.setVisibility(View.VISIBLE);
        });
    }

    private void switchToLogin(View view) {
        TextView loginText = view.findViewById(R.id.loginText);
        loginText.setText("登录");
        TextView changeLoginText = view.findViewById(R.id.changeLoginText);
        changeLoginText.setText("| 点我进行注册");
        view.findViewById(R.id.registerLayout).setVisibility(View.GONE);
        view.findViewById(R.id.accountList).setVisibility(View.VISIBLE);
    }

    private void switchToRegister(View view) {
        TextView loginText = view.findViewById(R.id.loginText);
        loginText.setText("注册");
        TextView changeLoginText = view.findViewById(R.id.changeLoginText);
        changeLoginText.setText("| 点我进行登录");
        view.findViewById(R.id.registerLayout).setVisibility(View.VISIBLE);
        view.findViewById(R.id.accountList).setVisibility(View.GONE);
    }

    private void putUserIntoRecycleView(RecyclerView recyclerView, List<User> userList) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(new AccountListAdapter(userList));
    }

    private void handleNetResp(Response response, Consumer<HttpResp> onSuccess) {
        if (response == null) {
            GlobalToast.SERVER_NOT_RESPONSE.run();
            return;
        }
        if (!response.isSuccessful()) {
            GlobalToast.RESPONSE_ERROR.accept(response.code());
            response.close();
            return;
        }
        try {
            HttpResp httpResp = JsonSerializer.deSerialize(response.body() != null ? response.body().string() : null, HttpResp.class);
            if (httpResp == null || !httpResp.isSuccess()) {
                GlobalToast.RESPONSE_NOT_SUCCESS.accept(httpResp == null ? null : httpResp.getMessage());
                response.close();
                return;
            }
            onSuccess.accept(httpResp);
        } catch (Exception e) {
            GlobalToast.CONTENT_UNACCEPTABLE.run();
        } finally {
            response.close();
        }
    }
}
