package com.nyzg.geo.conf;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@EnableKafka
@Configuration
public class KafkaConsumerConf {

    public static final String GROUP_ID = "imageGroup";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapAddr;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootStrapAddr);
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(2);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setThreadNameSupplier(new Function<>() {
            final private AtomicInteger count = new AtomicInteger(0);

            @Override
            public String apply(MessageListenerContainer messageListenerContainer) {
                return "KafkaConsumer-" + count.getAndIncrement();
            }
        });

        factory.setCommonErrorHandler(getDefaultErrorHandler());
        return factory;
    }

    @NotNull
    private static DefaultErrorHandler getDefaultErrorHandler() {
        ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
        exponentialBackOff.setInitialInterval(1000L);
        exponentialBackOff.setMultiplier(2.0);
        exponentialBackOff.setMaxInterval(128000L);
        exponentialBackOff.setMaxAttempts(6);
        return new DefaultErrorHandler(
                (record, exception) -> System.err.println("重试耗尽，: Topic=" + record.topic()
                        + ", Partition=" + record.partition()
                        + ", Offset=" + record.offset()
                        + ", Key=" + record.key()
                        + ", Error=" + exception.getMessage()),
                exponentialBackOff
        );
    }
}