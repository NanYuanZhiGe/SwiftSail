package com.nyzg.geo.conf;

import io.minio.MinioClient;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConf {

    private final String mMinioUrl;
    private final String mAccessKey;
    private final String mSecretKey;

    public MinioConf(
            @Value("${minio.url}")
            String minioUrl,
            @Value("${minio.accessKey}")
            String accessKey,
            @Value("${minio.secretKey}")
            String secretKey) {
        mMinioUrl= minioUrl;
        mAccessKey = accessKey;
        mSecretKey = secretKey;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(mMinioUrl)
                .credentials(mAccessKey, mSecretKey)
                .build();
    }
}
