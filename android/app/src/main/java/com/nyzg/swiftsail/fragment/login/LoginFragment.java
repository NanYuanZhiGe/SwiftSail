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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.bean.MatchUtils;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.listener.LoginCountingListener;
import com.nyzg.swiftsail.netobj.login.MailVerifyReq;
import com.nyzg.swiftsail.netobj.login.RegisterVerifyMailReq;
import com.nyzg.swiftsail.repository.LoginRepository;

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
    volatile boolean cancelAll = false;

    public static Fragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (LoginRepository.getInstance().getCurrentUser().getValue() != null) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                //当前用户为空
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您还没有登录，要回到主界面吗（将本地账户登录）？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            LoginRepository.getInstance().login(GlobalInstance.LOCAL_USER);
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
        //这个动画已弃用，上一次登录用户的校验移交至MainActivity启动后来检验，
        //现在LoginFragment的职能就是专职注册和登录，不会有其他任何的多余操作
        father.findViewById(R.id.checkLastLogin).setVisibility(View.GONE);
        showLoginPage(father, null);//先传一个空的，让它显示注册界面
        //观察userList，如果查询到了用户数据，就更新为登录界面
        LoginRepository.getInstance()
                .availableUserList
                .observe(
                        getViewLifecycleOwner(),
                        userList -> showLoginPage(father, userList)
                );
        onClickGetVerifyCode(father);//获取验证码的点击事件
        onClickSubmitRegister(father);//提交注册信息的点击事件逻辑
        onClickChangeMainPage(father);//点击切换登录和注册的点击事件逻辑
        return father;
    }


    private void showLoginPage(View father, List<User> userList) {
        father.findViewById(R.id.loginMainPage).setVisibility(View.VISIBLE);
        if (userList == null || userList.size() == 0) {
            switchToRegister(father);
        } else {
            putUserIntoRecycleView(father.findViewById(R.id.accountList), userList);
            switchToLogin(father);
        }
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
                        OkHttpClient httpClient = GlobalInstance.OK_HTTP_NO_PROXY;
                        Request request = new Request.Builder()
                                .url(new URL(ServerURL.URL_REGISTER_SUBMIT))
                                .method("POST",
                                        RequestBody.create(JsonSerializer.serialize(req),
                                                ServerURL.APPLICATION_JSON
                                        ))
                                .build();
                        return Optional.of(httpClient.newCall(request).execute());
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }).thenAcceptAsync(result -> {
                    stopWaiting.set(true);
                    NetWorkHandler.handleNetRespBeforeLogin((Response) result.orElse(null), () -> {
                    }, resp -> {
                        GlobalToast.RESPONSE_SUCCESS.accept(resp.getMessage());
                        try {
                            User user = JsonSerializer.mapToObject(resp.getContent(), User.class).orElse(null);
                            if (user == null) {
                                GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                                return;
                            }
                            //把这个注册用户写入数据库
                            LoginRepository.getInstance().updateUserToLocalAccountAsync(user);
                            //成功登录
                            LoginRepository.getInstance().login(user);
                            //返回主界面
                            MainActivity.popUntilTheInitOne(requireActivity().getSupportFragmentManager());
                        } catch (Exception e) {
                            GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
                        }
                    });
                }, ContextCompat.getMainExecutor(requireContext()));
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
            OkHttpClient okHttpClient = GlobalInstance.OK_HTTP_NO_PROXY;
            try {
                Request request = new Request.Builder()
                        .url(ServerURL.URL_REGISTER_VERIFY_CODE)
                        .method("POST",
                                RequestBody.create(JsonSerializer.serialize(new MailVerifyReq(strMailAddr)),
                                        ServerURL.APPLICATION_JSON
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

    @SuppressLint("NotifyDataSetChanged")
    private void putUserIntoRecycleView(RecyclerView recyclerView, List<User> userList) {
        if (recyclerView.getAdapter() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
            recyclerView.setAdapter(new MyAdapter(userList));
        } else {
            ((MyAdapter) recyclerView.getAdapter()).updateAllData(userList);
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        List<User> userList;

        public MyAdapter(List<User> originalUserList) {
            // 创建副本，避免污染原始数据
            this.userList = new ArrayList<>();
            if (originalUserList != null) {
                this.userList.addAll(originalUserList);
            }
        }

        public void updateAllData(List<User> newList) {
            this.userList = new ArrayList<>(newList.size());
            this.userList.addAll(newList);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new MyViewHolder(inflater.inflate(R.layout.account_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.bind(userList.get(position));
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        TextView firstCharacter;
        TextView nickName;
        TextView email;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            firstCharacter = itemView.findViewById(R.id.firstCharacter);
            nickName = itemView.findViewById(R.id.accountNickName);
            email = itemView.findViewById(R.id.accountEmail);
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(User user) {
            if (user.id == -1) {
                firstCharacter.setText("?");
                nickName.setText("账户未列出？");
                email.setText("点此添加登录账户");
                itemView.setOnClickListener((view -> MainActivity.addFragmentToStackTop(
                        requireActivity().getSupportFragmentManager(),
                        UserNotInListFragment.newInstance()
                )));
                return;
            }
            firstCharacter.setText(user.nickName.substring(0,1));
            nickName.setText(user.nickName);
            email.setText(user.email);
            itemView.setOnClickListener(view -> {
                LoginRepository.getInstance().onLoginUser.setValue(user);
                MainActivity.addFragmentToStackTop(
                        requireActivity().getSupportFragmentManager(),
                        LoginTypeSelectFragment.newInstance()
                );
            });
        }
    }
}