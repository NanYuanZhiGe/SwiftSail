package com.nyzg.swiftsail.repository;


import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.SQLiteDB;

import java.util.concurrent.CompletableFuture;

public class NetDataRepository {
    private static volatile NetDataRepository self;

    //fileName
    public MutableLiveData<String> headIconNotifier = new MutableLiveData<>();

    private NetDataRepository() {
        LoginRepository.getInstance().currentUser.observeForever(user -> {
            if (user == null
                    || user.id == GlobalInstance.LOCAL_USER.id) {
                return;
            }
            final long userId = user.id;
            CompletableFuture.runAsync(() -> {
                String headIconUrl = SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                        .shareTable()
                        .getHeadIcon(userId, "headIconUrl");
                if (headIconUrl == null) {
                    return;
                }
                headIconNotifier.postValue(headIconUrl);
            });
        });
    }

    public static NetDataRepository getInstance() {
        if (self != null) {
            return self;
        }
        synchronized (NetDataRepository.class) {
            if (self != null) {
                return self;
            }
            self = new NetDataRepository();
            return self;
        }
    }
}
