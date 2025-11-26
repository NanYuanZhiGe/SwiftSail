package com.nyzg.user.conf;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TestConf {
    @Value("${test.string}")
    String s;

    @Value("${database.test}")
    String databaseTestString;


    @PostConstruct
    public void sayHelloWorld() {
        log.info(s);
        log.info(databaseTestString);
    }
}
