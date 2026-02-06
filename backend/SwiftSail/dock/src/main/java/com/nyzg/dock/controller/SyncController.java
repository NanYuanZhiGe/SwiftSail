package com.nyzg.dock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyzg.common.Pair;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.dbobj.Record;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.RecordTableMapper;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.netobj.*;
import com.nyzg.dock.service.FitbitWebApiService;
import com.nyzg.dock.service.SyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@RestController
@Slf4j
public class SyncController {
    private static final long LEAST_START_EPOCH = LocalDate.of(2025, 11, 1).toEpochDay();
    static private final HttpResp TEST_SUCCESS = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "HELLO WORLD");
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static byte[] CHANGE_LINE = "\n".getBytes(StandardCharsets.UTF_8);
    private final static byte[] END_RESPONSE = "{{\"_eof\":true}}".getBytes(StandardCharsets.UTF_8);
    private final static HttpResp INVALID_DAY = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "不合法的日期");
    private final static HttpResp NETWORK_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "请求手表数据网络错误");
    private final static HttpResp NO_WORKING_WATCH = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "没有正在工作的手表");
    private final static HttpResp USER_NO_PUSH_DATA = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "用户未上传数据");
    @Resource
    SyncService syncService;
    @Resource
    RecordTableMapper recordTableMapper;
    @Resource
    WatchTableMapper watchTableMapper;
    @Resource
    FitbitWebApiService fitbitWebApiService;

    @PostMapping(path = "/get/data/day")
    public HttpResp getDataDay(
            @RequestHeader("userId") String userId,
            @RequestBody GetDataDayReq req) {
        if (req == null || req.getDay() < LEAST_START_EPOCH) {
            return INVALID_DAY;
        }
        long queryUserId = Long.parseLong(userId);
        //先从数据库中查询数据
        {
            DayRecord dayRecord = syncService.getDayRecordFromDb(queryUserId, req.getDay());
            if (dayRecord != null && !dayRecord.getRecordList().isEmpty()) {
                return new HttpResp(true, dayRecord);
            }
        }
        Watch currentWatch = syncService.getCurrentActivateWatch(queryUserId);
        if (currentWatch == null) {
            return NO_WORKING_WATCH;
        }
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
        CompletableFuture.supplyAsync(() -> {
            syncService.insertDataIntoDb(queryUserId, req.getDay(), pair.getB().getRecordList());
            return null;
        });
        return new HttpResp(true, pair.getB());
    }


    @PostMapping(path = "/acquire/sync/data")
    public ResponseEntity<StreamingResponseBody> acquireSyncData(
            @RequestHeader("userId") String userId,
            @RequestBody AcquireSyncDataReq req) {
        //+++++++++++++++++++校验clientId的合法性+++++++++++++++
        //clientId不能为空
        if (req.getClientId() == null || req.getClientId().isEmpty()) {
            log.info(String.format("userId: %s 的请求客户端clientId为空", userId));
            return ResponseEntity.badRequest().build();
        }
        //检查clientId和userId是否匹配
        long queryUserId = Long.parseLong(userId);
        String watchUserId = watchTableMapper.getWatchUserId(queryUserId, req.getClientId());
        //出现这种情况一般都是手表刚刚添加，然后用户立刻同步数据，此时
        //第一次的refresh token还没有结束，没有拿到watchUserId
        //或者是恶意构造了一个请求，此时我们不理他，直接返回
        if (watchUserId == null) {
            log.info(String.format("userId: %s 的请求clientId和userId不匹配", userId));
            return ResponseEntity.badRequest().build();
        }
        //+++++++++++++++++++校验leakDataList的合法性+++++++++++++++
        //没有数据就直接结束
        if (req.getLeakDataList() == null || req.getLeakDataList().isEmpty()) {
            return ResponseEntity.ok().build();
        }
        //----------------开始同步数据-----------------------
        StreamingResponseBody responseBody = outputStream -> {
            //遍历所有缺失的数据
            for (Pair<Long, Long> between : req.getLeakDataList()) {
                //+++++++++++++++++++起始日期合法性校验+++++++++++++++
                long startEpoch = between.getA();
                long endEpoch = between.getB();
                if (startEpoch > endEpoch) {
                    continue;
                }
                if (startEpoch < LEAST_START_EPOCH) {//2025/11/1
                    continue;
                }
                //进行本地分页查询，缺少的数据进行网络请求
                //分页大小为100
                long i = startEpoch;
                while (i <= endEpoch) {
                    //确定查询数据的起始
                    int querySize = 99;
                    if (i + querySize > endEpoch) {
                        querySize = (int) (endEpoch - i + 1);
                    }
                    //db中的数据按照epochDay进行排序
                    List<Record> dbList = recordTableMapper.selectRecordFromAndEnd(
                            queryUserId, i, i + querySize
                    );
                    //需要封装每天的不同数据到同一个dayRecord中，并且对于缺失的数据，需要进行网络请求
                    //分页查询的范围是[i,i+querySize]
                    long leakStart = i;
                    long lastEpoch = -1L;
                    DayRecord dayRecord = new DayRecord();

                    for (Record record : dbList) {
                        if (lastEpoch == -1L) {//第一次
                            dayRecord.getRecordList().add(record);
                            lastEpoch = record.getEpochDay();
                        }
                        //不是第一次了
                        //但是是同一天的数据
                        if (lastEpoch == record.getEpochDay()) {
                            dayRecord.getRecordList().add(record);
                            continue;
                        }
                        //到了新的一批数据
                        //可以序列化返回给客户端了
                        OBJECT_MAPPER.writeValue(outputStream, dayRecord);
                        outputStream.write(CHANGE_LINE);
                        //删除旧的数据，添加新的一天的数据
                        dayRecord.getRecordList().clear();
                        dayRecord.getRecordList().add(record);
                        //这个时候就需要检查是否缺失了数据
                        if (leakStart + 1 <= lastEpoch - 1) {//缺少了[leakStart+1,lastEpoch-1]的数据
                            //进行网络请求获取数据
                            Optional<List<DayRecord>> webRecordList = fitbitWebApiService.getRangeRecordAndSyncToDatabaseNullAtFail(
                                    queryUserId,
                                    req.getClientId(),
                                    leakStart,
                                    lastEpoch - 1
                            );
                            //值得注意的是，网络请求并不是一定能拿到具体的数据，也有可能拿不到数据
                            //这里面只会返回网络请求成功的数据，里面会有三次重试
                            //拿不到的数据要么是用户没有同步给手表厂商，要么就是网络异常
                            webRecordList.ifPresent(recordList -> {
                                for (DayRecord dr : recordList) {
                                    try {
                                        OBJECT_MAPPER.writeValue(outputStream, dr);
                                        outputStream.write(CHANGE_LINE);
                                    } catch (Exception e) {
                                        log.info("网络请求的结果DayRecord序列化错误：SyncController:acquireSyncData");
                                    }
                                }
                            });
                        }
                        //更新leakStart和lastEpoch
                        leakStart = record.getEpochDay();
                        lastEpoch = leakStart;
                    }
                    //处理最后一批数据
                    if (!dayRecord.getRecordList().isEmpty()) {
                        OBJECT_MAPPER.writeValue(outputStream, dayRecord);
                        outputStream.write(CHANGE_LINE);
                    }
                    if (leakStart + 1 <= endEpoch) {
                        fitbitWebApiService.getRangeRecordAndSyncToDatabaseNullAtFail(
                                queryUserId,
                                req.getClientId(),
                                leakStart,
                                lastEpoch - 1
                        ).ifPresent(recordList -> {
                            for (DayRecord dr : recordList) {
                                try {
                                    OBJECT_MAPPER.writeValue(outputStream, dr);
                                    outputStream.write(CHANGE_LINE);
                                } catch (Exception e) {
                                    log.info("网络请求的结果DayRecord序列化错误：SyncController:acquireSyncData");
                                }
                            }
                        });
                    }
                    i += querySize;
                    i += 1;
                }
            }
            //写入终止符
            outputStream.write(END_RESPONSE);
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
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
