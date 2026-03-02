package com.nyzg.geo.conf;

import io.minio.messages.DeleteObject;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaProducerConf {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapAddr;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> map = new HashMap<>();
        map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapAddr);
        map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        map.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 5 * 1024 * 1024);
        return new DefaultKafkaProducerFactory<>(map);
    }

    @Bean
    public ProducerFactory<String, byte[]> producerFactory2() {
        return getStringMapProducerFactoryByte();
    }

    @Bean
    public ProducerFactory<String, byte[]> producerFactory3() {
        return getStringMapProducerFactoryByte();
    }

    @NotNull
    private <T> ProducerFactory<String, T> getStringMapProducerFactoryByte() {
        Map<String, Object> map = new HashMap<>();
        map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapAddr);
        map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        map.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 64 * 1024 * 1024);
        return new DefaultKafkaProducerFactory<>(map);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean("ListDeleteObject")
    KafkaTemplate<String, byte[]> kafkaTemplateDelete() {
        return new KafkaTemplate<>(producerFactory2());
    }

    @Bean("MapAddObject")
    KafkaTemplate<String,byte[]> kafkaTemplateMapAdd() {
        return new KafkaTemplate<>(producerFactory3());
    }
}