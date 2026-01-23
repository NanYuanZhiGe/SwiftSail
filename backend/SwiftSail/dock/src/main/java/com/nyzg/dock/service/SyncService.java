package com.nyzg.dock.service;

import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.mapper.RecordBackUpTableMapper;
import com.nyzg.dock.netobj.RecordBackUpAndroid;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncService {
    private final HttpResp SYNC_BACKUP_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "运动记录同步失败");
    private final HttpResp SYNC_BACKUP_SUCCEED = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "运动记录同步成功");
    @Resource
    RecordBackUpTableMapper recordBackUpMapper;

    public HttpResp syncBackUpRecord(List<RecordBackUpAndroid> recordList) {
        try {
            recordBackUpMapper.insertRecordBackUpAndroid(recordList);
        } catch (Exception e) {
            return SYNC_BACKUP_FAIL;
        }
        return SYNC_BACKUP_SUCCEED;
    }
}
