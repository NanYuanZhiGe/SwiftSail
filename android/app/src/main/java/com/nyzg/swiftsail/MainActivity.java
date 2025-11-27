package com.nyzg.swiftsail;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.fragment.LoginFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.worker.LoginWorker;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {
    static public final int SHOW_REGISTER_MSG = 0;
    static public final int SHOW_LOGIN_MSG = 1;
    static public final int DO_RUNNABLE = 2;
    static public final int ADD_FRAGMENT=3;
    private static volatile Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new InnerHandler(this.getMainLooper(), this);
        fullScreen();
        checkAndDoLogin();
    }

    private void checkAndDoLogin() {
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(new OneTimeWorkRequest.Builder(LoginWorker.class).setConstraints(
                new Constraints.Builder().build()
        ).build());
    }

    protected void addFragmentToStackTop(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.mainFragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
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


    public static Handler getHandler() {
        return handler;
    }

    public static class InnerHandler extends Handler {
        WeakReference<MainActivity> activityWeakReference;

        public InnerHandler(Looper looper, MainActivity activity) {
            super(looper);
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            MainActivity mainActivity = activityWeakReference.get();
            if (mainActivity == null) {
                return;
            }
            switch (msg.what) {
                case SHOW_REGISTER_MSG:
                    mainActivity.addFragmentToStackTop(new LoginFragment(null));
                    break;
                case SHOW_LOGIN_MSG:
                    List<User> userList = (List<User>) msg.obj;
                    mainActivity.addFragmentToStackTop(new LoginFragment(userList));
                    break;
                case DO_RUNNABLE:
                    Runnable r = (Runnable) msg.obj;
                    r.run();
                    break;
                case ADD_FRAGMENT:
                    Fragment fragment=(Fragment)msg.obj;
                    mainActivity.addFragmentToStackTop(fragment);
                default:
            }
        }
    }
}