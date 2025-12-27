package com.nyzg.swiftsail.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.RecordActivity;
import com.nyzg.swiftsail.fragment.record.RecordRecordFragment;
import com.nyzg.swiftsail.viewmodel.RecordRecordViewModel;

import java.text.DecimalFormat;
import java.time.LocalDate;

public class RecordRecordService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "record_record_location";
    private volatile boolean pause = false;
    private volatile boolean quit = false;
    private LocationManager locationManager;
    private final LocationListener locationListener = new LocationListener() {
        Location lastLocation;
        double accumulateDistance;
        long accumulateTime;
        long lastTime = 0L;
        private final DecimalFormat kilo = new DecimalFormat("0.000");
        private final DecimalFormat speed = new DecimalFormat("0.00");

        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (quit) {
                this.quit();
                return;
            }
            if (pause) {
                lastLocation = null;
                lastTime = 0L;
                return;
            }
            if (lastLocation == null) {
                lastLocation = location;
                lastTime = System.currentTimeMillis();
                return;
            }
            double temp = location.distanceTo(lastLocation);
            lastLocation = location;
            long currentTime = System.currentTimeMillis();
            RecordRecordViewModel recordViewModel = RecordRecordFragment.recordViewModel;
            if (temp < 10 && recordViewModel != null) {
                accumulateDistance += temp;
                accumulateTime += currentTime - lastTime;
                recordViewModel.setDistanceKiloMeter(
                        this.kilo.format(accumulateDistance / 1000)
                );
                double divided = accumulateTime / 1000.0;//单位是秒
                double recordSpeed = 0;
                if (divided > 1e-3) {
                    recordSpeed = accumulateDistance / divided;//米每秒
                }
                recordViewModel.setSpeedMeterSecond(
                        this.kilo.format(recordSpeed)
                );
                long accumulateSecond = accumulateTime / 1000;
                long minutes = accumulateSecond / 60;
                long second = accumulateSecond % 60;
                recordViewModel.setTimeMinuteSecond(
                        String.format("%02d:%02d", minutes, second)
                );
            }
            lastTime=currentTime;
        }

        private void quit() {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        locationManager = ((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                5,
                locationListener
        );
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,                 // 渠道 ID（字符串常量）
                "运动记录服务",             // 用户可见的渠道名称
                NotificationManager.IMPORTANCE_LOW // 重要性（LOW 表示不打扰）
        );
        serviceChannel.setDescription("正在后台记录您的运动轨迹");

        // 获取系统通知管理器并注册该渠道
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification createNotification() {
        // 点击通知返回主界面
        Intent notificationIntent = new Intent(this, RecordActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SwiftSail 运动记录中")
                .setContentText("点击返回应用")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void pause() {
        pause = true;
    }

    public void resume() {
        pause = false;
    }

    public void destroy() {
        quit = true;
        stopForeground(true);
        stopSelf();
    }
}