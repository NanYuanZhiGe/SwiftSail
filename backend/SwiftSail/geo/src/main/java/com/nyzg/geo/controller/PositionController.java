package com.nyzg.geo.controller;

import com.nyzg.geo.conf.KafkaTopicConf;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Slf4j
public class PositionController {

    @GetMapping(path = "/test/connection")
    public String testConnection(){
        return Thread.currentThread().getName();
    }

    @Resource
    private KafkaTemplate<String,String> kafkaTemplate;

    private AtomicInteger count=new AtomicInteger(0);

    @GetMapping(path = "/send/{anyString}")
    public String sendAnyStringToKafka(@PathVariable("anyString") String anyString){
        try{
            SendResult<String, String> stringStringSendResult = kafkaTemplate.send(KafkaTopicConf.TOPIC_IMAGE_PROCESS, count.getAndIncrement()+"",anyString + " " + Thread.currentThread().getName()).get();
            return stringStringSendResult.toString()+" "+Thread.currentThread().getName();
        }catch (Exception e){
            return "Interrupted Happened in "+Thread.currentThread().getName();
        }
    }
}
