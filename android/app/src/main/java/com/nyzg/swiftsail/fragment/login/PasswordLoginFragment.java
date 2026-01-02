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
import com.nyzg.swiftsail.bean.GlobalConf;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.encrypt.Sha256;
import com.nyzg.swiftsail.listener.LoginWaitingListener;
import com.nyzg.swiftsail.netobj.PasswordVerifyReq;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PasswordLoginFragment extends Fragment {
    User user;
    private final static String USER_KEY = "pwdFraKey";
    private volatile boolean quitForbidden = false;

    static Fragment newInstance(User user) {
        Fragment fragment = new PasswordLoginFragment();
        if (user != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(USER_KEY, user);
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.user = getArguments().getParcelable(USER_KEY);
        }
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

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_password_login, container, false);
        TextView textView = father.findViewById(R.id.submitPwdVerify);
        TextInputEditText pwdInput = father.findViewById(R.id.pwdInput);
        textView.setOnTouchListener(new LoginWaitingListener(
                textView, () -> {
            String pwd = pwdInput.getText() != null ? pwdInput.getText().toString() : null;
            if (pwd == null || pwd.isEmpty()) {
                GlobalToast.COMMON_TOAST.accept("无效密码");
                return Optional.empty();
            }
            quitForbidden = true;
            try {
                OkHttpClient httpClient = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(GlobalConf.URL_LOGIN_WITH_PASSWORD)
                        .method(GlobalConf.POST,
                                RequestBody.create(
                                        JsonSerializer.serialize(new PasswordVerifyReq(user.getEmail(), Sha256.generateSha256ByteArray(pwd))),
                                        GlobalConf.APPLICATION_JSON
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
                //把这个更新用户
                LoginRepository.INSTANCE.updateUserToLocalAccountAsync(tokenUser);
                //成功登录
                LoginRepository.INSTANCE.login(tokenUser);
                //返回主界面
                GlobalInstance.mainHandler.post(() -> MainActivity.popUntilTheInitOne(requireActivity().getSupportFragmentManager()));
            } catch (Exception e) {
                GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
            }
        }));
        return father;
    }
}
