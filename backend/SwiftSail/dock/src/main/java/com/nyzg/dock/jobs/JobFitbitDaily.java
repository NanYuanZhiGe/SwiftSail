package com.nyzg.dock.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyzg.common.DateUtils;
import com.nyzg.dock.dbobj.Record;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.RecordTableMapper;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.netobj.*;
import com.nyzg.dock.obj.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;


@Component
@Slf4j
public class JobFitbitDaily implements Job {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static ProxySelector PROXY_SELECTOR = ProxySelector.of(new InetSocketAddress("127.0.0.1", 7890));
    @Resource
    WatchTableMapper watchTableMapper;
    @Resource
    RecordTableMapper recordTableMapper;

    private final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Override
    public void execute(JobExecutionContext jobExecutionContext){
        String jobId = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
        String clientId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("clientId");
        if (clientId == null) {
            log.info(String.format("id: %s, group: %s 的clientId为空，无法进行后续日常拉取数据操作", jobId, jobGroup));
            return;
        }
        //获取accessToken
        Watch watch = watchTableMapper.getAccessToken(clientId);
        if (watch == null || watch.getAccessToken() == null || watch.getWatchUserId() == null) {
            log.info(String.format("id: %s, group: %s, 返回的watch数据不完整，没有办法拉取数据", jobId, jobGroup));
            return;
        }
        //检查expireTime
        long expireTime = watch.getExpireTime();
        long leftTime = System.currentTimeMillis() - expireTime;
        //这里的同步数据是有讲究的，只能同步前一天的，因为我们是中午12:00进行一次同步，这个时候用户的数据还是不完整的
        LocalDate lastDate = LocalDate.ofEpochDay(LocalDate.now().toEpochDay() - 1);
        String dateBefore = lastDate.format(DATE_FORMATTER);
        long epochDay = lastDate.toEpochDay();
        long epochWeek = DateUtils.getEpochWeek(epochDay);
        long epochMonth = DateUtils.getEpochMonth(lastDate);
        long epochYear = DateUtils.getEpochYear(lastDate);

        //sleep daily
        leftTime = getDataReturnLeftTime(
                leftTime, watch.getAccessToken(),
                String.format("https://api.fitbit.com/1.2/user/%s/sleep/date/%s.json", watch.getWatchUserId(), dateBefore),
                msg -> log.info(String.format("id: %s, group: %s, 查询sleep出错：%s", jobId, jobGroup, msg)),
                obj -> {
                    Record recordSleep = new Record();
                    long exposeValue = obj.getSummary().getTotalMinutesAsleep() * 60L * 1000L;
                    recordSleep.setExposeValue(exposeValue);
                    MySleep mySleep = new MySleep();
                    mySleep.setTotalSleepTime(obj.getSummary().getTotalMinutesAsleep() * 60 * 1000L);
                    mySleep.setTotalTimeInBed(obj.getSummary().getTotalTimeInBed() * 60 * 1000L);
                    mySleep.setSleepLines(new ArrayList<>(obj.getSleep().size()));
                    for (FitbitSleepResp.SleepRecord record : obj.getSleep()) {
                        if (record.isMainSleep()) {
                            String startTime = record.getStartTime();
                            String endTime = record.getEndTime();
                            LocalDateTime startDateTime = LocalDateTime.parse(startTime);
                            LocalDateTime endDateTime = LocalDateTime.parse(endTime);
                            mySleep.setMainStartEpoch(startDateTime.atZone(ZONE_ID).toEpochSecond() * 1000L);
                            mySleep.setMainEndEpoch(endDateTime.atZone(ZONE_ID).toEpochSecond() * 1000L);
                            mySleep.setMainDeepTime(record.getLevels().getSummary().getDeep().getMinutes() * 60 * 1000L);
                            mySleep.setMainLightTime(record.getLevels().getSummary().getLight().getMinutes() * 60 * 1000L);
                            mySleep.setMainRemTime(record.getLevels().getSummary().getRem().getMinutes() * 60 * 1000L);
                            mySleep.setMainWakeTime(record.getLevels().getSummary().getWake().getMinutes() * 60 * 1000L);
                        }
                        MySleep.SleepLine sleepLine = new MySleep.SleepLine();
                        sleepLine.setStartEpoch(LocalDateTime.parse(record.getStartTime()).atZone(ZONE_ID).toEpochSecond() * 1000L);
                        sleepLine.setEndEpoch(LocalDateTime.parse(record.getEndTime()).atZone(ZONE_ID).toEpochSecond() * 1000L);
                        sleepLine.setMainSleep(record.isMainSleep());
                        sleepLine.setDuration(record.getMinutesAsleep() * 60 * 1000L);
                        sleepLine.setTimeInBed(record.getTimeInBed() * 60 * 1000L);
                        mySleep.getSleepLines().add(sleepLine);
                    }
                    try {
                        recordSleep.setDetailValue(OBJECT_MAPPER.writeValueAsString(mySleep));
                    } catch (JsonProcessingException e) {
                        log.info("sleep 序列化错误");
                        return;
                    }
                    insertRecord(recordSleep, watch.getUserId(), RecordType.SLEEP, epochDay, epochWeek, epochMonth, epochYear);
                }, FitbitSleepResp.class);
        //剩下的数据可以一并查询 daily
        getDataReturnLeftTime(
                leftTime, watch.getAccessToken(),
                String.format("https://api.fitbit.com/1/user/%s/activities/date/%s.json", watch.getWatchUserId(), dateBefore),
                msg -> log.info(String.format("id: %s, group: %s, 查询heartRate出错：%s", jobId, jobGroup, msg)),
                obj -> {
                    //heart
                    Record record = new Record();
                    MyHeartRate myHeartRate = new MyHeartRate();
                    myHeartRate.setRest(obj.getSummary().getRestingHeartRate());
                    int count = 0;
                    for (FitbitActivitySummary.HeartRateZones zone : obj.getSummary().getHeartRateZones()) {
                        if (zone.getMinutes() > 0) {//用户的心率在这个区间
                            myHeartRate.getCaloriesOut()[count] = (float) zone.getCaloriesOut();
                            if (myHeartRate.getLow() == 0L) {//为空的话就是这个值
                                myHeartRate.setLow(zone.getMin());
                            }
                            //最大心率区间每次都会增长
                            myHeartRate.setHigh(zone.getMax());
                        }
                        ++count;
                    }
                    record.setExposeValue(
                            1000000000L + myHeartRate.getLow() + myHeartRate.getHigh() * 1000L + myHeartRate.getRest() * 1000000L
                    );
                    try {
                        record.setDetailValue(OBJECT_MAPPER.writeValueAsString(myHeartRate));
                        insertRecord(record, watch.getUserId(), RecordType.HEART, epochDay, epochWeek, epochMonth, epochYear);
                    } catch (JsonProcessingException e) {
                        log.info(e.getCause().toString());
                    }
                    //step
                    record = new Record();
                    MyStep myStep = new MyStep();
                    myStep.setSteps(obj.getSummary().getSteps());
                    myStep.setFloors(obj.getSummary().getFloors());
                    myStep.setSedentaryMinutes(obj.getSummary().getSedentaryMinutes());
                    record.setExposeValue(myStep.getSteps());
                    try {
                        record.setDetailValue(OBJECT_MAPPER.writeValueAsString(myStep));
                        insertRecord(record, watch.getUserId(), RecordType.HEART, epochDay, epochWeek, epochMonth, epochYear);
                    } catch (JsonProcessingException e) {
                        log.info(e.getCause().toString());
                    }
                    //distance
                    record = new Record();
                    MyDistance myDistance = getMyDistance(obj);
                    record.setExposeValue((long) (myDistance.getDistance() * 1000));
                    try {
                        record.setDetailValue(OBJECT_MAPPER.writeValueAsString(myDistance));
                    } catch (JsonProcessingException e) {
                        log.info(e.getCause().getMessage());
                        insertRecord(record, watch.getUserId(), RecordType.HEART, epochDay, epochWeek, epochMonth, epochYear);
                    }
                    //caloric
                    record = new Record();
                    MyConsumption myConsumption = getMyConsumption(obj);
                    record.setExposeValue(myConsumption.getActivityCalories());
                    try {
                        record.setDetailValue(OBJECT_MAPPER.writeValueAsString(myConsumption));
                    } catch (JsonProcessingException e) {
                        log.info(e.getCause().getMessage());
                        insertRecord(record, watch.getUserId(), RecordType.HEART, epochDay, epochWeek, epochMonth, epochYear);
                    }
                }, FitbitActivitySummary.class);
    }

