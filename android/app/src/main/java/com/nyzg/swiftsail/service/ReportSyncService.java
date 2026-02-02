package com.nyzg.swiftsail.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.ChannelId;
import com.nyzg.swiftsail.repository.SyncRepository;
import com.nyzg.swiftsail.worker.ReportSyncWorker;


public class ReportSyncService extends LifecycleService {

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
        this.startForeground(ChannelId.NOTIFICATION_ID.getAndIncrement(), createNotification());
        OneTimeWorkRequest request = new OneTimeWorkRequest
                .Builder(ReportSyncWorker.class)
                .setConstraints(new Constraints.Builder().build())
                .build();
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(request);
        workManager.getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo == null || (workInfo.getState() != WorkInfo.State.FAILED && workInfo.getState() != WorkInfo.State.SUCCEEDED)) {
                        return;
                    }
                    if (workInfo.getState() == WorkInfo.State.FAILED) {

                        return;
                    }
                    SyncRepository.getInstance().onSync.set(false);
                    stopSelf();
                });
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                ReportSyncService.class.getName(),                 // 渠道 ID（字符串常量）
                "运动轻舟报表同步",             // 用户可见的渠道名称
                NotificationManager.IMPORTANCE_DEFAULT // 重要性
        );
        serviceChannel.setDescription("正在后台同步您的运动报表");
        serviceChannel.enableVibration(false);

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
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return mBinder;
    }
}