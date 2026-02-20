package com.nyzg.dock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyzg.common.DateUtils;
import com.nyzg.common.Pair;
import com.nyzg.dock.dbobj.Record;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.netobj.*;
import com.nyzg.dock.obj.*;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.nyzg.dock.conf.TimeThings.ZONE_ID;

@Service
@Slf4j
public class FitbitWebApiService {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static ProxySelector PROXY_SELECTOR = ProxySelector.of(new InetSocketAddress("127.0.0.1", 7890));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    WatchTableMapper watchTableMapper;
    @Resource
    JdbcTemplate jdbcTemplate;

    public enum QueryStatus {
        SUCCEED, FAIL, NO_DATA
    }

    /**
     * 去fitbit哪里进行网络请求获取数据
     * 返回空一般就都是网络错误了
     */
    @NonNull
    public Pair<QueryStatus, DayRecord> getDayRecordNullAtFail(
            long userId,
            @NonNull String accessToken,
            @NonNull String watchUserId,
            long epochDay) {
        LocalDate date = LocalDate.ofEpochDay(epochDay);
        //先获取activity的数据，如果为空，则可能说明用户没有上传
        Optional<List<Record>> activityRecord = Optional.empty();
        for (int i = 0; i < 3; ++i) {
            activityRecord = getActivityRecordNullAtFail(
                    accessToken,
                    watchUserId,
                    date,
                    userId,
                    log::info
            );
            if (activityRecord.isPresent()) {
                break;
            }
        }
        //查不到数据或者是用户没有上传数据
        if (activityRecord.isEmpty()) {
            return new Pair<>(QueryStatus.FAIL, null);
        } else if (activityRecord.get().isEmpty()) {
            return new Pair<>(QueryStatus.NO_DATA, null);
        }
        Optional<Record> sleepRecord = Optional.empty();
        for (int i = 0; i < 3; ++i) {
            sleepRecord = getSleepRecordNullAtFail(
                    accessToken,
                    watchUserId,
                    date,
                    userId,
                    log::info
            );
            if (sleepRecord.isPresent()) {
                break;
            }
        }
        if (sleepRecord.isEmpty()) {
            return new Pair<>(QueryStatus.FAIL, null);
        }
        activityRecord.get().add(sleepRecord.get());
        return new Pair<>(QueryStatus.SUCCEED, new DayRecord(activityRecord.get()));
    }

