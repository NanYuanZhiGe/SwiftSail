package com.nyzg.swiftsail;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.nyzg.swiftsail.fragment.login.LoginFragment;
import com.nyzg.swiftsail.fragment.login.MainFragment;
import com.nyzg.swiftsail.worker.LoginWorker;


public class MainActivity extends AppCompatActivity {
    private static boolean onCheckLogin = false;

    //MainActivity的onCreate和App的生命周期是一致的
    //一般在一次使用应用时只会初始化一次
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragment, new MainFragment())
                .commit();
        fullScreen();
        checkAndDoLogin();
    }

    private void checkAndDoLogin() {
        if (onCheckLogin) {
            return;
        }
        onCheckLogin = true;
        OneTimeWorkRequest request = new OneTimeWorkRequest
                .Builder(LoginWorker.class)
                .setConstraints(new Constraints.Builder().build())
                .build();
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(request);
        workManager.getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo == null || (workInfo.getState() != WorkInfo.State.FAILED && workInfo.getState() != WorkInfo.State.SUCCEEDED)) {
                        return;
                    }
                    //如果上一次的登录依旧是有效的，就不要进入登录页面，什么都不做
                    //只有上一次没有登录，或者上一次的登录无法验证，才进入登录页面
                    if (workInfo.getState()==WorkInfo.State.FAILED){
                        addFragmentToStackTop(getSupportFragmentManager(),LoginFragment.newInstance());
                    }
                    onCheckLogin = false;
                });
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