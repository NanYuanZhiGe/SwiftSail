package com.nyzg.swiftsail.fragment.mine;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.encrypt.Biometric;
import com.nyzg.swiftsail.encrypt.Sha256;
import com.nyzg.swiftsail.fragment.login.KeyLoginFragment;
import com.nyzg.swiftsail.netobj.login.BiometricAddReq;
import com.nyzg.swiftsail.netobj.login.BiometricAddResp;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MineSecurityFragment extends Fragment {
    public static Fragment getInstance() {
        return new MineSecurityFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_mine_secuity, container, false);
        View changePassword = father.findViewById(R.id.changePassword);
        ((TextView) changePassword.findViewById(R.id.content)).setText("");
        View changeEmail = father.findViewById(R.id.changeEmail);
        ((TextView) changeEmail.findViewById(R.id.content)).setText("");
        View addBiometric = father.findViewById(R.id.addBiometric);
        ((TextView) addBiometric.findViewById(R.id.content)).setText("");
        father.findViewById(R.id.addBiometric).setOnClickListener(this::onAddBiometricClicked);
        return father;
    }

    private void onAddBiometricClicked(View view) {
        //里面会自动Toast
        if (!Biometric.isBiometricAvailable(requireContext())) {
            return;
        }
        assert this.getActivity() != null;
        LayoutInflater inflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MyPopupWindow popupWindow = new MyPopupWindow(
                inflater.inflate(R.layout.layout_mine_security_add_biometric, null, false)
                , ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                true, requireActivity());
        popupWindow.setOutsideTouchable(false);
        popupWindow.setClippingEnabled(false);
        popupWindow.showAtLocation(this.getView(), Gravity.CENTER, 0, 0);
    }
}

class MyPopupWindow extends PopupWindow {
    View itemView;
    TextView validate;
    TextView cancel;
    TextInputEditText pwdInput;
    private volatile boolean cancelAll = false;
    private boolean isLastRunning = false;
    private FragmentActivity activity;

    public MyPopupWindow(View inflate, int wrapContent, int wrapContent1, boolean b, FragmentActivity activity) {
        super(inflate, wrapContent, wrapContent1, b);
        this.activity = activity;
        this.itemView = inflate;
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        validate = itemView.findViewById(R.id.verify);
        pwdInput = itemView.findViewById(R.id.pwdInput);
        cancel = itemView.findViewById(R.id.cancel);
        validate.setOnClickListener(this::onValidateClicked);
        cancel.setOnClickListener(this::onCancelClicked);
    }


    private void onCancelClicked(View view) {
        cancelAll = true;
        super.dismiss();
    }

    private void onValidateClicked(View view) {
        if (isLastRunning) {
            GlobalToast.COMMON_TOAST.accept("请您耐心等待上一次请求结束");
            return;
        }
        isLastRunning = true;
        String pwd = pwdInput.getText() != null ? pwdInput.getText().toString() : null;
        if (pwd == null) {
            GlobalToast.COMMON_TOAST.accept("无效密码");
            return;
        }
        BiometricAddReq req = new BiometricAddReq();
        User user = LoginRepository.getInstance().getCurrentUser().getValue();
        if (user == null || user.id == 0L) {
            GlobalToast.COMMON_TOAST.accept("本地用户无法使用指纹验证功能");
            return;
        }
        req.email = user.email;
        req.deviceId = Biometric.getDeviceIdHash(GlobalApplication.getAppContext());
        req.deviceName = Build.MODEL;
        req.secretWord = Sha256.generateSha256ByteArray(pwd);
        CompletableFuture.supplyAsync(() -> {
            try {
                OkHttpClient client = GlobalInstance.OK_HTTP_NO_PROXY;
                Request request = new Request.Builder()
                        .url(new URL(ServerURL.URL_REGISTER_BIOMETRIC))
                        .method(ServerURL.POST, RequestBody.create(
                                MyJsonSerializer.serialize(req), ServerURL.APPLICATION_JSON
                        )).build();
                return Optional.of(client.newCall(request).execute());
            } catch (Exception e) {
                return Optional.empty();
            }
        }).thenAcceptAsync(resp -> NetWorkHandler.handleNetRespBeforeLogin(
                (Response) resp.orElse(null), () -> {
                    isLastRunning = false;
                }, result -> {
                    isLastRunning = false;
                    if (cancelAll) {
                        return;
                    }
                    try {
                        BiometricAddResp biometricAddResp = MyJsonSerializer.mapToObject(result.getContent(), BiometricAddResp.class).orElse(null);
                        if (biometricAddResp == null) {
                            GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                            return;
                        }
                        //密码验证成功，也获取到了新的token，要求用户进行指纹验证，然后存储token
                        //先生成密钥
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            Biometric.generateSecretKey(new KeyGenParameterSpec.Builder(
                                    KeyLoginFragment.KEY_NAME,
                                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                    .setUserAuthenticationRequired(true)
                                    .build());
                        } else {
                            Biometric.generateSecretKey(new KeyGenParameterSpec.Builder(
                                    KeyLoginFragment.KEY_NAME,
                                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                    .setUserAuthenticationRequired(true)
                                    .setUserAuthenticationParameters(
                                            5 * 60,
                                            KeyProperties.AUTH_BIOMETRIC_STRONG |
                                                    KeyProperties.AUTH_DEVICE_CREDENTIAL)
                                    .build());
                        }
                        //请求用户授权
                        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                                .setTitle("运动轻舟安全")
                                .setSubtitle("添加信任设备")
                                .setNegativeButtonText("取消")
                                .build();
                        BiometricPrompt prompt = new BiometricPrompt(
                                this.activity, ContextCompat.getMainExecutor(GlobalApplication.getAppContext()),
                                new BiometricPrompt.AuthenticationCallback() {
                                    @Override
                                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                        super.onAuthenticationError(errorCode, errString);
                                        GlobalToast.COMMON_TOAST.accept("生物授权错误！");
                                    }

                                    @Override
                                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                        super.onAuthenticationSucceeded(result);
                                        //然后完里面存储密钥
                                        Biometric.putData(
                                                biometricAddResp.token,
                                                KeyLoginFragment.PREFS_NAME,
                                                KeyLoginFragment.KEY_NAME,
                                                GlobalApplication.getAppContext()
                                        );
                                        GlobalToast.COMMON_TOAST.accept("信任设备添加成功！");
                                        MyPopupWindow.super.dismiss();
                                    }

                                    @Override
                                    public void onAuthenticationFailed() {
                                        super.onAuthenticationFailed();
                                        GlobalToast.COMMON_TOAST.accept("生物授权失败，请重试");
                                    }
                                }
                        );
                        prompt.authenticate(promptInfo);
                    } catch (Exception e) {
                        GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                    }
                }
        ), ContextCompat.getMainExecutor(GlobalApplication.getAppContext()));
    }

    @Override
    public void dismiss() {
    }
}