package com.nyzg.swiftsail.repository;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.obj.Pair;

import java.util.concurrent.atomic.AtomicBoolean;

public class SyncRepository {
    volatile private static SyncRepository INSTANCE;

    public MutableLiveData<Pair<String, Void>> notificationPair = new MutableLiveData<>();
    public final AtomicBoolean onSync = new AtomicBoolean(false);

    //数据同步

    public static final int SYNC_START=-1000;
    public static final int SYNC_FAIL=-1001;
    public static final int SYNC_SUCCESS=-1002;
    volatile public int syncTotal = 0;
    public MutableLiveData<Integer> currentSync = new MutableLiveData<>(0);

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
