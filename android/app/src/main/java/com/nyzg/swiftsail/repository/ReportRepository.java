package com.nyzg.swiftsail.repository;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.dbobj.Report;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportRepository {
    public static ReportRepository INSTANCE = new ReportRepository();
    private final static Executor THREAD_POOL = new ThreadPoolExecutor(
            1, 3, 180, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10),
            new ThreadFactory() {
                final private AtomicInteger count = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("ReportRepository-thread-" + count.getAndIncrement());
                    return thread;
                }
            }, new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private ReportRepository() {
    }

    private final MutableLiveData<Integer> pagerCapacity = new MutableLiveData<>(1);
    private final AtomicBoolean isSync = new AtomicBoolean(false);

    public MutableLiveData<Integer> getPagerCapacity() {
        return pagerCapacity;
    }

    //这个对象存储了用户全部的数据，
    //但是不一定是完整的，它内部使用LRU缓存实现
    //用以节省用户的内存
    private final MutableLiveData<ReportLruCache> mutableReportList = new MutableLiveData<>();

    public MutableLiveData<ReportLruCache> getMutableReportList() {
        return mutableReportList;
    }


    public CompletableFuture<Report> getDataAsync(Long day) {
        return CompletableFuture.supplyAsync(() -> {
            return null;
        }, THREAD_POOL);
    }

    /**
     * 同步数据
     * 这个函数是幂等的，而且其实例只能存在一个
     */
    public void syncDataAsync() {
        //如果正在同步数据就不要动了
        if (!isSync.compareAndSet(false, true)) {
            return;
        }
        //开始异步同步数据
        CompletableFuture.supplyAsync(() -> {

            return null;
        }).thenAcceptAsync(action -> isSync.set(false));
    }

    /**
     * key是日期，value是当天的报表数据
     * 日期统一使用LocalDate.now().toEpochDay()
     * 也就是从1970/1/1以来的计数日期
     */
    public static class ReportLruCache extends LinkedHashMap<Long, Report> {
        //capacity就是用户当前的所有运动记录的数量.
        private final int CAPACITY;

        public ReportLruCache(int capacity) {
            //调整accessOrder为true
            super(64, 0.75f, true);
            this.CAPACITY = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Report> eldest) {
            return size() > CAPACITY;
        }

        /**
         * 获取这个日期的时候，尝试从内部的缓存中获取
         * 如果拿不到，就尝试从磁盘中读取
         */
        @Override
        public Report get(Object key) {
            return super.get(key);
        }

        @Override
        public Report put(Long key, Report value) {
            return super.put(key, value);
        }

        public int getSize() {
            return CAPACITY;
        }
    }
}