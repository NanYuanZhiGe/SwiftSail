package com.nyzg.distance_count;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.widget.TextView;
import android.widget.Toast;


import com.nyzg.distance_count.kf.KfImp2D;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends AppCompatActivity {
    public static final int CHANGE_TEXT = 0;
    private final static int LOCATION_PERMISSION_CODE = 0;
    private static final AtomicBoolean hasStarted = new AtomicBoolean(false);
    public static MHandler mhandler = null;
    private volatile Location lastLocation = null;
    private final AtomicBoolean onPause = new AtomicBoolean(true);
    private double accumulateDistance = 0;
    private final KfImp2D kfImp2D = new KfImp2D();

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mhandler = new MHandler(Looper.getMainLooper(), this);
        checkAndAcquirePermission();

        TextView textView = findViewById(R.id.distance);

        findViewById(R.id.start).setOnClickListener(v -> {
            if (!hasStarted.compareAndSet(false, true)) {//已经启动过了
                return;
            }
            onPause.set(false);
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    5,
                    location -> {
                        if (onPause.get()) {
                            lastLocation = null;
                            return;
                        }
                        if (lastLocation == null) {
                            lastLocation = location;
                            Toast.makeText(GlobalApplication.getAppContext(), "Fist located:" + lastLocation.toString(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        double temp = location.distanceTo(lastLocation);
                        if (temp < 10) {
                            accumulateDistance += temp;
                            textView.setText(String.format("原始无修正：%.2f米，卡尔曼滤波修正：%.2f米",
                                    accumulateDistance,
                                    kfImp2D.getDistance(location)));
                        }
                        lastLocation = location;
                    }
            );
        });
        findViewById(R.id.pause).setOnClickListener(v -> onPause.set(true));
        findViewById(R.id.resume).setOnClickListener(v -> onPause.set(false));
    }

    public void checkAndAcquirePermission() {
        //已有权限
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }
        String[] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE
        };
        this.requestPermissions(permissions, LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_CODE) {//不是我们要的权限
            return;
        }
        boolean allGranted = true;
        for (int res : grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            Toast.makeText(this, "您没有给应用足够的权限，无法使用功能", Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void changeText(String text) {
        TextView textView = findViewById(R.id.distance);
        textView.setText(text);
    }

    public static class MHandler extends Handler {
        WeakReference<MainActivity> mainActivityWeakReference;

        public MHandler(@NonNull Looper looper, MainActivity mainActivity) {
            super(looper);
            this.mainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (mainActivityWeakReference.get() == null) {
                return;
            }
            switch (msg.what) {
                case MainActivity.CHANGE_TEXT:
                    mainActivityWeakReference.get().changeText((String) msg.obj);
                    break;
                default:
            }
        }
    }
}