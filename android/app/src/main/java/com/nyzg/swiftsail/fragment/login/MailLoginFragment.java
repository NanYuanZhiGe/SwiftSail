package com.nyzg.swiftsail.fragment.login;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.listener.LoginWaitingListener;
import com.nyzg.swiftsail.listener.LoginCountingListener;
import com.nyzg.swiftsail.netobj.login.MailCodeVerifyReq;
import com.nyzg.swiftsail.netobj.login.MailVerifyReq;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class MailLoginFragment extends Fragment {
    private String email;
    private static final String EMAIL_KEY = "user_email";
    private volatile boolean quitForbidden = false;

    public static Fragment newInstance() {
        return new MailLoginFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.email = getArguments() != null ? getArguments().getString(EMAIL_KEY) : null;
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!quitForbidden) {//如果没有在登录，就允许用户返回
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                //如果用户正在登录，就提示用户等待一会
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您正在尝试登录，请耐心等待（不会超过30秒钟）！")
                        .setPositiveButton("确定", (dialog, which) -> {
                            setEnabled(false);//禁用自己，避免无限递归
                        })
                        .show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_mail_login, container, false);
        TextView getMailCode = father.findViewById(R.id.getMailCode);
        onGetMailCodeClick(getMailCode);
        TextView submitMailVerify = father.findViewById(R.id.submitMailVerify);
        onSubmitMailVerifyClick(submitMailVerify, father.findViewById(R.id.mailInput));
        return father;
    }

    //获取邮箱验证码的点击事件
    @SuppressLint("ClickableViewAccessibility")
    private void onGetMailCodeClick(TextView textView) {
        textView.setOnTouchListener(new LoginCountingListener<>(
                textView, () -> Optional.of(email), strEmail -> {
            try {
                OkHttpClient httpClient = GlobalInstance.OK_HTTP_NO_PROXY;
                Request request = new Request.Builder()
                        .url(ServerURL.URL_LOGIN_VERIFY_CODE)
                        .method("POST",
                                RequestBody.create(
                                        JsonSerializer.serialize(new MailVerifyReq(strEmail)),
                                        ServerURL.APPLICATION_JSON
                                ))
                        .build();
                return Optional.of(httpClient.newCall(request).execute());
            } catch (Exception e) {
                return Optional.empty();
            }
        }, success -> GlobalToast.COMMON_TOAST.accept("验证码已发送")));
    }

    //提交验证码的逻辑
    @SuppressLint("ClickableViewAccessibility")
    private void onSubmitMailVerifyClick(TextView textView, TextInputEditText inputEditText) {
        textView.setOnTouchListener(new LoginWaitingListener(
                textView, () -> {
            String verifyCode = inputEditText.getText() != null ? inputEditText.getText().toString() : null;
            if (verifyCode == null || verifyCode.length() != 6 || !verifyCode.matches("\\d+")) {
                GlobalToast.COMMON_TOAST.accept("验证码格式不正确");
                return Optional.empty();
            }
            quitForbidden = true;
            try {
                OkHttpClient httpClient = GlobalInstance.OK_HTTP_NO_PROXY;
                Request request = new Request.Builder()
                        .url(ServerURL.URL_LOGIN_WITH_MAIL)
                        .method(ServerURL.POST, RequestBody.create(
                                JsonSerializer.serialize(new MailCodeVerifyReq(email, verifyCode)),
                                ServerURL.APPLICATION_JSON
                        ))
                        .build();
                return Optional.of(httpClient.newCall(request).execute());
            } catch (Exception e) {
                return Optional.empty();
            }
        }, resp -> {
            quitForbidden = false;
            if (!resp.isSuccess()) {
                GlobalToast.COMMON_TOAST.accept("请求失败：" + resp.getMessage());
                return;
            }
            try {
                User tokenUser = JsonSerializer.mapToObject(resp.getContent(), User.class).orElse(null);
                if (tokenUser == null) {
                    GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                    return;
                }
                //把这个注册用户写入数据库
                LoginRepository.getInstance().updateUserToLocalAccountAsync(tokenUser);
                //成功登录
                LoginRepository.getInstance().login(tokenUser);
                //返回主界面
                GlobalInstance.mainHandler.post(() -> MainActivity.popUntilTheInitOne(requireActivity().getSupportFragmentManager()));
            } catch (Exception e) {
                GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
            }
        }));
    }
}
