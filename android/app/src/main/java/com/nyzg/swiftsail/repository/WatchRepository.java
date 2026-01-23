package com.nyzg.swiftsail.repository;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.dbobj.Watch;

import java.util.ArrayList;
import java.util.List;

public class WatchRepository {
    private volatile static WatchRepository INSTANCE;

    public static WatchRepository getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (WatchRepository.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new WatchRepository();
        }
        return INSTANCE;
    }

    private WatchRepository() {
    }

    public MutableLiveData<List<Watch>> watchList = new MutableLiveData<>(new ArrayList<>());

    /**
     * 这里必须得加锁，不然用户进行快速删除的时候，每一个异步任务看到的视图会不一样，
     * 导致比如：有A、B、C，用户删除了A和B，但是B看到的不是B、C而是A、B、C
     * 导致最后视图时A、C，和用户的删除预期不符
     */
    synchronized public void deleteWatchThreadSafe(long id) {
        List<Watch> watches = watchList.getValue();
        if (watches == null) {
            return;
        }
        List<Watch> newList = new ArrayList<>(watches);
        for (Watch watch : watches) {
            if (watch.id == id) {
                continue;
            }
            newList.add(watch);
        }
        watchList.postValue(newList);
    }
}
