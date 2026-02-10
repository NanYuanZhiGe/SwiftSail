package com.nyzg.dock.jobs;

import com.nyzg.dock.conf.JobConf;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.service.FitbitWebApiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

@Slf4j
public class JobFitbitEightHour implements Job {
    @Resource
    JdbcTemplate jdbcTemplate;
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
        final String clientId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(JobConf.KEY_CLIENT_ID);
        final long userId = (Long) jobExecutionContext.getJobDetail().getJobDataMap().get(JobConf.KEY_USER_ID);
        String jobId = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
        Watch watch = jdbcTemplate.query("""
                        SELECT * FROM `watchTable` WHERE `userId`=? AND `clientId`=?;
                        """, new BeanPropertyRowMapper<>(Watch.class), userId, clientId)
                .stream().findFirst().orElse(null);
        if (watch == null) {
            removeTrigger(clientId, jobId, jobGroup);
            return;
        }
        //认为接下来的五分钟内一定可以完成操作，完不成再说
        long usefulTime = System.currentTimeMillis() + 5 * 60 * 1000;
        if (watch.getExpireTime() < usefulTime) {
            log.info(String.format("id: %s, group %s，的token已经过期，无法刷新，需要用户授权", jobId, jobGroup));
            markAsExpire(userId, clientId);
            removeTrigger(clientId, jobId, jobGroup);
            return;
        }
        if (watch.getActivate() == 0) {
            log.info(String.format("id: %s, group %s，已被用户下线", jobId, jobGroup));
            removeTrigger(clientId, jobId, jobGroup);
            return;
        }
        if (watch.getAuthorizeHeader() == null || watch.getRefreshToken() == null) {
            log.info(String.format("id: %s, group %s，的授权头或refreshToken为空，无法进行刷新token", jobId, jobGroup));
            markAsExpire(userId, clientId);
            removeTrigger(clientId, jobId, jobGroup);
            return;
        }
        //接下来调用fitbit的API进行token的刷新操作
        fitbitWebApiService.getRefreshToken(
                watch.getAuthorizeHeader(), watch.getRefreshToken(),
                //如果在网络请求期间token过期了，这里会返回401的错误，日志会显示
                //不过实际上fitbit是8小时过期一次，我们是7小时刷一次，除非服务器长时间宕机，这个一般不会又问题
                //如果失败了，就同时取消拉取数据的任务
                message -> {
                    log.info(String.format("id: %s, group: %s 的fresh请求失败，具体原因为：%s", jobId, jobGroup, message));
                    log.info(String.format("id: %s, group: %s 使用的authorizeHeader: %s和refreshToken: %s", jobId, jobGroup, watch.getAuthorizeHeader(), watch.getRefreshToken()));
                    //在数据库中标记为过期
                    markAsExpire(userId, clientId);
                    //删除定时任务
                    removeTrigger(clientId, jobId, jobGroup);
                },
                resp -> {
                    //刷新数据库
                    long expireTime = System.currentTimeMillis() + resp.getExpiresIn() * 1000L;
                    //这里有一个细节是需要注意的，就是用户可能会临时把这个手表下线，如果它下线了，就不要再更新了
                    //下一次任务触发的时候，由于activate是0，会自动取消定时任务
                    jdbcTemplate.update("""
                            UPDATE `watchTable`\s
                            SET `accessToken`=?,`refreshToken`=?,`expireTime`=?\s
                            WHERE `userId`=? AND `clientId`=? AND `activate`=1;
                            """, resp.getAccessToken(), resp.getRefreshToken(), expireTime, userId, clientId);
                    log.info(String.format("id: %s, group: %s token刷新成功，下一次过期时间：%s", jobId, jobGroup, expireTime));
                }
        );
    }

    private void markAsExpire(long userId, String clientId) {
        jdbcTemplate.update("""
                UPDATE `watchTable` SET `expireTime`=0,`activate`=0 WHERE `userId`=? AND `clientId`=?;
                """, userId, clientId);
    }

    private void removeTrigger(String clientId, String jobId, String jobGroup) {
        TriggerKey triggerKey = new TriggerKey(
                JobConf.TRIGGER_FITBIT_EIGHT_HOUR_PREFIX + clientId,
                JobConf.TRIGGER_FITBIT_EIGHT_HOUR_GROUP);
        try {
            scheduler.unscheduleJob(triggerKey);
            log.info(String.format("id: %s, group: %s 取消定时任务成功", jobId, jobGroup));
        } catch (Exception e) {
            log.info(String.format("id: %s, group: %s 取消定时任务出错: %s", jobId, jobGroup, Arrays.toString(e.getStackTrace())));
        }
    }
}