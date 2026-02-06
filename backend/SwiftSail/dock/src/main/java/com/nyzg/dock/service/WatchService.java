package com.nyzg.dock.service;

import com.nyzg.common.Pair;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.netobj.WatchAddReq;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class WatchService {
    @Resource
    RedissonClient redissonClient;
    @Resource
    WatchTableMapper watchTableMapper;
    private static final HttpResp GRANT_EXPIRE_OR_NOT_FOUND = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "未授权或授权已过期");
    private static final HttpResp SERVER_ADD_WATCH_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "服务端添加手表错误");
    private static final Pair<HttpResp, String> PAIR_GRANT_EXPIRE_OR_NOT_FOUND = new Pair<>(GRANT_EXPIRE_OR_NOT_FOUND, null);
    private static final Pair<HttpResp, String> PAIR_SERVER_ADD_WATCH_FAIL = new Pair<>(SERVER_ADD_WATCH_FAIL, null);
    private static final Pair<HttpResp, String> PAIR_CURRENT_HAS_ACTIVATE_WATCH = new Pair<>(new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "当前已有在工作的手表，请先删除或下线"), null);
    private static final String DOCK_WATCH_STATE = "dock:watch:state:";

    /**
     * @return 返回值第一个是应该返回给客户端的响应，包含里面的错误原因，第二值是客户端之前存在Redis中的授权码
     */
    @Transactional
    public Pair<HttpResp, String> checkIsCallbackSuccessAndDoDatabase(WatchAddReq req) {
        RBucket<String> rBucket = redissonClient.getBucket(DOCK_WATCH_STATE + req.getState());
        if (!rBucket.isExists()) {//没有授权码
            return PAIR_GRANT_EXPIRE_OR_NOT_FOUND;
        }
        //获取授权码
        String code = rBucket.get();
        rBucket.delete();
        //写入数据库
        try {
            Watch watch = new Watch();
            watch.setUserId(req.userId);
            watch.setName(req.name);
            watch.setClientId(req.clientId);
            watch.setWatchType(req.watchType);
            watch.setAuthorizeHeader(req.authorizeHeader);
            watch.setCodeVerifier(req.codeVerifier);
            List<Watch> watchList = watchTableMapper.selectWatchByClientId(req.userId, req.clientId);
            if (watchList.isEmpty()) {//如果之前没有添加过这个手表，就添加
                watch.setActivate(1);
                log.info("没有手表");
                watchTableMapper.insertWatch(watch);
            } else {//如果添加过，就更新
                long now = System.currentTimeMillis();
                boolean hasActivateWatch = false;
                for (Watch query : watchList) {
                    if (query.getExpireTime() > now && query.getActivate() > 0) {
                        hasActivateWatch = true;
                        break;
                    }
                }
                if (hasActivateWatch) {//已经有在激活的手表了
                    watch.setActivate(0);
                    log.info("已经有手表了");
                    watchTableMapper.updateWatch(watch, req.clientId);
                    return PAIR_CURRENT_HAS_ACTIVATE_WATCH;
                }
                watch.setActivate(1);
                log.info("有手表，但是全部是未激活的状态");
                watchTableMapper.updateWatch(watch, req.clientId);
            }
        } catch (Exception ignore) {
            return PAIR_SERVER_ADD_WATCH_FAIL;
        }
        //成功写入手表数据，返回成功
        return new Pair<>(HttpResp.COMMON_SUCCESS, code);
    }

    /**
     * 在Redis中存储state和code
     */
    public void saveStateAndCode(String state, String code) {
        RBucket<String> rBucket = redissonClient.getBucket(DOCK_WATCH_STATE + state);
        rBucket.set(code, Duration.ofSeconds(3600));
    }
}
