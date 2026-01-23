package com.nyzg.dock.controller;

import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.netobj.SyncRecordBackUpReq;
import com.nyzg.dock.netobj.RecordBackUpAndroid;
import com.nyzg.dock.service.SyncService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SyncController {
    static private final HttpResp TEST_SUCCESS = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "HELLO WORLD");
    private static final HttpResp TOKEN_EXPIRE = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "token过期！请删除设备后重新添加！");
    @Resource
    SyncService syncService;

    @PostMapping(path = "/sync/stop")
    public HttpResp syncStop(){

        return null;
    }

    /**
     * 是由可能出现token过期的现象（一般都是服务器自己内部的问题），需要返回给用户自己处理
     */
    @PostMapping(path = "/sync/record/backup")
    public HttpResp syncRecordBackUp(@RequestBody SyncRecordBackUpReq req) {
        if (req == null || req.getRecordList() == null || req.getRecordList().isEmpty()) {
            return HttpResp.COMMON_SUCCESS;
        }
        //禁止脏数据进入
        for (RecordBackUpAndroid record : req.getRecordList()) {
            if (record.getUserId() == 0L) {
                return HttpResp.COMMON_SUCCESS;
            }
        }
        return syncService.syncBackUpRecord(req.getRecordList());
    }

    @GetMapping("/test/connection")
    public HttpResp testConnection() {
        return TEST_SUCCESS;
    }
}
