package com.nyzg.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class CommonApplication {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(LocalDate.now().toEpochDay());
    }
}
