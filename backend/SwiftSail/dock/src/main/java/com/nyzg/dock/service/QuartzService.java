package com.nyzg.dock.service;

import com.nyzg.dock.jobs.JobFitbitDaily;
import com.nyzg.dock.jobs.JobFitbitEightHour;
import com.nyzg.dock.mapper.WatchTableMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class QuartzService {
    @Resource
    FitbitWebApiService fitbitWebApiService;
    @Resource
    WatchTableMapper watchTableMapper;
    @Resource
    Scheduler scheduler;

    /**
     * 异步添加定时任务
     * 首先是拉取一次token和refresh Token，写一次数据库
     * 然后是添加两个任务，一个是每天12：00（中午）拉取一次数据的任务
     * 另外一个是每7天拉取一次新Token的任务
     */
    @Async
    public void addFitbitPullSchedule(
            String clientId, String authorizeHeader,
            String code, String codeVerifier
    ) {
        fitbitWebApiService.getTokenAsync(
                clientId, authorizeHeader,
                code, codeVerifier
        ).thenAccept(action -> action.ifPresent(token -> {
            //这里获取了fitbit的token，然后写一次数据库
            //过期时间是28800秒，然后转化为毫秒就是乘以1000
            long expireTime = System.currentTimeMillis() + 28800 * 1000;
            watchTableMapper.updateWatchToken(
                    clientId,
                    token.getAccessToken(),
                    token.getRefreshToken(),
                    token.getUserId(),
                    expireTime
            );
            //然后是开启两个任务，一个是每天中午12：00拉取一次数据
            //任务的id就是jobFitbitDaily<ClientId>
            //trigger的id是triggerFitbitWeek<ClientId>
            JobDetail jobDetailDaily = JobBuilder.newJob(JobFitbitDaily.class)
                    .withIdentity("jobFitbitDaily-" + clientId, "JobFitbitDaily")
                    .usingJobData("clientId", clientId)
                    .build();
            Trigger triggerDaily = TriggerBuilder.newTrigger()
                    .withIdentity("triggerFitbitDaily-" + clientId, "TriggerFitbitDaily")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 12 * * ?"))
                    .build();
            //执行拉取数据的job
            try {
                scheduler.scheduleJob(jobDetailDaily, triggerDaily);
            } catch (Exception e) {
                log.error("无法启动Fitbit每日任务" + clientId);
                return;
            }
            //一个是每7小时refresh一波token
            JobDetail jobDetailEightHour = JobBuilder.newJob(JobFitbitEightHour.class)
                    .withIdentity("jobFitbitEightHour-" + clientId, "JobFitbitEightHour")
                    .usingJobData("clientId", clientId)
                    .build();
            Trigger triggerWeek = TriggerBuilder.newTrigger()
                    .withIdentity("triggerFitbitEightHour-" + clientId, "TriggerFitbitEightHour")
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInHours(7)
                            .repeatForever())
                    .build();
            //执行刷新token的job
            try {
                scheduler.scheduleJob(jobDetailEightHour, triggerWeek);
            } catch (Exception e) {
                log.error("无法启动Fitbit每周任务" + clientId);
            }
        }));
    }
}
