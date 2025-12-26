package com.nyzg.swiftsail.fragment.login;

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
import com.nyzg.swiftsail.bean.GlobalFunction;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.bean.MatchUtils;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.listener.LoginCountingListener;
import com.nyzg.swiftsail.netobj.MailVerifyReq;
import com.nyzg.swiftsail.netobj.RegisterVerifyMailReq;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    static private final String USER_LIST_KEY = "user_list";
    List<User> userList;
    Runnable waitingLastLogin;
    volatile boolean cancelLastLogin = false;
    static final String[] LAST_LOGIN_TEXT = {"检查上一次的登录账户中.", "检查上一次的登录账户中..", "检查上一次的登录账户中..."};
    int lastLoginTextPointer = 0;
    volatile boolean cancelAll = false;
    boolean haveFirstCheckLogin=false;

    public static Fragment newInstance(ArrayList<User> userList) {
        Fragment fragment = new LoginFragment();
        if (userList != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(USER_LIST_KEY, userList);
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.userList = getArguments() != null ? getArguments().getParcelableArrayList(USER_LIST_KEY) : null;
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您还没有登录，要回到主界面吗")
                        .setPositiveButton("确定", (dialog, which) -> {
                            setEnabled(false);//禁用自己，避免无限递归
                            cancelAll = true;//取消所有操作
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
        View father = inflater.inflate(R.layout.fragment_login, container, false);
        tryLastLogin(father);
        onClickGetVerifyCode(father);
        onClickSubmitRegister(father);
        onClickChangeMainPage(father);//点击切换登录和注册的点击事件逻辑
        return father;
    }

    //尝试进行上一次登录的流程
    private void tryLastLogin(View father) {
        if (haveFirstCheckLogin){
            showLoginPage(father);
            return;
        }
        haveFirstCheckLogin=true;
        if (waitingLastLogin != null) {
            return;
        }
        father.findViewById(R.id.loginMainPage).setVisibility(View.GONE);
        father.findViewById(R.id.checkLastLogin).setVisibility(View.VISIBLE);
        //主界面动画
        waitingLastLogin = () -> {
            if (cancelLastLogin || cancelAll) {
                cancelLastLogin = false;
                waitingLastLogin = null;
                return;
            }
            ((TextView) father.findViewById(R.id.checkLastLogin))
                    .setText(LAST_LOGIN_TEXT[lastLoginTextPointer]);
            ++lastLoginTextPointer;
            if (lastLoginTextPointer == LAST_LOGIN_TEXT.length) {
                lastLoginTextPointer = 0;
            }
            handler.postDelayed(waitingLastLogin, 1000);
        };
        handler.post(waitingLastLogin);
        CompletableFuture.supplyAsync(() -> {
            //从数据库中读取上一次的登录用户
            SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
            List<LastLogin> lastLoginList = sqLiteDB.lastLoginTable().selectAllFromLastLoginTable();
            if (lastLoginList.isEmpty()) {//没有上一次的登录用户
                return Optional.empty();
            }
            LastLogin lastLogin = lastLoginList.get(0);
            //从数据库中读取旧的数据
            String token = sqLiteDB.userTable().selectTokenFromUserTable(lastLogin.userId);
            User currentUser = new User();
            currentUser.setId(lastLogin.userId);
            GlobalInstance.currentUser.set(currentUser);
            if (token == null) {//过期的token
                return Optional.empty();
            }
            //然后向服务端进行验签
            return GlobalFunction.acquireNewTokenSyncNullAtFail(token);
        }).thenAccept(token -> {
            cancelLastLogin = true;
            //没有上次登录用户或者登录token过期，显示登录页面
            if (!token.isPresent()) {
                showLoginPage(father);
                return;
            }
            //更新和保存token
            GlobalFunction.onSuccessLoginSync(GlobalInstance.currentUser.get().getId(), (String) token.get());
            GlobalFunction.goBackToMainPage();//返回主界面
        });
    }

    private void showLoginPage(View father) {
        handler.post(() -> {
            father.findViewById(R.id.checkLastLogin).setVisibility(View.GONE);
            father.findViewById(R.id.loginMainPage).setVisibility(View.VISIBLE);
            if (userList == null) {
                switchToRegister(father);
            } else {
                putUserIntoRecycleView(father.findViewById(R.id.accountList), userList);
                switchToLogin(father);
            }
        });
    }

    //点击按钮注册用户点击事件逻辑
    @SuppressLint("ClickableViewAccessibility")
    private void onClickSubmitRegister(View father) {
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
                                .method("POST",
                                        RequestBody.create(JsonSerializer.serialize(req),
                                                GlobalConf.APPLICATION_JSON
                                        ))
                                .build();
                        return Optional.of(httpClient.newCall(request).execute());
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }).thenAccept(result -> {
                    stopWaiting.set(true);
                    GlobalFunction.handleNetResp((Response) result.orElse(null), () -> {
                    }, resp -> {
                        GlobalToast.RESPONSE_SUCCESS.accept(resp.getMessage());
                        try {
                            User user = JsonSerializer.mapToObject(resp.getContent(), User.class).orElse(null);
                            if (user == null) {
                                GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                                return;
                            }
                            //把这个注册用户写入数据库
                            GlobalFunction.safeInsertRegisterUserSync(user);
                            //成功登录
                            GlobalFunction.onSuccessLoginSync(user.getId(), user.getToken());
                        } catch (Exception e) {
                            GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                        }
                    });
                });
                return true;
            }
        });
    }

    //点击按钮获取验证码点击事件逻辑
    @SuppressLint("ClickableViewAccessibility")
    private void onClickGetVerifyCode(View view) {
        TextView verifyCodeButton = view.findViewById(R.id.getVerifyNumber);
        TextInputEditText mailAddr = view.findViewById(R.id.registerEmail);
        verifyCodeButton.setOnTouchListener(new LoginCountingListener<>(verifyCodeButton, () -> {
            String mail = mailAddr.getText() != null ? mailAddr.getText().toString() : null;
            if (mail == null || mail.isEmpty() || MatchUtils.isMailAddrIllegal(mail)) {
                Toast.makeText(GlobalApplication.getAppContext(), "邮箱格式不正确", Toast.LENGTH_LONG).show();
                view.setPressed(false);
                return Optional.empty();
            }
            return Optional.of(mail);
        }, strMailAddr -> {
            OkHttpClient okHttpClient = GlobalInstance.okHttpClient;
            try {
                Request request = new Request.Builder()
                        .url(GlobalConf.URL_REGISTER_VERIFY_CODE)
                        .method("POST",
                                RequestBody.create(JsonSerializer.serialize(new MailVerifyReq(strMailAddr)),
                                        GlobalConf.APPLICATION_JSON
                                ))
                        .build();
                return Optional.of(okHttpClient.newCall(request).execute());
            } catch (Exception e) {
                return Optional.empty();
            }
        }, resp -> GlobalToast.RESPONSE_SUCCESS.accept(resp.getMessage())));
    }

    //点击切换登录和注册的点击事件逻辑
    private void onClickChangeMainPage(View view) {
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
}