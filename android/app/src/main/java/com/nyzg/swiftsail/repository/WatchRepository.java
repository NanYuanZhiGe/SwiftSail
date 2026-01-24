package com.nyzg.swiftsail.repository;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dao.WatchTable;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.dbobj.Watch;
import com.nyzg.swiftsail.netobj.report.WatchSyncCheckReq;
import com.nyzg.swiftsail.netobj.report.WatchSyncCheckResp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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


    public void getWatchListAsync() {
        User user = LoginRepository.getInstance().currentUser.getValue();
        if (user == null) {
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            WatchTable watchTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).watchTable();
            List<Watch> queryList = watchTable.selectWatchByUserId(user.id);
            List<String> clientIdList = new ArrayList<>(queryList.size());
            Map<String, Watch> queryMap = new HashMap<>();
            for (Watch watch : queryList) {
                queryMap.put(watch.clientId, watch);
                clientIdList.add(watch.clientId);
            }
            NetWorkHandler.handleNetRespAfterLogin(
                    null,
                    NetWorkBuilder.doChunkRequest(NetWorkBuilder.buildJsonRequestJwt(
                            ServerURL.URL_CHECK_SYNC_STATUS, ServerURL.POST, new WatchSyncCheckReq(user.id, clientIdList)
                    )),
                    () -> {
                        GlobalToast.COMMON_TOAST.accept("无法获取服务器的数据，手表同步状态为上次数据");
                        watchList.postValue(queryList);
                    },
                    resp -> {
                        List<Watch> resultList = new ArrayList<>(queryList.size());
                        for (Map.Entry<String, Boolean> entry : resp.resultMap.entrySet()) {
                            Watch watch = queryMap.get(entry.getKey());
                            if (watch != null) {
                                watch.accessible = entry.getValue();
                            }
                            resultList.add(watch);
                        }
                        watchList.postValue(resultList);
                    },
                    WatchSyncCheckResp.class
            );
            return null;
        });
    }

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
