package com.nyzg.dock.dbobj;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Watch {
    long userId;
    String name;
    String clientId;
    String watchUserId;
    String watchType;
    String authorizeHeader;
    String codeVerifier;
    String accessToken;
    String refreshToken;
    long expireTime;//过期时间，不是28800，而是现实世界中的currentMillionSeconds
    int activate;//是否出于拉取数据的状态
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createTime;
}
