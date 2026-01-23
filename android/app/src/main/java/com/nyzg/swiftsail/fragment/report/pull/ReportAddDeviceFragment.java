package com.nyzg.swiftsail.fragment.report.pull;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.EncryptThreadSafe;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dao.WatchTable;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.dbobj.Watch;
import com.nyzg.swiftsail.encrypt.Uuid;
import com.nyzg.swiftsail.fragment.InnerFragment;
import com.nyzg.swiftsail.netobj.report.WatchAddReq;
import com.nyzg.swiftsail.obj.SucceedOrNot;
import com.nyzg.swiftsail.obj.Tuple;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReportAddDeviceFragment extends InnerFragment {

    public static Fragment getInstance() {
        return new ReportAddDeviceFragment();
    }

    private WatchViewModel watchViewModel;
    private TextInputEditText inputDeviceName;
    private Spinner inputDeviceType;
    private TextInputEditText inputClientId;
    private TextInputEditText inputClientSecret;
    private TextView grantedBtn;
    private volatile boolean quitForbidden = false;
    private volatile boolean submitDataForbidden = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        watchViewModel = new ViewModelProvider(this).get(WatchViewModel.class);
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (quitForbidden) {
                    GlobalToast.COMMON_TOAST.accept("正在处理您的请求，很快就会完成！");
                    return;
                }
                Watch watch = watchViewModel.getWatch().getValue();
                if (watch == null) {//用户没有填写信息
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                //提醒用户是否要退出
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您确定要退出吗？填写的信息不会保存！")
                        .setPositiveButton("确定", (dialog, which) -> {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_device_add, container, false);
        inputDeviceName = father.findViewById(R.id.inputDeviceName);
        inputDeviceType = father.findViewById(R.id.inputDeviceType);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.reportAddDeviceType,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputDeviceType.setAdapter(adapter);
        inputClientId = father.findViewById(R.id.inputClientId);
        inputClientSecret = father.findViewById(R.id.inputClientSecret);
        father.findViewById(R.id.submit).setOnClickListener(this::onGrantSubmit);
        grantedBtn = father.findViewById(R.id.hasGranted);
        grantedBtn.setOnClickListener(this::onGrantedCheck);
        grantedBtn.setVisibility(View.INVISIBLE);
        return father;
    }

    /**
     * 向服务端检查授权情况
     * 并提交必要的信息
     */
    private void onGrantedCheck(View v) {
        //这边要禁止用户退出以及继续提交其他内容
        //避免一些奇奇怪怪的bug
        //向服务端查询信息
        Watch watch = watchViewModel.getWatch().getValue();
        String codeVerifier = watchViewModel.getCodeVerifier().getValue();
        String state = watchViewModel.getState().getValue();
        User user = LoginRepository.getInstance().getCurrentUser().getValue();
        if (state == null) {
            GlobalToast.COMMON_TOAST.accept("请您先进行授权");
            return;
        }
        if (user == null || user.id == 0L || watch == null) {
            GlobalToast.COMMON_TOAST.accept("请您先登录或填写完整的信息");
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            //禁止用户退出以及继续提交其他内容
            quitForbidden = true;
            submitDataForbidden = true;
            WatchAddReq req = new WatchAddReq(
                    user.id,
                    watch.clientId,
                    watch.name,
                    watch.type,
                    watch.authorizeHeader,
                    codeVerifier,
                    state
            );
            OkHttpClient client = GlobalInstance.OK_HTTP_NO_PROXY;
            Request request = NetWorkBuilder.buildJsonRequestJwt(
                    ServerURL.URL_IS_WATCH_ADDED,
                    ServerURL.POST,
                    req
            );
            Response response;//response后面会自动释放
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                GlobalToast.SERVER_NOT_RESPONSE.run();
                return SucceedOrNot.FAIL;
            }
            AtomicBoolean success = new AtomicBoolean(true);
            NetWorkHandler.handleNetRespAfterLogin(
                    requireActivity().getSupportFragmentManager(),
                    response,
                    () -> {//请求失败，里面会自动toast message
                        success.set(false);
                    },
                    action -> {//请求成功，就把这个watch写到数据库里面
                        //不用当心clientId重复，里面会有replace
                        WatchTable watchTable = SQLiteDB.getDatabase(requireContext()).watchTable();
                        watchTable.insertWatch(watch);
                    },
                    Void.class
            );
            return success.get() ? SucceedOrNot.SUCCEED : SucceedOrNot.FAIL;
        }).thenAcceptAsync(res -> {
            quitForbidden = false;
            submitDataForbidden = false;
            //操作成功
            if (res == SucceedOrNot.SUCCEED) {
                //删除已经提交的信息
                watchViewModel.clean();
                GlobalToast.COMMON_TOAST.accept("手表添加成功！");
                return;
            }
            //state一定要清空，标志用户需要进行授权
            watchViewModel.getState().setValue(null);
            GlobalToast.COMMON_TOAST.accept("手表添加失败！");
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * 这里先先生成临时的手表信息，然后跳转到对应的页面
     */
    @SuppressLint("DefaultLocale")
    private void onGrantSubmit(View v) {
        if (submitDataForbidden) {
            GlobalToast.COMMON_TOAST.accept("您上次提交的信息正在处理中，请耐心等待");
            return;
        }
        Watch watch = getWatch();
        if (watch == null) {//信息不完整，或者用户未登录
            return;
        }
        //保存信息
        watchViewModel.getWatch().setValue(watch);
        Tuple<String, String, String> codeChallenge = EncryptThreadSafe.getBase64UrlSha256RandomString();
        watchViewModel.getCodeVerifier().setValue(codeChallenge.getA());
        String state = Uuid.getUuidString36();
        watchViewModel.getState().setValue(state);
        String parseUrl = "https://www.fitbit.com/oauth2/authorize?client_id=" + watch.clientId + "&response_type=code" +
                "&code_challenge=" + codeChallenge.getC() + "&code_challenge_method=S256" +
                "&scope=activity%20heartrate%20location%20nutrition%20oxygen_saturation%20profile" +
                "%20respiratory_rate%20settings%20sleep%20social%20temperature%20weight" +
                "&state=" + state;
        Log.v("myTag", parseUrl);
        //跳转页面
        openCustomTabFitbit(requireActivity(), new CustomTabsIntent.Builder().build(), Uri.parse(parseUrl));
        //显示“我已授权的按钮”
        grantedBtn.setVisibility(View.VISIBLE);
    }

    private Watch getWatch() {
        //校验用户的输入
        User currentUser = LoginRepository.getInstance().getCurrentUser().getValue();
        if (currentUser == null || currentUser.id == 0L) {
            GlobalToast.COMMON_TOAST.accept("未登录用户不支持添加手表");
            return null;
        }
        if (inputDeviceName == null || inputDeviceType == null || inputClientId == null || inputClientSecret == null) {
            return null;
        }
        if (inputDeviceName.getText() == null) {
            GlobalToast.COMMON_TOAST.accept("设备名字不能为空");
            return null;
        }
        if (inputClientId.getText() == null) {
            GlobalToast.COMMON_TOAST.accept("client_id不能为空");
            return null;
        }
        if (inputClientSecret.getText() == null) {
            GlobalToast.COMMON_TOAST.accept("client_secret不能为空");
            return null;
        }
        //获取数据
        String deviceName = inputDeviceName.getText().toString();
        String deviceType = inputDeviceType.getSelectedItem().toString();
        String clientId = inputClientId.getText().toString();
        String clientSecret = inputClientSecret.getText().toString();
        //构造结果
        Watch watch = new Watch();
        watch.userId = currentUser.id;
        watch.clientId = clientId;
        watch.type = deviceType;
        watch.name = deviceName;
        watch.authorizeHeader = EncryptThreadSafe.transferStringToBase64EncodedString(clientId + ":" + clientSecret);
        watch.accessible=true;
        return watch;
    }

    private void openCustomTabFitbit(
            Activity activity,
            CustomTabsIntent customTabsIntent,
            Uri uri
    ) {
        customTabsIntent.intent.setPackage(null);
        customTabsIntent.launchUrl(activity, uri);
    }

    public static class WatchViewModel extends ViewModel {
        private final MutableLiveData<Watch> watch = new MutableLiveData<>(null);
        private final MutableLiveData<String> codeVerifier = new MutableLiveData<>();
        private final MutableLiveData<String> state = new MutableLiveData<>();

        public MutableLiveData<String> getState() {
            return state;
        }

        public MutableLiveData<String> getCodeVerifier() {
            return codeVerifier;
        }

        public MutableLiveData<Watch> getWatch() {
            return watch;
        }

        public void clean() {
            watch.setValue(null);
            codeVerifier.setValue(null);
            state.setValue(null);
        }
    }
}