    @NotNull
    private static MyDistance getMyDistance(FitbitActivitySummary obj) {
        MyDistance myDistance = new MyDistance();
        for (FitbitActivitySummary.Distances distance : obj.getSummary().getDistances()) {
            switch (distance.getActivity()) {
                case "total" -> myDistance.setDistance(distance.getDistance());
                case "Walk" -> myDistance.setWalkDistance(distance.getDistance());
                case "lightlyActive" -> myDistance.setLightlyActiveDistance(distance.getDistance());
                case "moderatelyActive" -> myDistance.setModerateActiveDistance(distance.getDistance());
                case "veryActive" -> myDistance.setVeryActiveDistance(distance.getDistance());
            }
        }
        return myDistance;
    }

    @NotNull
    private static MyConsumption getMyConsumption(FitbitActivitySummary obj) {
        MyConsumption myConsumption = new MyConsumption();
        myConsumption.setActivityCalories(obj.getSummary().getActivityCalories());
        myConsumption.setCaloriesOut(obj.getSummary().getCaloriesOut());
        myConsumption.setLightlyActiveMinutes(obj.getSummary().getLightlyActiveMinutes());
        myConsumption.setFairlyActiveMinutes(obj.getSummary().getFairlyActiveMinutes());
        myConsumption.setVeryActiveMinutes(obj.getSummary().getVeryActiveMinutes());
        return myConsumption;
    }

