package com.nyzg.swiftsail.netobj.report;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

public class Watch {
    public long userId;
    public String name;
    public String clientId;
    public String watchUserId;
    public String watchType;
    public String authorizeHeader;
    public String codeVerifier;
    public String accessToken;
    public String refreshToken;
    public long expireTime;//过期时间，不是28800，而是现实世界中的currentMillionSeconds
    public int activate;//是否出于拉取数据的状态
    public LocalDateTime createTime;
}