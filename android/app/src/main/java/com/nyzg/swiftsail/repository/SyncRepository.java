package com.nyzg.swiftsail.repository;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationManager;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.ChannelId;
import com.nyzg.swiftsail.obj.Pair;

import java.util.concurrent.atomic.AtomicBoolean;

public class SyncRepository {
    volatile private static SyncRepository INSTANCE;

    public MutableLiveData<Pair<String,Void>> notificationPair =new MutableLiveData<>();

    public MutableLiveData<Float> totalSyncProgress=new MutableLiveData<>(.0f);
    public final AtomicBoolean onSync = new AtomicBoolean(false);
    private SyncRepository() {
    }

    public static SyncRepository getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (SyncRepository.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new SyncRepository();
        }
        return INSTANCE;
    }
}
