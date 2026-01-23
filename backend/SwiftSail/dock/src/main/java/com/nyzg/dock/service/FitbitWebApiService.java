package com.nyzg.dock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyzg.dock.netobj.FitbitRefreshTokenResp;
import com.nyzg.dock.obj.FitbitOath2Token;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class FitbitWebApiService {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static ProxySelector PROXY_SELECTOR = ProxySelector.of(new InetSocketAddress("127.0.0.1", 7890));

    public CompletableFuture<Optional<FitbitOath2Token>> getTokenAsync(
            @NonNull String clientId,
            @NonNull String authorizeHeader,
            @NonNull String code,
            @NonNull String codeVerifier) {
        HttpClient httpClient = HttpClient.newBuilder().proxy(PROXY_SELECTOR).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.fitbit.com/oauth2/token"))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + authorizeHeader)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(String.format("client_id=%s&code=%s&code_verifier=%s&grant_type=authorization_code", clientId, code, codeVerifier)))
                .build();
        CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return future.thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return Optional.of(OBJECT_MAPPER.readValue(body, FitbitOath2Token.class));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                });
    }

    public void getRefreshToken(
            String authorizeHeader,
            String refreshToken,
            Consumer<String> onRequestFail,
            Consumer<FitbitRefreshTokenResp> onSuccess) {
        HttpClient httpClient = HttpClient.newBuilder().proxy(PROXY_SELECTOR).build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(" https://api.fitbit.com/oauth2/token"))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + authorizeHeader)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.format("grant_type=refresh_token&refresh_token=%s", refreshToken)
                ))
                .build();
        HttpResponse<String> resp;
        try {
            resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            onRequestFail.accept("无法和服务器建立连接，或者IO过程被打断");
            return;
        }
        if (resp.statusCode() == 400) {
            onRequestFail.accept("请求语法错误");
            return;
        } else if (resp.statusCode() == 401) {
            onRequestFail.accept("需要用户授权，或者是token过期，详情: " + resp.body());
            return;
        } else if (resp.statusCode() != 200) {
            onRequestFail.accept("请求失败，具体信息: " + resp.toString());
            return;
        }
        FitbitRefreshTokenResp tokenResp;
        try {
            tokenResp = OBJECT_MAPPER.readValue(resp.body(), FitbitRefreshTokenResp.class);
        } catch (Exception e) {
            onRequestFail.accept("解析服务端返回的数据出错");
            return;
        }
        onSuccess.accept(tokenResp);
    }
}
