package com.nzyg.gateway.conf;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class LimitConf {
    @Bean
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.just("1");
    }
}
