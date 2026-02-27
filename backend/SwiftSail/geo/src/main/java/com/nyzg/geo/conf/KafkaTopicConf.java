package com.nyzg.geo.conf;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConf {
    public static final String TOPIC_IMAGE_PROCESS = "ImageProcess";
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapAddr;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> map = new HashMap<>();
        map.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapAddr);
        return new KafkaAdmin(map);
    }

    @Bean
    public NewTopic topicImg() {
        return new NewTopic(TOPIC_IMAGE_PROCESS, 2, (short) 1);
    }
}
