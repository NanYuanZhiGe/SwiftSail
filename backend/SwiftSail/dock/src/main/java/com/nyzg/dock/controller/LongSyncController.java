package com.nyzg.dock.controller;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.SpringConfigurator;

@Component
@ServerEndpoint(
        value = "/ws/dock/websocket/report/sync",
        configurator = SpringConfigurator.class
)
@Slf4j
public class LongSyncController {
    @OnOpen
    public void webSocketTest(Session session, EndpointConfig config){

    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("message get: "+message);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

    }

    @OnError
    public void onError(Session session, Throwable throwable) {

    }
}