    private void insertRecord(Record record, long userId, String type, long epochDay, long epochWeek, long epochMonth, long epochYear) {
        record.setUserId(userId);
        record.setRecordId(UUID.randomUUID().toString());
        record.setType(type);
        record.setEpochDay(epochDay);
        record.setEpochWeek(epochWeek);
        record.setEpochMonth(epochMonth);
        record.setEpochYear(epochYear);
        recordTableMapper.insertRecordIntoTable(record);
    }

    private <T> long getDataReturnLeftTime(long leftTime, String accessToken, String url, Consumer<String> onFail, Consumer<T> onSuccess, Class<T> clz) {
        if (leftTime <= 0) {
            return -1;
        }
        long startTime = System.currentTimeMillis();
        HttpClient httpClient = HttpClient.newBuilder().proxy(PROXY_SELECTOR).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).header(HttpHeaders.ACCEPT, "application/json").GET().build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            onFail.accept("网络错误");
            return leftTime - (System.currentTimeMillis() - startTime);
        } catch (InterruptedException e) {
            onFail.accept("请求过程被异常终止（interrupt）");
            return leftTime - (System.currentTimeMillis() - startTime);
        }
        if (response.statusCode() == 400) {
            onFail.accept("语法错误或者是请求无法解析");
            return leftTime - (System.currentTimeMillis() - startTime);
        } else if (response.statusCode() == 401) {
            onFail.accept("需要用户授权或者是授权码过期");
            return leftTime - (System.currentTimeMillis() - startTime);
        } else if (response.statusCode() != 200) {
            onFail.accept("其他错误: " + request.toString());
            return leftTime - (System.currentTimeMillis() - startTime);
        }
        T t;
        try {
            t = OBJECT_MAPPER.readValue(response.body(), clz);
        } catch (Exception e) {
            onFail.accept("响应反序列化错误");
            return leftTime - (System.currentTimeMillis() - startTime);
        }
        onSuccess.accept(t);
        return leftTime - (System.currentTimeMillis() - startTime);
    }

    public static String accessTokenTest = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyM1RSNVgiLCJzdWIiOiJDUjJWUEIiLCJpc3MiOiJGaXRiaXQiLCJ0eXAiOiJhY2Nlc3NfdG9rZW4iLCJzY29wZXMiOiJyc29jIHJzZXQgcm94eSBycHJvIHJudXQgcnNsZSByYWN0IHJyZXMgcmxvYyByd2VpIHJociBydGVtIiwiZXhwIjoxNzY5MjA2NzgzLCJpYXQiOjE3NjkxNzc5ODN9.S36VlcT9JndPpJidQlAi931AX8DR-1NqhZdK-Wl_fMo";

    @Test
    public void testSleep() {
        long leftTime = System.currentTimeMillis() + 60 * 60 * 1000;
        String accessToken = accessTokenTest;
        String userId = "CR2VPB";
        String date = LocalDate.ofEpochDay(LocalDate.now().toEpochDay() - 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        getDataReturnLeftTime(leftTime, accessToken, String.format("https://api.fitbit.com/1.2/user/%s/sleep/date/%s.json", userId, date), log::info, obj -> log.info(obj.toString()), FitbitSleepResp.class);
    }

    @Test
    public void testActivity() {
        long leftTime = System.currentTimeMillis() + 60 * 60 * 1000;
        String accessToken = accessTokenTest;
        String userId = "CR2VPB";
        String date = LocalDate.ofEpochDay(LocalDate.now().toEpochDay() - 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        getDataReturnLeftTime(leftTime, accessToken,
                String.format("https://api.fitbit.com/1/user/%s/activities/date/%s.json", userId, date), log::info, obj -> log.info(obj.toString()), FitbitActivitySummary.class);
    }
}