package com.nyzg.dock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
public class DockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockApplication.class, args);
    }

}
