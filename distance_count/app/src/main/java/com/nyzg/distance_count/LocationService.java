package com.nyzg.distance_count;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Interpolator;
import android.location.Location;
import android.location.LocationRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class LocationService extends Service implements Executor {

    private static volatile Location lastLocation = null;
    private static final AtomicReference<Double> movedMeters = new AtomicReference<>(.0);
    private static volatile FusedLocationProviderClient locationServices = null;
    Runnable r = null;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(129, createNotification());

        locationServices = LocationServices.getFusedLocationProviderClient(this);
        if (r == null) {
            r = () -> {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(GlobalApplication.getAppContext(), "应用权限不足", Toast.LENGTH_LONG).show();
                    return;
                }
                locationServices.getCurrentLocation(new CurrentLocationRequest.Builder()
                        .setMaxUpdateAgeMillis(1000)
                        .setDurationMillis(2000)
                        .build(), null
                ).addOnSuccessListener(this, location -> {
                    if (location == null) {//请求失败或者是超时
                        Log.v("myTag", "request failed");
                        return;
                    }
                    Log.v("myTag", "request success");
                    if (lastLocation == null) {
                        lastLocation = location;
                        return;
                    }
                    double temp = movedMeters.get();
                    movedMeters.set(temp + location.distanceTo(lastLocation));
                    lastLocation = location;
                    Message message = Message.obtain();
                    message.what = MainActivity.CHANGE_TEXT;
                    message.obj = String.format("%s", movedMeters.get());
                    MainActivity.mhandler.sendMessage(message);
                    handler.postDelayed(r, 1000);
                });
            };
            handler.post(r);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                "location Service",
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, "location Service")
                .setContentTitle("距离统计服务运行中")
                .setContentText("正在记录您的移动距离...")
                .setSmallIcon(R.drawable.ic_launcher_background) // 替换为你自己的图标，或用系统默认
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void execute(Runnable command) {

    }
}
