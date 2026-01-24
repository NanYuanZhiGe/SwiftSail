package com.nyzg.swiftsail.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.ChannelId;


public class ReportSyncService extends Service {

    public class MyBinder extends Binder {
        public ReportSyncService getService() {
            return ReportSyncService.this;
        }
    }

    private final IBinder mBinder = new MyBinder();
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        this.startForeground(2, createNotification());
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                ReportSyncService.class.getName(),                 // 渠道 ID（字符串常量）
                "运动轻舟报表同步",             // 用户可见的渠道名称
                NotificationManager.IMPORTANCE_DEFAULT // 重要性
        );
        serviceChannel.setDescription("正在后台同步您的运动报表");

        // 获取系统通知管理器并注册该渠道
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, ReportSyncService.class.getName())
                .setContentTitle("运动轻舟 报表同步")
                .setContentText("数据同步中")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(null)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(()->{
            while (true){
                Log.v("myTag","waiting");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}