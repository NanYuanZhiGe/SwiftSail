package com.nyzg.swiftsail.bean;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    public static final ExecutorService REPORT_DETAIL_THREAD_POOL = new ThreadPoolExecutor(
            2, 2,
            5 * 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadFactory() {
                final private AtomicInteger count = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("ReportDetailThread-" + count.getAndAdd(1));
                    return thread;
                }
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );
}
