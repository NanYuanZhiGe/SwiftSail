package com.nyzg.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class CommonApplication {
    private enum Status {
        ERROR, CLOSE, HEAR_BEAT;
    }

    private static class Message implements Delayed {
        long delay;
        Status status;

        public Message(long delay, @NonNull Status status) {
            this.delay = delay;
            this.status = status;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return delay-System.currentTimeMillis();
        }

        @Override
        public int compareTo(@NotNull Delayed other) {
            if (other == this) return 0;
            long diff = this.delay - ((Message) other).delay;
            return (diff > 0) ? 1 : (diff < 0) ? -1 : 0;
        }
    }

    static final private DelayQueue<Message> delayQueue = new DelayQueue<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println(LocalDate.ofEpochDay(20490));
    }
}
