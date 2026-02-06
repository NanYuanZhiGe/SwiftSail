package com.nyzg.dock.jobs;

import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.service.FitbitWebApiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
public class JobFitbitEightHour implements Job {
    @Resource
    WatchTableMapper watchTableMapper;
    @Resource
    FitbitWebApiService fitbitWebApiService;
    @Resource
    Scheduler scheduler;

    /**
     * 执行任务的时候需要进行下面的这个逻辑：
     * 检查数据库中的expireTime，验证现在的refreshToken是否还有效，
     * 如果无效了就停止另外的JobFitbitDaily
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        String clientId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("clientId");
        String jobId = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
        if (clientId == null) {
            log.info(String.format("查询不到clientId, id:%s, group: %s", jobId, jobGroup));
            return;
        }
        Watch watch = watchTableMapper.checkExpireTime(clientId);
        //认为接下来的五分钟内一定可以完成操作，完不成再说
        long usefulTime = System.currentTimeMillis() + 5 * 60 * 1000;
        if (watch.getExpireTime() < usefulTime) {
            log.info(String.format("id: %s, group %s，的token已经过期，无法刷新，需要用户授权", jobId, jobGroup));
            return;
        } else if (watch.getAuthorizeHeader() == null) {
            log.info(String.format("id: %s, group %s，的授权头为空，无法进行刷星token", jobId, jobGroup));
            return;
        }
        //接下来调用fitbit的API进行token的刷新操作
        long nextExpireTime = System.currentTimeMillis();
        fitbitWebApiService.getRefreshToken(
                watch.getAuthorizeHeader(), watch.getRefreshToken(),
                //如果在网络请求期间token过期了，这里会返回401的错误，日志会显示
                //不过实际上fitbit是8小时过期一次，我们是7小时刷一次，除非服务器长时间宕机，这个一般不会又问题
                //如果失败了，就同时取消拉取数据的任务
                message -> {
                    log.info(String.format("id: %s, group: %s 的fresh请求失败，具体原因为：%s", jobId, jobGroup, message));
                    log.info(String.format("id: %s, group: %s 使用的authorizeHeader: %s和refreshToken: %s", jobId, jobGroup, watch.getAuthorizeHeader(), watch.getRefreshToken()));
                    TriggerKey triggerKey = new TriggerKey(
                            "triggerFitbitDaily-" + jobId.replace("jobFitbitEightHour-", ""),
                            "TriggerFitbitDaily");
                    try {
                        scheduler.unscheduleJob(triggerKey);
                    } catch (Exception e) {
                        log.info(String.format("id: %s, group: %s 取消定时任务出错: %s", jobId, jobGroup, Arrays.toString(e.getStackTrace())));
                    }
                    //在数据库中标记expireTime
                    watchTableMapper.markWatchExpire(clientId);
                },
                resp -> {
                    //刷新数据库
                    watchTableMapper.updateRefreshToken(
                            clientId,
                            resp.getAccessToken(),
                            resp.getRefreshToken(),
                            nextExpireTime + resp.getExpiresIn()
                    );
                    log.info(String.format("id: %s, group: %s token刷新成功：%s", jobId, jobGroup, LocalDateTime.now()));
                }
        );
    }
}
