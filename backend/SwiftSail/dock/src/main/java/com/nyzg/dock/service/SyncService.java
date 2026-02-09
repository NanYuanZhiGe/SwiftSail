package com.nyzg.dock.service;

import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.dbobj.Record;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.RecordBackUpTableMapper;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.netobj.DayRecord;
import com.nyzg.dock.netobj.RecordBackUpAndroid;
import com.nyzg.dock.netobj.WatchSyncCheckReq;
import com.nyzg.dock.netobj.WatchSyncCheckResp;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SyncService {
    private final HttpResp SYNC_BACKUP_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "运动记录同步失败");
    private final HttpResp SYNC_BACKUP_SUCCEED = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "运动记录同步成功");
    private final HttpResp SYNC_QUERY_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "查询同步状态失败");
    @Resource
    RecordBackUpTableMapper recordBackUpMapper;
    @Resource
    WatchTableMapper watchTableMapper;
    @Resource
    JdbcTemplate jdbcTemplate;
    @Resource
    PlatformTransactionManager transactionManager;

    /**
     * 插入数据，不会多插或重复插
     */
    @Async
    public void insertDataIntoDbAsync(long userId, long epochDay, @NonNull List<Record> recordList) {
        new TransactionTemplate(transactionManager).execute(action -> {
            try {
            /*
            这里必须使用for update来强制保证事务之间顺序进行。举一个例子，假设用户快速地查询同一天的数据，
            然后这一天的数据并没有在数据库中，第一次请求还没有写数据库的时候第二次请求就查完数据了，导致两个
            请求都通过网络请求拿到了数据，然后来到这个地方异步更新数据库。它们的数据都是一样的
            当前项目数据库的事务隔离级别是RR，也就是说，这两个事务A、B进来的时候，MVCC会给它们分配唯一一个数据库视图
            而由于这两个事务都可能是活动事务，导致它们都认为数据库是空的，导致它们全部都执行delete和insert，这就很没有必要了
            所以必须select的时候加一个for update加锁，如果你select到了数据，说明别人已经插入了数据，你就不用了
             */
                Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM `recordTable` WHERE `userId`=? AND `epochDay`=? FOR UPDATE;
                        """, new BeanPropertyRowMapper<>(Integer.class), userId, epochDay);
                if (count != null && count > 0) {
                    return null;
                }
                //如果有数据干扰的话，这里也是能够删除的
                jdbcTemplate.update("""
                        DELETE FROM `recordTable` WHERE `userId`=? AND `epochDay`=?;
                        """, userId, epochDay);
                jdbcTemplate.batchUpdate("""
                                INSERT INTO recordTable\s
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
            } catch (Exception e) {
                log.info(e.getCause().getMessage());
            }
            return null;
        });
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

    @Nullable
    public Watch getCurrentActivateWatch(long userId) {
        try {
            List<Watch> watchList = jdbcTemplate.query("""
                    SELECT `accessToken`,`watchUserId` FROM `watchTable` WHERE `userId`=? AND activate>0;
                    """, (rs, row) -> new Watch(
                    0L, null,
                    null, rs.getString("watchUserId"),
                    null, null,
                    null, rs.getString("accessToken"),
                    null, 0L,
                    1, null
            ), userId);
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
