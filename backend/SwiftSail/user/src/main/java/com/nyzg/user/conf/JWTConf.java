package com.nyzg.user.conf;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@Getter
public class JWTConf {
    byte[] hsKey;

    public JWTConf(@Value("${jwt.hs-key:hello-world}") String strHsKey) {
        this.hsKey = strHsKey.getBytes(StandardCharsets.UTF_8);
    }
}