    /**
     * @return 返回空表示错误
     */
    public Optional<List<DayRecord>> getRangeRecordAndAsyncToDatabase(
            long userId,
            @NonNull String clientId,
            long fromEpoch, long endEpoch) {
        List<Watch> watchList = watchTableMapper.selectWatchByClientId(userId, clientId);
        if (watchList.isEmpty()) {
            return Optional.empty();
        }
        Watch watch = watchList.get(0);
        List<Record> resultList = new ArrayList<>((int) (endEpoch - fromEpoch + 1) * 5);
        //逐一获取sleep的数据
        for (long i = fromEpoch; i <= endEpoch; ++i) {
            for (int j = 0; ; ++j) {
                Optional<Record> record = getSleepRecordNullAtFail(
                        watch.getAccessToken(),
                        watch.getWatchUserId(),
                        LocalDate.ofEpochDay(i),
                        watch.getUserId(),
                        log::info
                );
                if (record.isEmpty() && j == 2) {
                    log.info("睡眠数据请求失败");
                    return Optional.empty();
                } else if (record.isPresent()) {
                    resultList.add(record.get());
                    break;
                }
            }
        }
        //然后逐一获取activity的summary
        for (long i = fromEpoch; i <= endEpoch; ++i) {
            for (int j = 0; ; ++j) {
                Optional<List<Record>> recordList = getActivityRecordNullAtFail(
                        watch.getAccessToken(),
                        watch.getWatchUserId(),
                        LocalDate.ofEpochDay(i),
                        watch.getUserId(),
                        log::info
                );
                if (recordList.isEmpty() && j == 2) {
                    log.info("请求activity失败");
                    return Optional.empty();
                } else if (recordList.isPresent()) {
                    resultList.addAll(recordList.get());
                    break;
                }
            }
        }
        //异步插入数据库
        CompletableFuture.supplyAsync(() -> {
            String placeHolders = resultList.stream().map(record -> "(?,?,?,?,?,?,?,?,?,?)").collect(Collectors.joining(","));
            String sql = """
                    INSERT IGNORE INTO `recordTable` (id, userId, recordId, type, exposeValue, detailValue, epochDay, epochWeek, epochMonth, epochYear) VALUES \s
                    """ + placeHolders;
            Object[] flatArgs = resultList.stream()
                    .map(record -> new Object[]{
                            null, record.userId, record.recordId, record.type,
                            record.exposeValue, record.detailValue, record.epochDay, record.epochWeek,
                            record.epochMonth, record.epochYear
                    })
                    .flatMap(Arrays::stream)
                    .toArray();
            jdbcTemplate.update(sql, flatArgs);
            return null;
        });
        //整理resultList，返回DayRecord
        List<DayRecord> dayRecords = new ArrayList<>(resultList.stream()
                .collect(Collectors.groupingBy(
                        Record::getEpochDay,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    DayRecord dr = new DayRecord();
                                    dr.getRecordList().addAll(list);
                                    return dr;
                                }
                        )
                ))
                .values());
        return Optional.of(dayRecords);
    }

    public Optional<Record> getSleepRecordNullAtFail(
            @NonNull String accessToken,
            @NonNull String watchUserId,
            @NonNull LocalDate date,
            long userId, Consumer<String> onError) {
        AtomicReference<Record> result = new AtomicReference<>();
        getWatchDataSync(
                accessToken,
                String.format("https://api.fitbit.com/1.2/user/%s/sleep/date/%s.json", watchUserId, date.format(DATE_FORMATTER)),
                onError,
                resp -> convertFitbitSleepResp(resp, date, userId, result::set),
                FitbitSleepResp.class);
        if (result.get() == null) {
            return Optional.empty();
        }
        return Optional.of(result.get());
    }

    private void convertFitbitSleepResp(
            @Nullable FitbitSleepResp resp,
            @NonNull LocalDate date,
            long userId, Consumer<Record> afterConvert) {
        if (resp == null || resp.getSleep() == null) {
            return;
        }
        Record recordSleep = new Record();
        long exposeValue = resp.getSummary().getTotalMinutesAsleep() * 60L * 1000L;
        recordSleep.setExposeValue(exposeValue);
        MySleep mySleep = new MySleep();
        mySleep.setTotalSleepTime(resp.getSummary().getTotalMinutesAsleep() * 60 * 1000L);
        mySleep.setTotalTimeInBed(resp.getSummary().getTotalTimeInBed() * 60 * 1000L);
        mySleep.setSleepLines(new ArrayList<>(resp.getSleep().size()));
        for (FitbitSleepResp.SleepRecord record : resp.getSleep()) {
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
        setRecord(recordSleep, userId, RecordType.SLEEP, date);
        afterConvert.accept(recordSleep);
    }

    public Optional<List<Record>> getActivityRecordNullAtFail(
            @NonNull String accessToken,
            @NonNull String watchUserId,
            @NonNull LocalDate date,
            long userId, Consumer<String> onError) {
        AtomicReference<List<Record>> result = new AtomicReference<>();
        getWatchDataSync(
                accessToken,
                String.format("https://api.fitbit.com/1/user/%s/activities/date/%s.json", watchUserId, date.format(DATE_FORMATTER)),
                onError,
                obj -> {
                    if (obj == null || obj.getSummary() == null) {
                        return;
                    }
                    List<Record> appendList = new ArrayList<>(4);//解析查询四种指标
                    //heart
                    Record record = new Record();
                    MyHeartRate myHeartRate = new MyHeartRate();
                    myHeartRate.setRest(obj.getSummary().getRestingHeartRate());
                    int count = 0;
                    //连心率数据都没有，不可能，用户一定是没有上传
                    if (obj.getSummary().getHeartRateZones() == null || obj.getSummary().getHeartRateZones().isEmpty()) {
                        result.set(appendList);
                        return;
                    }
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
                    parseRecordSucceedAppend(appendList, record, myHeartRate, userId, RecordType.HEART, date);
                    //step
                    record = new Record();
                    MyStep myStep = new MyStep();
                    myStep.setSteps(obj.getSummary().getSteps());
                    myStep.setFloors(obj.getSummary().getFloors());
                    myStep.setSedentaryMinutes(obj.getSummary().getSedentaryMinutes());
                    record.setExposeValue(myStep.getSteps());
                    parseRecordSucceedAppend(appendList, record, myStep, userId, RecordType.STEP, date);
                    //distance
                    record = new Record();
                    MyDistance myDistance = getMyDistance(obj);
                    record.setExposeValue((long) (myDistance.getDistance() * 1000));
                    parseRecordSucceedAppend(appendList, record, myDistance, userId, RecordType.DISTANCE, date);
                    //caloric
                    record = new Record();
                    MyConsumption myConsumption = getMyConsumption(obj);
                    record.setExposeValue(myConsumption.getCaloriesOut());
                    parseRecordSucceedAppend(appendList, record, myConsumption, userId, RecordType.CALORIC, date);
                    result.set(appendList);
                }, FitbitActivitySummary.class);
        if (result.get() == null) {
            return Optional.empty();
        }
        return Optional.of(result.get());
    }

    private void parseRecordSucceedAppend(
            @NonNull List<Record> resultList,
            @NonNull Record record,
            @NonNull Object jsonObj,
            long userId,
            @NonNull String type,
            @NonNull LocalDate date) {
        try {
            record.setDetailValue(OBJECT_MAPPER.writeValueAsString(jsonObj));
            setRecord(record, userId, type, date);
            resultList.add(record);
        } catch (JsonProcessingException e) {
            log.info(e.getCause().toString());
        }
    }

    @NotNull
    private MyDistance getMyDistance(@NonNull FitbitActivitySummary obj) {
        MyDistance myDistance = new MyDistance();
        if (obj.getSummary() == null) {
            return myDistance;
        }
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
    private MyConsumption getMyConsumption(@NonNull FitbitActivitySummary obj) {
        MyConsumption myConsumption = new MyConsumption();
        if (obj.getSummary() == null) {
            return myConsumption;
        }
        myConsumption.setActivityCalories(obj.getSummary().getActivityCalories());
        myConsumption.setCaloriesOut(obj.getSummary().getCaloriesOut());
        myConsumption.setLightlyActiveMinutes(obj.getSummary().getLightlyActiveMinutes());
        myConsumption.setFairlyActiveMinutes(obj.getSummary().getFairlyActiveMinutes());
        myConsumption.setVeryActiveMinutes(obj.getSummary().getVeryActiveMinutes());
        return myConsumption;
    }

    private void setRecord(
            @NonNull Record record,
            long userId,
            @NonNull String type,
            @NonNull LocalDate date) {
        record.setUserId(userId);
        record.setRecordId(UUID.randomUUID().toString());
        record.setType(type);
        long epochDay = date.toEpochDay();
        long epochWeek = DateUtils.getEpochWeek(epochDay);
        long epochMonth = DateUtils.getEpochMonth(date);
        long epochYear = DateUtils.getEpochYear(date);
        record.setEpochDay(epochDay);
        record.setEpochWeek(epochWeek);
        record.setEpochMonth(epochMonth);
        record.setEpochYear(epochYear);
    }

    private <T> void getWatchDataSync(String accessToken, String url, Consumer<String> onFail, Consumer<T> onSuccess, Class<T> clz) {
        HttpClient httpClient = HttpClient.newBuilder().proxy(PROXY_SELECTOR).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/json")
                .GET().build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            onFail.accept("网络错误");
            return;
        } catch (InterruptedException e) {
            onFail.accept("请求过程被异常终止（interrupt）");
            return;
        }
        if (response.statusCode() == 400) {
            onFail.accept("语法错误或者是请求无法解析");
            return;
        } else if (response.statusCode() == 401) {
            onFail.accept("需要用户授权或者是授权码过期");
            return;
        } else if (response.statusCode() != 200) {
            onFail.accept("其他错误: " + request.toString());
            return;
        }
        T t;
        try {
            t = OBJECT_MAPPER.readValue(response.body(), clz);
        } catch (Exception e) {
            onFail.accept("响应反序列化错误");
            return;
        }
        onSuccess.accept(t);
    }

    public Optional<FitbitOath2Token> getTokenSync(
            @NonNull String clientId,
            @NonNull String authorizeHeader,
            @NonNull String code,
            @NonNull String codeVerifier) {
        HttpClient httpClient = HttpClient.newBuilder().proxy(PROXY_SELECTOR).build();
        try {
            HttpResponse<String> response = httpClient.send(
                    getGetTokenRequest(clientId, authorizeHeader, code, codeVerifier),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            return Optional.of(OBJECT_MAPPER.readValue(response.body(), FitbitOath2Token.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private HttpRequest getGetTokenRequest(
            @NonNull String clientId,
            @NonNull String authorizeHeader,
            @NonNull String code,
            @NonNull String codeVerifier) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://api.fitbit.com/oauth2/token"))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + authorizeHeader)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(String.format("client_id=%s&code=%s&code_verifier=%s&grant_type=authorization_code", clientId, code, codeVerifier)))
                .build();
    }

    public void getRefreshToken(
            String authorizeHeader,
            String refreshToken,
            Consumer<String> onRequestFail,
            Consumer<FitbitRefreshTokenResp> onSuccess) {
        HttpClient httpClient = HttpClient.newBuilder().proxy(PROXY_SELECTOR).build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.fitbit.com/oauth2/token"))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + authorizeHeader)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.format("grant_type=refresh_token&refresh_token=%s", refreshToken)
                ))
                .build();
        HttpResponse<String> resp;
        try {
            resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            onRequestFail.accept("无法和服务器建立连接，或者IO过程被打断");
            return;
        }
        if (resp.statusCode() == 400) {
            onRequestFail.accept("请求语法错误");
            return;
        } else if (resp.statusCode() == 401) {
            onRequestFail.accept("需要用户授权，或者是token过期，详情: " + resp.body());
            return;
        } else if (resp.statusCode() != 200) {
            onRequestFail.accept("请求失败，具体信息: " + resp);
            return;
        }
        FitbitRefreshTokenResp tokenResp;
        try {
            tokenResp = OBJECT_MAPPER.readValue(resp.body(), FitbitRefreshTokenResp.class);
        } catch (Exception e) {
            onRequestFail.accept("解析服务端返回的数据出错");
            return;
        }
        onSuccess.accept(tokenResp);
    }
}