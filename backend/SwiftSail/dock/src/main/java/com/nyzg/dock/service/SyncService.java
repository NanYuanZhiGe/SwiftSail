package com.nyzg.dock.service;

import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.RecordBackUpTableMapper;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.netobj.RecordBackUpAndroid;
import com.nyzg.dock.netobj.WatchSyncCheckReq;
import com.nyzg.dock.netobj.WatchSyncCheckResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SyncService {
    private final HttpResp SYNC_BACKUP_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "运动记录同步失败");
    private final HttpResp SYNC_BACKUP_SUCCEED = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "运动记录同步成功");
    private final HttpResp SYNC_QUERY_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "查询同步状态失败");
    @Resource
    RecordBackUpTableMapper recordBackUpMapper;
    @Resource
    WatchTableMapper watchTableMapper;

    public HttpResp syncBackUpRecord(List<RecordBackUpAndroid> recordList) {
        try {
            recordBackUpMapper.insertRecordBackUpAndroid(recordList);
        } catch (Exception e) {
            return SYNC_BACKUP_FAIL;
        }
        return SYNC_BACKUP_SUCCEED;
    }

    public HttpResp checkSyncStatus(WatchSyncCheckReq req) {
        WatchSyncCheckResp resp = new WatchSyncCheckResp();
        resp.setUserId(resp.getUserId());
        Map<String, Boolean> map = new HashMap<>();
        try {
            List<Watch> watchList = watchTableMapper.checkSyncStatus(req.getUserId(), req.getClientIdList());
            for (String clientId : req.getClientIdList()) {
                map.put(clientId, false);
            }
            long now = System.currentTimeMillis();
            for (Watch watch : watchList) {
                if (watch.getExpireTime() > now) {
                    map.put(watch.getClientId(), true);
                }
            }
            resp.setResultMap(map);
        } catch (Exception e) {
            return SYNC_QUERY_FAIL;
        }
        return new HttpResp(true, resp);
    }
}
