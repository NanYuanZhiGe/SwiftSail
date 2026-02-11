package com.nyzg.dock.controller;

import com.nyzg.common.Pair;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.netobj.DayRecord;
import com.nyzg.dock.netobj.GetDataDayReq;
import com.nyzg.dock.netobj.RecordBackUpAndroid;
import com.nyzg.dock.netobj.SyncRecordBackUpReq;
import com.nyzg.dock.service.FitbitWebApiService;
import com.nyzg.dock.service.SyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@Slf4j
public class SyncController {
    private static final long LEAST_START_EPOCH = LocalDate.of(2025, 11, 1).toEpochDay();
    static private final HttpResp TEST_SUCCESS = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "HELLO WORLD");
    private final static HttpResp INVALID_DAY = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "不合法的日期");
    private final static HttpResp NETWORK_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "请求手表数据网络错误");
    private final static HttpResp NO_WORKING_WATCH = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "没有正在工作的手表");
    private final static HttpResp USER_NO_PUSH_DATA = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "用户未上传数据");
    @Resource
    SyncService syncService;
    @Resource
    FitbitWebApiService fitbitWebApiService;

    /**
     * 获取某一天的数据，一个DayRecord就是一个List<Record>
     * 里面包含了不同类型的Record
     * 这个方法是原子性的，要么获取往所有的Record，要么就失败
     * 不会出现DayRecord中部分包含某一天的Record，比如就只有睡眠的Record，没有其他的
     *
     * @param req 内部是epochDay long
     * @return content为DayRecord
     */
    @PostMapping(path = "/get/data/day")
    public HttpResp getDataDay(
            @RequestHeader("userId") String userId,
            @RequestBody GetDataDayReq req) {
        if (req == null || req.getDay() < LEAST_START_EPOCH) {
            return INVALID_DAY;
        }
        long queryUserId = Long.parseLong(userId);
        //先从数据库中查询数据
        boolean isToday = LocalDate.now().toEpochDay() == req.getDay();
        //需要注意的是，如果用户查询的数据是今天，不能走数据库的逻辑，因为“今天”的数据是不完整的
        //比如用户中午的数据和晚上的数据是会不一致的，所以不能直接写入数据库，那自然这里也没有必要查数据库
        if (!isToday) {
            DayRecord dayRecord = syncService.getDayRecordFromDb(queryUserId, req.getDay());
            if (dayRecord != null && !dayRecord.getRecordList().isEmpty()) {
                return new HttpResp(true, dayRecord);
            }
        }
        //获取当前可用的手表
        Watch currentWatch = syncService.getCurrentActivateWatch(queryUserId);
        if (currentWatch == null) {
            return NO_WORKING_WATCH;
        }
        //网络请求数据
        Pair<FitbitWebApiService.QueryStatus, DayRecord> pair = fitbitWebApiService.getDayRecordNullAtFail(
                queryUserId,
                currentWatch.getAccessToken(),
                currentWatch.getWatchUserId(),
                req.getDay()
        );
        if (pair.getA() == FitbitWebApiService.QueryStatus.FAIL) {
            return NETWORK_FAIL;
        } else if (pair.getA() == FitbitWebApiService.QueryStatus.NO_DATA) {
            return USER_NO_PUSH_DATA;
        }
        //异步写入数据库，这里就算写不进也没有关系，如果有东西出错，下一次请求会写入的
        if (!isToday) {
            syncService.insertDataIntoDbAsync(pair.getB().getRecordList());
        }
        return new HttpResp(true, pair.getB());
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
