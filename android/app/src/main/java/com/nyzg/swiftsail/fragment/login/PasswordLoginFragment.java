package com.nyzg.swiftsail.fragment.login;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalConf;
import com.nyzg.swiftsail.bean.GlobalFunction;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.encrypt.Sha256;
import com.nyzg.swiftsail.listener.LoginWaitingListener;
import com.nyzg.swiftsail.netobj.PasswordVerifyReq;

import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PasswordLoginFragment extends Fragment {
    User user;
    private final static String USER_KEY = "pwdFraKey";

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
        }, resp -> GlobalFunction.onSuccessLoginSync(user.getEmail(), resp.getContent())));
        return father;
    }
}
