package com.nyzg.dock.service;

import com.nyzg.common.netobj.HttpResp;
import com.nyzg.dock.jobs.JobFitbitEightHour;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.obj.FitbitOath2Token;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
public class QuartzService {
    @Resource
    FitbitWebApiService fitbitWebApiService;
    @Resource
    WatchTableMapper watchTableMapper;
    @Resource
    Scheduler scheduler;
    private static final String TRIGGER_FITBIT_EIGHT_HOUR_PREFIX = "jobFitbitEightHour-";
    private static final String TRIGGER_FITBIT_EIGHT_HOUR_GROUP = "TriggerFitbitEightHour";
    private static final HttpResp GET_FIRST_TOKEN_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "初次获取fitbit的accessToken失败");
    private static final HttpResp UPDATE_WATCH_TOKEN_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "更新手表授权码错误");
    private static final HttpResp START_SCHEDULER_ERROR = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "后台定时任务启动失败");

    /**
     * 异步添加定时任务
     * 首先是拉取一次token和refresh Token，写一次数据库
     * 然后是添加两个任务，一个是每天12：00（中午）拉取一次数据的任务
     * 另外一个是7小时拉取一次新Token的任务
     */
    public HttpResp addFitbitPullSchedule(
            long userId,
            String clientId, String authorizeHeader,
            String code, String codeVerifier) {
        Optional<FitbitOath2Token> result = fitbitWebApiService.getTokenSync(clientId, authorizeHeader, code, codeVerifier);
        if (result.isEmpty()) {
            return GET_FIRST_TOKEN_FAIL;
        }
        FitbitOath2Token token = result.get();
        //这里获取了fitbit的token，然后写一次数据库
        //过期时间是28800秒，然后转化为毫秒就是乘以1000
        long expireTime = System.currentTimeMillis() + 28800 * 1000;
        try {
            watchTableMapper.updateWatchToken(
                    clientId,
                    token.getAccessToken(),
                    token.getRefreshToken(),
                    token.getUserId(),
                    expireTime
            );
        } catch (Exception e) {
            return UPDATE_WATCH_TOKEN_FAIL;
        }

        //--------------------------每7小时refresh一波token------------------------------------
        try {
            TriggerKey triggerKey = new TriggerKey(TRIGGER_FITBIT_EIGHT_HOUR_PREFIX + clientId, TRIGGER_FITBIT_EIGHT_HOUR_GROUP);
            if (!scheduler.checkExists(triggerKey)) {
                //执行拉取数据的job
                JobDetail jobDetailEightHour = JobBuilder.newJob(JobFitbitEightHour.class)
                        .withIdentity("jobFitbitEightHour-" + clientId, "JobFitbitEightHour")
                        .usingJobData("clientId", clientId)
                        .build();
                Trigger triggerEightHour = TriggerBuilder.newTrigger()
                        .withIdentity(TRIGGER_FITBIT_EIGHT_HOUR_PREFIX + clientId, TRIGGER_FITBIT_EIGHT_HOUR_GROUP)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInHours(7)
                                .repeatForever())
                        .build();
                //执行刷新token的job
                scheduler.scheduleJob(jobDetailEightHour, triggerEightHour);
            }
        } catch (Exception e) {
            log.error("无法启动Fitbit每周任务" + clientId + e.getCause());
            stopSchedulerAndDelete(userId, clientId);
            return START_SCHEDULER_ERROR;
        }
        return HttpResp.COMMON_SUCCESS;
    }

    /**
     * 停止拉取数据：
     * 1. 取消定时任务
     * 2. 删除数据库中的watch
     */
    public void stopSchedulerAndDelete(long userId, String clientId) {
        TriggerKey triggerKey1 = new TriggerKey(
                TRIGGER_FITBIT_EIGHT_HOUR_PREFIX + clientId,
                TRIGGER_FITBIT_EIGHT_HOUR_GROUP
        );
        try {
            scheduler.unscheduleJob(triggerKey1);
            watchTableMapper.deleteWatch(userId, clientId);
        } catch (Exception e) {
            log.info(e.getCause().getMessage());
        }
    }

    public void stopSchedulerOnly(String clientId) {
        TriggerKey triggerKey1 = new TriggerKey(
                TRIGGER_FITBIT_EIGHT_HOUR_PREFIX + clientId,
                TRIGGER_FITBIT_EIGHT_HOUR_GROUP
        );
        try {
            scheduler.unscheduleJob(triggerKey1);
        } catch (Exception e) {
            log.info(e.getCause().getMessage());
        }
    }
}