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

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.listener.LoginCountingListener;

import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * 指纹登录的逻辑如下：
 * ---
 * 1. 首先校验用户的设备是否支持指纹登录
 * 2. 然后检查权限，向用户申请权限
 * 3. 检查是否有私钥，如果没有就提醒用户去用户界面添加指纹，返回
 * 4. 进行指纹验证
 * 5. 向服务端申请challenge，签名后返回
 * 6. 服务端返回结果，解析反馈给用户
 */
public class KeyLoginFragment extends Fragment {
    String email;
    private final static String EMAIL_KEY = "keyLogin_key";

    public static Fragment newInstance(String email) {
        Fragment fragment = new KeyLoginFragment();
        if (email != null) {
            Bundle bundle = new Bundle();
            bundle.putString(EMAIL_KEY, email);
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.email = getArguments().getString(EMAIL_KEY);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_key_login, container, false);
        TextView textView = father.findViewById(R.id.doFingerVerify);
        textView.setOnTouchListener(new LoginCountingListener<String>(
                textView, () -> {
            return Optional.of(this.email);
        }, strEmail -> {
            try {
                OkHttpClient httpClient = GlobalInstance.okHttpClient;
                Request request = new Request.Builder().build();
                return Optional.of(httpClient.newCall(request).execute());
            } catch (Exception e) {
                return Optional.empty();
            }
        }, success -> {

        }));
        return father;
    }
}
