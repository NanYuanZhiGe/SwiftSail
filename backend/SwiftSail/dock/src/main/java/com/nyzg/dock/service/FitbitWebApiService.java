package com.nyzg.dock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyzg.common.ss_utils.EncryptThreadSafe;
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

@Service
public class FitbitWebApiService {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public CompletableFuture<Optional<FitbitOath2Token>> getTokenAsync(
            @NonNull String clientId,
            @NonNull String clientSecret,
            @NonNull String code,
            @NonNull String codeVerifier) {
        HttpClient httpClient = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 7890))).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.fitbit.com/oauth2/token"))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + EncryptThreadSafe.transferStringToBase64EncodedString(clientId + ":" + clientSecret))
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

    public CompletableFuture<String> getHeartRateZones(
            @NonNull String accessToken,
            @NonNull String userId,
            @NonNull LocalDate date
    ) {
        HttpClient httpClient = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 7890))).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://api.fitbit.com/1/user/%s/activities/heart/date/%s/1d.json", userId, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/json")
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
}
