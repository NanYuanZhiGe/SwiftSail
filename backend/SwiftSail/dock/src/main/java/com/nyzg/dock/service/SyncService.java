package com.nyzg.dock.service;

import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.dbobj.Record;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.RecordBackUpTableMapper;
import com.nyzg.dock.netobj.DayRecord;
import com.nyzg.dock.netobj.RecordBackUpAndroid;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SyncService {
    private final HttpResp SYNC_BACKUP_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "运动记录同步失败");
    private final HttpResp SYNC_BACKUP_SUCCEED = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "运动记录同步成功");
    @Resource
    RecordBackUpTableMapper recordBackUpMapper;
    @Resource
    JdbcTemplate jdbcTemplate;
    @Resource
    PlatformTransactionManager transactionManager;

    /**
     * 插入数据，不会多插或重复插
     */
    @Async
    public void insertDataIntoDbAsync( @NonNull List<Record> recordList) {
        new TransactionTemplate(transactionManager).execute(action -> {
                    jdbcTemplate.batchUpdate("""
                                    INSERT IGNORE INTO recordTable\s
                                    (id, userId, recordId, type, exposeValue, detailValue, epochDay, epochWeek, epochMonth, epochYear)\s
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                    """, recordList, recordList.size(),
                            (PreparedStatement ps, Record r) -> {
                                ps.setObject(1, null);
                                ps.setLong(2, r.getUserId());
                                ps.setString(3, UUID.randomUUID().toString());
                                ps.setString(4, r.getType());
                                ps.setLong(5, r.getExposeValue());
                                ps.setString(6, r.getDetailValue());
                                ps.setLong(7, r.getEpochDay());
                                ps.setLong(8, r.getEpochWeek());
                                ps.setLong(9, r.getEpochMonth());
                                ps.setLong(10, r.getEpochYear());
                            }
                    );
                    return null;
                }
        );
    }

    @Nullable
    public DayRecord getDayRecordFromDb(long userId, long epochDay) {
        try {
            List<Record> recordList = jdbcTemplate.query("""
                    SELECT * FROM `recordTable` WHERE `userId`=? AND `epochDay`=?;
                    """, new BeanPropertyRowMapper<>(Record.class), userId, epochDay);
            return new DayRecord(recordList);
        } catch (Exception e) {
            log.info(String.format("查询%d的%d日的数据出错：%s", userId, epochDay, e.getCause()));
        }
        return null;
    }

    /**
     * 注意，返回的watch里面只有watchUserId和accessToken
     */
    @Nullable
    public Watch getCurrentActivateWatch(long userId) {
        try {
            long currentTime = System.currentTimeMillis();
            List<Watch> watchList = jdbcTemplate.query("""
                    SELECT `accessToken`,`watchUserId` FROM `watchTable` WHERE `userId`=? AND activate>0 AND `expireTime`>?;
                    """, (rs, row) -> new Watch(
                    0L, null,
                    null, rs.getString("watchUserId"),
                    null, null,
                    null, rs.getString("accessToken"),
                    null, 0L,
                    1, null
            ), userId, currentTime);
            if (watchList.isEmpty()) {
                return null;
            }
            return watchList.get(0);
        } catch (Exception e) {
            log.info(e.getCause().getMessage());
            return null;
        }
    }

    public HttpResp syncBackUpRecord(List<RecordBackUpAndroid> recordList) {
        try {
            recordBackUpMapper.insertRecordBackUpAndroid(recordList);
        } catch (Exception e) {
            return SYNC_BACKUP_FAIL;
        }
        return SYNC_BACKUP_SUCCEED;
    }
}
