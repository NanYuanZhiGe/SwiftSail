package com.nyzg.swiftsail.fragment.login;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalConf;
import com.nyzg.swiftsail.bean.GlobalFunction;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.encrypt.Biometric;
import com.nyzg.swiftsail.netobj.BiometricUser;
import com.nyzg.swiftsail.netobj.KeyVerifyReq;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


import javax.crypto.Cipher;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class KeyLoginFragment extends Fragment {
    String email;
    private final static String EMAIL_KEY = "keyLogin_key";
    private volatile boolean quitForbidden = false;
    private BiometricPrompt prompt;
    private BiometricPrompt.PromptInfo promptInfo;
    public static final String KEY_NAME = "SWIFT_SAIL_BIOMETRIC_KEY";
    public static final String PREFS_NAME = "SWIFT_SAIL_PREFS_NAME";

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
        View father = inflater.inflate(R.layout.fragment_key_login, container, false);
        TextView textView = father.findViewById(R.id.doFingerVerify);
        boolean isHardWareSupport = Biometric.isBiometricAvailable(requireContext());
        if (!isHardWareSupport) {//硬件不支持
            textView.setText("您的设备不支持指纹登录");
        } else {
            //点击文字进行指纹验证登录
            initBiometricCheck();
            textView.setOnClickListener(v -> doBiometricCheck());
        }
        return father;
    }

    private void initBiometricCheck() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        prompt = new BiometricPrompt(requireActivity(), executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                GlobalToast.COMMON_TOAST.accept("验证成功，等待服务器校验中");
                /*
                if (result.getCryptoObject() == null || result.getCryptoObject().getCipher() == null) {
                    GlobalToast.COMMON_TOAST.accept("验证成功，但是无法获取解码器");
                    quitForbidden = false;
                    return;
                }
                 */
                //从设备获取到加密的token，向服务进行验证
                //返回的结果是如下：
                //success , newToken
                //如果成功，newToken不为空，失败newToken为空
                //如果成功就把newToken存储到设备里面，
                //如果为空就提醒用户使用验证码或者是密码的方式进行登录

                //先获取token
                String token = Biometric.getLastData(
                        Biometric.getCipher(),
                        PREFS_NAME, KEY_NAME, requireContext());
                if (token == null) {
                    GlobalToast.COMMON_TOAST.accept("无法获取旧凭证，请尝试其他登录方式");
                    quitForbidden = false;
                    return;
                }
                //然后进行网络请求验证token
                CompletableFuture.supplyAsync(() -> {
                    try {
                        KeyVerifyReq keyVerifyReq = new KeyVerifyReq();
                        keyVerifyReq.email = KeyLoginFragment.this.email;
                        keyVerifyReq.token = token;
                        keyVerifyReq.deviceId = Biometric.getDeviceIdHash(requireContext());
                        if (keyVerifyReq.deviceId == null) {
                            GlobalToast.COMMON_TOAST.accept("您的设备不存在设备id！");
                            return Optional.empty();
                        }
                        OkHttpClient httpClient = GlobalInstance.okHttpClient;
                        Request request = new Request.Builder()
                                .url(new URL(GlobalConf.URL_LOGIN_WITH_KEY))
                                .method(GlobalConf.POST,
                                        RequestBody.create(JsonSerializer.serialize(keyVerifyReq),
                                                GlobalConf.APPLICATION_JSON))
                                .build();
                        return Optional.of(httpClient.newCall(request).execute());
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }).thenAccept(
                        netRes -> GlobalFunction.handleNetResp((Response) netRes.orElse(null),
                                () -> {//网络请求成功，但是失败，里面会自动toast错误内容，这里只需要允许用户退出就行
                                    quitForbidden = false;
                                },
                                resp -> {
                                    quitForbidden = false;
                                    try {
                                        BiometricUser user = JsonSerializer.mapToObject(resp.getContent(), BiometricUser.class).orElse(null);
                                        if (user == null) {
                                            GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                                            return;
                                        }
                                        //登录成功后，除了要做其他的事情外，还需要刷新登录token
                                        User innerUser = user.tokenUser;
                                        String newBioToken = user.token;
                                        //刷新内部的token
                                        Biometric.putData(newBioToken, PREFS_NAME, KEY_NAME, requireContext());
                                        //把这个用户写入数据库
                                        LoginRepository.INSTANCE.updateUserToLocalAccountAsync(innerUser);
                                        //成功登录
                                        LoginRepository.INSTANCE.login(innerUser);
                                        //返回主界面
                                        ContextCompat.getMainExecutor(GlobalApplication.getAppContext()).execute(() -> MainActivity.popUntilTheInitOne(requireActivity().getSupportFragmentManager()));
                                    } catch (Exception e) {
                                        GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                                    }
                                }
                        )
                );
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                GlobalToast.COMMON_TOAST.accept("验证失败");
                quitForbidden = false;
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                GlobalToast.COMMON_TOAST.accept("验证出错");
                quitForbidden = false;
            }
        });
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("运动轻舟登录")
                .setSubtitle("指纹验证登录")
                .setNegativeButtonText("取消")
                .build();
    }

    public void doBiometricCheck() {
        quitForbidden = true;
        prompt.authenticate(promptInfo);
    }
}
