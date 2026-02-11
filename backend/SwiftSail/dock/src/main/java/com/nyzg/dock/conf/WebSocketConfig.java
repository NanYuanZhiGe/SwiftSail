package com.nyzg.dock.conf;

import com.nyzg.dock.controller.LongSyncController;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.nio.charset.StandardCharsets;

@Configuration
public class WebSocketConfig {
    @Resource
    ObjectProvider<LongSyncController> objectProvider;

    @Bean
    public WebSocketConfigurer webSocketConfigurer() {
        return new WebSocketConfigurer() {
            @Override
            public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
                registry.addHandler(objectProvider.getObject(), "/ws/dock/websocket/report/sync");
            }
        };
    }
}
