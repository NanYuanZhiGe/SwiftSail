package com.nyzg.geo.conf;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@EnableKafka
@Configuration
@Slf4j
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
    public ConsumerFactory<String, byte[]> consumerFactory2() {
        return getStringMapConsumerFactoryByte();
    }

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactory3() {
        return getStringMapConsumerFactoryByte();
    }

    @NotNull
    private <T> ConsumerFactory<String, T> getStringMapConsumerFactoryByte() {
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
                ByteArrayDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("container3")
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory3() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory3());
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setThreadNameSupplier(new Function<>() {
            final private AtomicInteger count = new AtomicInteger(0);
            @Override
            public String apply(MessageListenerContainer messageListenerContainer) {
                return "KafkaConsumer3-" + count.getAndIncrement();
            }
        });

        factory.setCommonErrorHandler(getDefaultErrorHandler());
        return factory;
    }

    @Bean("container2")
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory2() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory2());
        factory.setConcurrency(2);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setThreadNameSupplier(new Function<>() {
            final private AtomicInteger count = new AtomicInteger(0);

            @Override
            public String apply(MessageListenerContainer messageListenerContainer) {
                return "KafkaConsumer2-" + count.getAndIncrement();
            }
        });

        factory.setCommonErrorHandler(getDefaultErrorHandler());
        return factory;
    }

    @Bean("container1")
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
                (record, exception) -> {
                    log.error("Kafka分配消息有问题：", exception);
                    System.err.println("重试耗尽，: Topic=" + record.topic()
                            + ", Partition=" + record.partition()
                            + ", Offset=" + record.offset()
                            + ", Key=" + record.key()
                            + ", Error=" + exception.getMessage());
                },
                exponentialBackOff
        );
    }
}