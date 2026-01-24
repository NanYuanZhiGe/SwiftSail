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

import com.google.gson.Gson;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.RecordActivity;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.dbobj.RecordBackUp;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.encrypt.Uuid;
import com.nyzg.swiftsail.bean.DateUtils;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.RecordRecordRepository;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class RecordRecordService extends Service {
    public class MyBinder extends Binder {
        public RecordRecordService getService() {
            return RecordRecordService.this;
        }
    }

    private class MyLocationListener implements LocationListener {
        double accumulateDistance;//单位米
        double accumulateTime;//单位秒
        Location lastLocation;
        long lastTime = 0L;
        double lastSpeed = .0;
        private final DecimalFormat kilo = new DecimalFormat("0.000");
        private final DecimalFormat speed = new DecimalFormat("0.00");
        private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (quit) {
                return;
            }
            if (pause) {
                lastLocation = null;
                lastTime = 0L;
                lastSpeed = .0;
                return;
            }
            if (lastLocation == null) {
                lastLocation = location;
                lastTime = System.currentTimeMillis();
                return;
            }
            double deltaDistance = location.distanceTo(lastLocation);//米，这个不一定准确
            long currentTime = System.currentTimeMillis();
            double deltaTime = (currentTime - lastTime) / 1000.0;//秒，这个一定准确
            RecordRecordRepository repository = RecordRecordRepository.INSTANCE;

            //检查位移是否合法
            //deltaDistance<=(lastSpeed+maxAccelerate*deltaTime)*deltaTime*0.5
            double legalDistance = (lastSpeed + maxAccelerate * deltaTime) * deltaTime * 0.5;

            if (deltaDistance > legalDistance) {//不合法，使用上一次的速度*时间记录位移
                deltaDistance = lastSpeed * deltaTime;
            }
            //如果用户的速度很小，比如0.2m/s，这个时候就认为是小波动，不会累计数据
            //防止deltaTime过小，如果时间过小，认为速度为0
            double currentSpeed = deltaTime > 1e-3 ? deltaDistance / deltaTime : .0;
            if (currentSpeed >= 0.2) {
                accumulateTime += deltaTime;
                accumulateDistance += deltaDistance;
                //========更新fragment中的UI=============
                //单位是公里，所以需要除以1000
                repository.setDistanceKilo(this.kilo.format(accumulateDistance / 1000));
                //速度是米每秒，不需要转化单位
                repository.setSpeedMeterSecond(this.speed.format(currentSpeed));
                long minutes = ((long) accumulateTime) / 60;
                long second = ((long) accumulateTime) % 60;
                //显示用户运动的分和秒
                repository.setMinuteSecond(String.format("%02d:%02d", minutes, second));
                repository.setLocation(location);
            }
            //更新上一次的数据
            lastLocation = location;
            lastTime = currentTime;
            lastSpeed = currentSpeed;
        }

        public void quit() {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        }

        //reset仅仅是清除当前的状态变量，累积的记录不会清除
        public void reset() {
            lastSpeed = 0.;
            lastLocation = null;
            lastTime = 0L;
        }

        //cancel是重置所有东西，包括累积的记录
        public void cancel() {
            lastSpeed = 0.;
            lastLocation = null;
            lastTime = 0L;
            accumulateDistance = .0;
            accumulateTime = 0L;
        }

        /**
         * summary不要重置信息，重置信息由cancel触发
         */
        public void summary() {
            User currentUser = LoginRepository.getInstance().getCurrentUser().getValue();
            if (currentUser == null) {
                currentUser = GlobalInstance.LOCAL_USER;
            }
            RecordRecordRepository rRRepository = RecordRecordRepository.INSTANCE;
            RecordBackUp recordBackUp = new RecordBackUp();
            recordBackUp.recordId = Uuid.getUuidString36();//运动记录的id
            //运动记录绑定到当前用户
            recordBackUp.userId = currentUser.id;
            LocalDate nowDate = LocalDate.now();
            recordBackUp.epochDay = nowDate.toEpochDay();
            recordBackUp.epochWeek = DateUtils.getEpochWeek(recordBackUp.epochDay);
            recordBackUp.epochMonth = DateUtils.getEpochMonth(nowDate);
            recordBackUp.epochYear = DateUtils.getEpochYear(nowDate);
            Map<String, Object> map = new HashMap<>();
            /*数据的格式
            "type":"useFeet",
            "duration":100,
            "distance":10,
            "startTime":"yyyy:MM:dd HH:mm:ss",
            "endTime":"yyyy:MM:dd HH:mm:ss"
             */
            LocalDateTime startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(sportStartTime),
                    ZoneId.systemDefault()
            );
            LocalDateTime endTime = LocalDateTime.now();
            map.put("type", type);//useFeet userWheel
            map.put("duration", accumulateTime);//米
            map.put("distance", accumulateDistance);//秒
            map.put("startTime", startTime.format(DATE_TIME_FORMATTER));//yyyy:MM:dd hh:mm:ss
            map.put("endTime", endTime.format(DATE_TIME_FORMATTER));
            recordBackUp.detailValue = new Gson().toJson(map);
            RecordRecordRepository.INSTANCE.getSportStartTime().setValue(sportStartTime);
            recordBackUp.sync = 0;
            if (currentUser.id == 0L) {//如果是本地用户，不会上传至云端，就直接标记为已同步
                recordBackUp.sync = 1;
            }
            sportStartTime = 0L;
            rRRepository.getRecordBackUp().setValue(recordBackUp);
            //这里之前犯了错误，在这里更新了RecordFragment的柱状图
            //这个是错误的，summary只能够返回总结的数据，不能做其他的事情
            //因为用户是有可能取消summary的，如果你更新了，虽然数据库的数据没有影响，但是但钱的UI
            //显示的数据就和数据库中的数据不一致了
            //另外，由于这里更新了，后面用户submit了数据之后，又会提交一次，这就会导致数据是两倍
        }
    }

    private final IBinder binder = new MyBinder();
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "record_record_location";
    private volatile boolean pause = false;
    private volatile boolean quit = false;
    private LocationManager locationManager;
    private volatile float maxAccelerate = 10f;
    private volatile String type;
    private final MyLocationListener locationListener = new MyLocationListener();
    private long sportStartTime = 0L;

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
        sportStartTime = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.maxAccelerate = intent.getFloatExtra("maxAccelerate", 10f);
        this.type = intent.getStringExtra("recordType");
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

    public void cancel() {
        this.locationListener.cancel();
        //cancel之后需要更新一波UI，因为这个时候还是没有位置记录，不会触发UI的自动更新
        RecordRecordRepository.INSTANCE.initValue();
    }

    public void resume() {
        pause = false;
        if (sportStartTime == 0L) {
            sportStartTime = System.currentTimeMillis();
        }
    }

    public void destroy() {
        quit = true;
        locationListener.quit();
        stopForeground(true);
        stopSelf();
    }
}