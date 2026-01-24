package com.nyzg.swiftsail;

import static androidx.core.content.ContextCompat.getSystemService;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import com.nyzg.swiftsail.bean.ChannelId;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.fragment.login.LoginFragment;
import com.nyzg.swiftsail.fragment.login.MainFragment;
import com.nyzg.swiftsail.fragment.main.WaitingFragment;
import com.nyzg.swiftsail.obj.SucceedOrNot;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.RecordRepository;
import com.nyzg.swiftsail.repository.SyncRepository;


public class MainActivity extends AppCompatActivity {
    private long lastBackPressedTime = 0L;
    private static final long BACK_PRESS_EXIT_INTERVAL = 2000;//2秒

    //MainActivity的onCreate和App的生命周期是一致的
    //一般在一次使用应用时只会初始化一次
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //显示加载界面
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragment, WaitingFragment.getInstance())
                .commit();
        fullScreen();
        checkAndDoLogin();
        doubleClickToQuitApp();
        createHeadsUpNotificationChannel();
        observeNotificationRequest();
    }


    private void createHeadsUpNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                ChannelId.HEAD_UP_CHANNEL_ID,
                "操作提示",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("用于短暂提示用户操作状态");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void observeNotificationRequest() {
        SyncRepository.getInstance().notificationPair.observeForever(pair -> {
            String msg = pair.getA();
            if (msg == null || msg.isEmpty()) {
                return;
            }
            Notification notification = new NotificationCompat.Builder(this, ChannelId.HEAD_UP_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("您已经在同步数据了")
                    .setTimeoutAfter(3000)
                    .setAutoCancel(true)
                    .build();
            getSystemService(NotificationManager.class).notify(ChannelId.NOTIFICATION_ID.getAndIncrement(), notification);
        });
    }


    private void doubleClickToQuitApp() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long currentPressedTime = System.currentTimeMillis();
                if (lastBackPressedTime == 0L) {
                    Toast.makeText(MainActivity.this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
                    lastBackPressedTime = currentPressedTime;
                    return;
                }
                long interval = System.currentTimeMillis() - lastBackPressedTime;
                if (interval <= BACK_PRESS_EXIT_INTERVAL) {
                    setEnabled(false);//禁用自己，避免无限递归
                    finishAffinity();//退出应用
                    return;
                }
                Toast.makeText(MainActivity.this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
                lastBackPressedTime = currentPressedTime;
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void checkAndDoLogin() {
        LoginRepository.getInstance()
                .tryLastLoginAsync()
                .thenAcceptAsync(result -> {
                    if (result == SucceedOrNot.FAIL) {
                        //使用上一次的登录账户登录失败，添加登录页面，
                        MainActivity.this.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainFragment, MainFragment.getInstance())
                                .commit();
                        MainActivity.this.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainFragment, LoginFragment.newInstance())
                                .addToBackStack(null)
                                .commit();
                    } else {
                        //使用上一次的登录账户登录成功，直接进入主界面
                        MainActivity.this.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainFragment, MainFragment.getInstance())
                                .commit();
                    }
                    //此时LoginRepository已经成功更新了currentUser
                    RecordRepository.getInstance().updateUserOnChange();
                }, ContextCompat.getMainExecutor(this));
    }

    public static void addFragmentToStackTop(FragmentManager manager, Fragment fragment) {
        Fragment current = manager.findFragmentById(R.id.mainFragment);
        //不要重复添加
        if (current != null && current.getClass().equals(fragment.getClass())) {
            return;
        }
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.mainFragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public static void popUntilTheInitOne(FragmentManager manager) {
        if (manager.isStateSaved()) {
            return;
        }
        int count = manager.getBackStackEntryCount();
        if (count > 0) {
            manager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private void fullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) {
                return;
            }
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }
}