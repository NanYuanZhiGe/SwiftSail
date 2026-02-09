package com.nyzg.gateway.filter;

import com.nyzg.common.Pair;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Order(1)//控制执行顺序，越小越先执行
@Slf4j
public class JwtFilter implements GlobalFilter {
    byte[] hsKey;

    public JwtFilter(@Value("${jwt.hs-key:hello-world}") String strHsKey) {
        this.hsKey = strHsKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        //如果是登录相关的请求，直接放行
        if (path.startsWith("/api/user/login")
                || path.startsWith("/api/dock/oauth")
                || path.startsWith("/api/dock/ws/dock")) {
            log.info("放行" + request.getURI());
            return chain.filter(exchange);
        }
        //否则必须校验请求头
        HttpHeaders headers = request.getHeaders();
        Pair<JwtThreadSafe.Status, Map<String, Object>> statusMapPair;
        try {
            String jwt = headers.getFirst("jwt-token");
            statusMapPair = JwtThreadSafe.validateJwtTokenHMac(jwt, hsKey);
            //如果token为空或者token校验不过，不允许往后走
            if (jwt == null || statusMapPair.getA() != JwtThreadSafe.Status.SUCCESS) {
                log.info("no token: " + path);
                return noToken(exchange, request);
            }
        } catch (Exception e) {
            log.info("解析请求jwt-token请求头出错：" + e.getCause());
            return noToken(exchange, request);
        }
        //解析token，重写请求头
        String email = (String) statusMapPair.getB().get("email");
        //put的时候放进去的是long，hutool解析的时候会变成number with format
        //不要强转，调用toString就好了
        String userId = (statusMapPair.getB().get("userId")).toString();
        ServerHttpRequest newRequest = request
                .mutate()
                .header("email", email)
                .header("userId", userId)
                .build();
        exchange = exchange.mutate().request(newRequest).build();
        log.warn("放行" + request.getURI());
        return chain.filter(exchange);
    }

    private Mono<Void> noToken(ServerWebExchange exchange, ServerHttpRequest request) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String errorMsg = "{\"code\":401,\"message\":\"Missing token or token expired\"}";
        DataBuffer buffer = response.bufferFactory().wrap(errorMsg.getBytes());
        log.warn("禁止" + request.getURI());
        return response.writeWith(Mono.just(buffer)); // 返回 Mono<Void>
    }
}
