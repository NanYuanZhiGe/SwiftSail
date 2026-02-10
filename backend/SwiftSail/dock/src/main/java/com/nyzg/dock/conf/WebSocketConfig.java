package com.nyzg.dock.conf;

import com.nyzg.dock.controller.LongSyncController;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
public class WebSocketConfig {
    @Bean
    public WebSocketConfigurer webSocketConfigurer() {
        return new WebSocketConfigurer() {
            @Override
            public void registerWebSocketHandlers(@NotNull WebSocketHandlerRegistry registry) {
                registry.addHandler(new LongSyncController(), "/ws/dock/websocket/report/sync");
            }
        };
    }
}
