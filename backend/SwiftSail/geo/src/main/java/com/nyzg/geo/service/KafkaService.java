package com.nyzg.geo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class KafkaService {
    @KafkaListener(topics = "ImageProcess", groupId = "imageGroup",concurrency = "2")
    public void listenGroupImage2(String message, Acknowledgment ack){
        log.info("Received Message in group imageGroup: " + message+" "+Thread.currentThread().getName());
        ack.acknowledge();
    }
}
