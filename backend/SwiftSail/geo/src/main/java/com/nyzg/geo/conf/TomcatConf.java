package com.nyzg.geo.conf;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
public class TomcatConf {
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            // 创建 OfVirtual，指定虚拟线程名称的前缀，以及线程编号起始值
            Thread.Builder.OfVirtual ofVirtual = Thread.ofVirtual().name("virtual-thread#", 1);
            // 获取虚拟线程池工厂
            ThreadFactory factory = ofVirtual.factory();
            // 通过该工厂，创建 ExecutorService
            protocolHandler.setExecutor(Executors.newThreadPerTaskExecutor(factory));
        };
    }
}