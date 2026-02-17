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
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.RecordActivity;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.obj.record.DistanceAlgorithm;
import com.nyzg.swiftsail.obj.record.DistanceSimple;
import com.nyzg.swiftsail.obj.record.MyManualDistance;
import com.nyzg.swiftsail.repository.RecordRecordRepository;

public class RecordRecordService extends Service {
    public class MyBinder extends Binder {
        public RecordRecordService getService() {
            return RecordRecordService.this;
        }
    }

    private class MyLocationListener implements LocationListener {
        double accumulateTime;//单位秒
        long lastTime = 0L;
        final private DistanceAlgorithm distanceAlgorithm;

        public MyLocationListener() {
            String type = RecordRecordRepository.getInstance().SELECT_TYPE.getValue();
            if (type != null && type.equals(RecordType.BIKE)) {
                this.distanceAlgorithm = new DistanceSimple(6.5f);
            } else {
                this.distanceAlgorithm = new DistanceSimple(10f);
            }
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (quit) {
                return;
            }
            if (pause) {//如果暂停了
                lastTime = 0L;
                return;
            }
            if (lastTime == 0L) {
                lastTime = System.currentTimeMillis();
            } else {
                long currentTime = System.currentTimeMillis();
                accumulateTime += (currentTime - lastTime) / 1000.0;
                lastTime = currentTime;
            }
            distanceAlgorithm.update(location);
        }

        public void quit() {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        }

        //reset仅仅是清除当前的状态变量，累积的记录不会清除
        public void reset() {
            lastTime = 0L;
            accumulateTime = 0L;
            distanceAlgorithm.reset();
        }


        /**
         * summary不要重置信息，重置信息由cancel触发
         */
        public void summary() {
            double distance = distanceAlgorithm.summary();
            MyManualDistance myManualDistance = new MyManualDistance(
                    accumulateTime, distance
            );
            RecordRecordRepository.getInstance().exposeRecord0.setValue((long) distance);
            RecordRecordRepository.getInstance().detailRecord0.setValue(MyJsonSerializer.serialize(myManualDistance));
        }
    }

    private final IBinder binder = new MyBinder();
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "record_record_location";
    private volatile boolean pause = false;
    private volatile boolean quit = false;
    private LocationManager locationManager;
    private final MyLocationListener locationListener = new MyLocationListener();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,                 // 渠道 ID（字符串常量）
                "运动轻舟运动记录",             // 用户可见的渠道名称
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
                .setContentTitle("运动轻舟 运动记录")
                .setContentText("点击返回应用")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void pause() {
        pause = true;
        this.locationListener.reset();
    }

    /**
     * summary是做统计当前的记录的数据，
     * 不会涉及到数据库的写操作
     */
    public void summary() {
        this.locationListener.summary();
    }


    public void reset() {
        locationListener.reset();
    }

    public void resume() {
        pause = false;
    }

    public void destroy() {
        quit = true;
        locationListener.quit();
        stopForeground(true);
        stopSelf();
    }
}