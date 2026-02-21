package com.nyzg.swiftsail.repository;

import android.annotation.SuppressLint;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.HealthIndicator;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.report.DayRecord;
import com.nyzg.swiftsail.netobj.report.GetDataDayReq;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.ParsedReport;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class ReportRepository {
    volatile private static ReportRepository INSTANCE;

    public static ReportRepository getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (ReportRepository.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new ReportRepository();
        }
        return INSTANCE;
    }

    private final static Executor THREAD_POOL = new ThreadPoolExecutor(
            2, 2,
            5 * 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadFactory() {
                final private AtomicInteger count = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("ReportRepository-thread-" + count.getAndIncrement());
                    return thread;
                }
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private ReportRepository() {
    }

    //ReportMain0Fragment用于展示信息的东西
    public final MutableLiveData<ParsedReport> mainReport = new MutableLiveData<>();

    //最多存储64天的ReportMain0Fragment的数据
    private final LruParsedReportCache parsedReportCache = new LruParsedReportCache(32);
    private final ReadWriteLock parsedReportCacheLock = new ReentrantReadWriteLock();

    /**
     * 有缓存优化，
     * 通过线程池的设计，保证用户在快速的切换的时候不会过度堆积任务，而且能够保证响应速度
     */
    public void getReportMainAsync(Long day, @Nullable Consumer<String> afterDone) {
        CompletableFuture.supplyAsync(() -> {
            ParsedReport parsedReport;
            parsedReportCacheLock.readLock().lock();
            try {
                parsedReport = parsedReportCache.get(day);
            } finally {
                parsedReportCacheLock.readLock().unlock();
            }
            if (parsedReport != null) {//如果有数据
                mainReport.postValue(parsedReport);
                return "同步数据成功";
            }
            //没有数据就去数据库中查询
            //数据库中没有就去网络查询拿到数据
            //然后加写锁写入数据
            /*
            这里不用双重检查的原因如下：
            1. 当用户连续选择同一天的时候DateSelector不会重复提交任务
            2. 用户在大部分情况下是提交不同天的任务的，如果使用写锁+双重检查，命中率极低，
            同时还降低了并发度
            3. 对于极限情况，用户切到其他天，又快速切回来，可能在极端情况下会导致一点点性能问题，
            但是无所谓，因为每次查询的数据是一致的，所以不怕写入脏数据，再者，这样子缓存很容易命中
             */
            Pair<ParsedReport, String> pair = getExposeValueAndParseNullAtFail(day);
            if (pair.getA() == null) {
                return pair.getB();
            }
            parsedReportCacheLock.writeLock().lock();
            try {
                parsedReportCache.put(day, pair.getA());
                mainReport.postValue(pair.getA());
            } finally {
                parsedReportCacheLock.writeLock().unlock();
            }
            return "同步数据成功";
        }, THREAD_POOL).thenAcceptAsync(msg -> {
            if (afterDone == null) {
                return;
            }
            afterDone.accept(msg);
        }, ContextCompat.getMainExecutor(GlobalApplication.getAppContext()));
    }


    private final Pair<ParsedReport, String> NULL_USER = new Pair<>(null, "当前用户不存在(null)，无法查询数据");
    private final Pair<ParsedReport, String> LOCAL_USER_NOT_SUPPORT = new Pair<>(null, "本地用户不支持此功能");

    /**
     * 从数据库中查询exposeValue，然后解析出来
     * 如果没有就进行网络查询拿到数据
     * 返回空表示查询失败
     *
     * @return 第一个数据，第二个是成功/失败信息
     */
    @SuppressLint("DefaultLocale")
    @NonNull
    private Pair<@Nullable ParsedReport, @NonNull String> getExposeValueAndParseNullAtFail(Long day) {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        User user = LoginRepository.getInstance().currentUser.getValue();
        if (user == null) {
            return NULL_USER;
        } else if (user.id == GlobalInstance.LOCAL_USER.id) {
            return LOCAL_USER_NOT_SUPPORT;
        }
        long userId = user.id;
        //检查数据库中是否有数据
        int count = sqLiteDB.recordTable().countRecordWithDay(userId, day);
        if (count > 0) {//如果数据库中有数据
            return new Pair<>(getParsedReportBaseOnDb(userId, day), "");
        }
        //进行网络操作
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> syncResult = new AtomicReference<>("");
        long today = LocalDate.now().toEpochDay();
        NetWorkHandler.handleNetRespAfterLogin(
                null,
                NetWorkBuilder.doChunkRequest(NetWorkBuilder.buildJsonRequestJwt(
                        ServerURL.URL_GET_DAY_DATA, ServerURL.POST, new GetDataDayReq(day)
                )),
                syncResult::set,
                dayRecord -> {//写入数据库
                    try {
                        if (today > day) {//今天的数据是不完整的，不能够写入数据库中
                            sqLiteDB.recordTable().insertRecordList(dayRecord.recordList);
                        }
                        syncResult.set("同步数据成功");
                    } catch (Exception e) {
                        syncResult.set("同步数据成功，但是写入数据库失败：" + e.getCause());
                    }
                    success.set(true);
                },
                DayRecord.class
        );
        if (success.get()) {
            return new Pair<>(getParsedReportBaseOnDb(userId, day), syncResult.get());
        }
        return new Pair<>(null, syncResult.get());
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    private ParsedReport getParsedReportBaseOnDb(long userId, long day) {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        try {
            ParsedReport result = new ParsedReport();
            Long caloric = sqLiteDB.recordTable().getExposeValue(userId, RecordType.CALORIC, day);
            if (caloric != null) {
                result.caloric = caloric + "卡";
                result.caloricProgress = HealthIndicator.caloricBalanceIndicator(-1, caloric);
            }
            Long distance = sqLiteDB.recordTable().getExposeValue(userId, RecordType.DISTANCE, day);
            if (distance != null) {
                double kilometers = distance / 1000.0;
                result.stepDistance = String.format("%.2f公里", kilometers);
                result.distanceProgress = HealthIndicator.distanceIndicator(kilometers);
            }
            Long heartRate = sqLiteDB.recordTable().getExposeValue(userId, RecordType.HEART, day);
            if (heartRate != null && heartRate >= 1000000000L) {
                //第三位为低心率，中三位为高心率，高三位为静息心率
                int lowRate = (int) (heartRate % 1000);
                int high = (int) (heartRate / 1000 % 1000);
                int rest = (int) (heartRate / 1000000 % 1000);
                result.heartRange = String.format("%d-%d bpm", lowRate, high);
                result.restHeartBeat = String.format("%d bpm", rest);
                result.heartProgress = HealthIndicator.restHeartRateIndicator(rest);
            }
            Long nutrition = sqLiteDB.recordTable().getExposeValue(userId, RecordType.FOOD, day);
            if (nutrition != null) {
                result.nutrition = nutrition + "卡";
            }
            Long sleep = sqLiteDB.recordTable().getExposeValue(userId, RecordType.SLEEP, day);
            if (sleep != null) {//sleep是毫秒
                float time = sleep / 1000f / 3600f;
                int hour = (int) time;
                int minute = (int) ((time - hour) * 60);
                float score = HealthIndicator.sleepIndicator(hour);
                result.sleepTime = String.format("%d小时%d分钟", hour, minute);
                result.sleepScore = (int) (score * 100) + "";
                result.sleepProgress = score;
            }
            Long step = sqLiteDB.recordTable().getExposeValue(userId, RecordType.STEP, day);
            if (step != null) {
                result.stepCount = step + "";
                result.stepProgress = HealthIndicator.stepIndicator(step);
            }
            return result;
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * key 是epoch day
     */
    private static class LruParsedReportCache extends LinkedHashMap<Long, ParsedReport> {
        private final int CAPACITY;

        public LruParsedReportCache(int CAPACITY) {
            super(16, 0.75f, true);
            this.CAPACITY = CAPACITY;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ParsedReport> eldest) {
            return size() > CAPACITY;
        }
    }
}