package com.nyzg.common;

import java.util.concurrent.CompletableFuture;

public class CommonApplication {
    public static void main(String[] args){
        CompletableFuture.supplyAsync(()->{
            System.out.println(Thread.currentThread().getName());
            return null;
        }).thenAccept(action->{
            System.out.println(Thread.currentThread().getName());
        });
    }
}
