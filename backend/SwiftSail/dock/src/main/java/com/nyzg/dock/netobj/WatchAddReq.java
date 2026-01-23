package com.nyzg.dock.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WatchAddReq {
    public long userId;
    public String clientId;
    public String name;
    public String watchType;
    public String authorizeHeader;
    public String codeVerifier;
    public String state;
}