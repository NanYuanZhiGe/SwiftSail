package com.nyzg.dock.dbobj;

import lombok.Data;
import lombok.NoArgsConstructor;

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
}
