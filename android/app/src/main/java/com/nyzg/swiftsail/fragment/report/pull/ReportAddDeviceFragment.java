package com.nyzg.swiftsail.fragment.report.pull;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.EncryptThreadSafe;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.dbobj.Watch;
import com.nyzg.swiftsail.encrypt.Uuid;
import com.nyzg.swiftsail.fragment.InnerFragment;
import com.nyzg.swiftsail.netobj.report.WatchAddReq;
import com.nyzg.swiftsail.obj.SucceedOrNot;
import com.nyzg.swiftsail.obj.Tuple;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.ArrowPositionRules;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 这个类是无状态的，它的功能就是做我们手表管理状态机中的“授权”步骤
 * 客户端的授权步骤分为两步：厂商授权和授权检查
 * 由于它是无状态的，您可以在任何时候调用这个页面让它出现在顶层
 */
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
    private OnBackPressedCallback callback;
    private ImageView notification;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    final private List<String> notificationList = new ArrayList<>(3);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        watchViewModel = new ViewModelProvider(this).get(WatchViewModel.class);
        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressedLogic();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void onBackPressedLogic() {
        if (callback == null) {
            return;
        }
        if (quitForbidden) {
            GlobalToast.COMMON_TOAST.accept("正在处理您的请求，很快就会完成！");
            return;
        }
        Watch watch = watchViewModel.getWatch().getValue();
        if (watch == null) {//用户没有填写信息
            callback.setEnabled(false);
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }
        //提醒用户是否要退出
        new AlertDialog.Builder(requireContext())
                .setTitle("")
                .setMessage("您确定要退出吗？填写的信息不会保存！")
                .setPositiveButton("确定", (dialog, which) -> {
                    callback.setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_device_add, container, false);
        father.findViewById(R.id.backward).setOnClickListener(v -> onBackPressedLogic());
        inputDeviceName = father.findViewById(R.id.inputDeviceName);
        inputDeviceType = father.findViewById(R.id.inputDeviceType);
        recyclerView = father.findViewById(R.id.msgBox);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new MyAdapter(notificationList));
        drawerLayout = father.findViewById(R.id.drawerLayout);
        //用户手动划开消息栏的时候自动改变图标
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                notification.setImageDrawable(ContextCompat.getDrawable(
                        GlobalApplication.getAppContext(), R.drawable.notifications
                ));
            }
        });
        notification = father.findViewById(R.id.notification);
        notification.setOnClickListener(this::onNotificationClicked);
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

    private void doNotification(String msg) {
        //使用balloon来提醒用户
        Context context = getContext();
        if (context != null) {
            Balloon balloon = new Balloon.Builder(context)
                    .setArrowSize(10)
                    .setArrowOrientation(ArrowOrientation.TOP)
                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                    .setArrowPosition(0.5f)
                    .setWidth(BalloonSizeSpec.WRAP)
                    .setHeight(65)
                    .setPaddingHorizontal(12)
                    .setTextSize(15f)
                    .setCornerRadius(4f)
                    .setAlpha(0.9f)
                    .setText(msg)
                    .setTextColor(ContextCompat.getColor(context, R.color.black))
                    .setTextIsHtml(false)
                    .setBackgroundColor(ContextCompat.getColor(context, R.color.recordRecordDetail))
                    .setBalloonAnimation(BalloonAnimation.FADE)
                    .setLifecycleOwner(getViewLifecycleOwner())
                    .build();
            balloon.showAlignBottom(this.notification);
            balloon.dismissWithDelay(2000L);//2秒自己结束
        }
        //添加消息到列表中，用户可以滑动查看消息
        notificationList.add(msg);
        MyAdapter myAdapter = (MyAdapter) recyclerView.getAdapter();
        if (myAdapter == null) {
            return;
        }
        myAdapter.notifyItemInserted(notificationList.size() - 1);
        //如果成功插入，更新图标提醒用户
        this.notification.setImageDrawable(ContextCompat.getDrawable(
                GlobalApplication.getAppContext(), R.drawable.notification_unread
        ));
    }

    private void onNotificationClicked(View v) {
        notification.setImageDrawable(ContextCompat.getDrawable(
                GlobalApplication.getAppContext(), R.drawable.notifications));
        drawerLayout.openDrawer(GravityCompat.END);
    }

    /**
     * 授权检查
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
            doNotification("请您先进行授权");
            return;
        }
        if (user == null || user.id == 0L || watch == null) {
            doNotification("请您先登录或填写完整的信息");
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
                    this::doNotification,
                    action -> {/*
                    请求成功，什么都不要做
                    对于以前的版本，会写到数据库中，但是这样子除了增加业务的复杂度之外，没有任何作用
                    因为你用户回到“设备管理”界面，甭管你本地有没有设备，都需要进行网络查询设备的状态
                    那你还不如直接网络请求设备的信息就好了，这样子还容易保持数据的一致性
                    */
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
                doNotification("手表添加成功！");
                return;
            }
            //state一定要清空，标志用户需要进行授权
            watchViewModel.getState().setValue(null);
            doNotification("手表添加失败！");
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * 厂商授权
     * 获取用户填写的内容，跳转浏览器让用户进行第三方授权
     */
    @SuppressLint("DefaultLocale")
    private void onGrantSubmit(View v) {
        if (submitDataForbidden) {
            doNotification("您上次提交的信息正在处理中，请耐心等待");
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
        //跳转页面
        openCustomTabOrBrowser(null, requireActivity(), Uri.parse(parseUrl));
        //显示“我已授权的按钮”
        grantedBtn.setVisibility(View.VISIBLE);
    }

    private Watch getWatch() {
        //校验用户的输入
        User currentUser = LoginRepository.getInstance().getCurrentUser().getValue();
        if (currentUser == null || currentUser.id == 0L) {
            doNotification("未登录用户不支持添加手表");
            return null;
        }
        if (inputDeviceName == null || inputDeviceType == null || inputClientId == null || inputClientSecret == null) {
            return null;
        }
        //获取数据
        String deviceName = inputDeviceName.getText()==null?null:inputDeviceName.getText().toString().trim();
        if (deviceName==null||deviceName.isEmpty()) {
            doNotification("设备名字不能为空");
            return null;
        }
        String clientId = inputClientId.getText()==null?null:inputClientId.getText().toString().trim();
        if (clientId==null||clientId.isEmpty()) {
            doNotification("client_id不能为空");
            return null;
        }
        String clientSecret = inputClientSecret.getText()==null?null:inputClientSecret.getText().toString().trim();
        if (clientSecret==null||clientSecret.isEmpty()) {
            doNotification("client_secret不能为空");
            return null;
        }
        String deviceType = inputDeviceType.getSelectedItem().toString();
        //构造结果
        Watch watch = new Watch();
        watch.userId = currentUser.id;
        watch.clientId = clientId;
        watch.type = deviceType;
        watch.name = deviceName;
        watch.authorizeHeader = EncryptThreadSafe.transferStringToBase64EncodedString(clientId + ":" + clientSecret);
        watch.accessible = true;
        return watch;
    }

    /**
     * @param openPackage null时表示为默认浏览器
     * @param uri         打开浏览器的地址
     */
    private void openCustomTabOrBrowser(
            @Nullable String openPackage,//后期可以尝试优化为先打开edge，chrome什么的再打开默认浏览器
            Activity activity,
            Uri uri) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.intent.setPackage(openPackage);
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

    public static class MyAdapter extends RecyclerView.Adapter<MyHolder> {
        private final List<String> notification;

        public MyAdapter(@NonNull List<String> notification) {
            this.notification = notification;
        }

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_common_speech, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            holder.textView.setText(notification.get(position));
        }

        @Override
        public int getItemCount() {
            return notification.size();
        }
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
        }
    }
}