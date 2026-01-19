package com.nyzg.swiftsail.netobj.report;

public class WatchAddReq {
    public long userId;
    public String clientId;
    public String name;
    public String watchType;
    public String authorizeHeader;
    public String codeVerifier;
    public String state;

    public WatchAddReq() {
    }

    public WatchAddReq(
            long userId, String clientId,
            String name, String watchType,
            String authorizeHeader, String codeVerifier,
            String state) {
        this.userId = userId;
        this.clientId = clientId;
        this.name = name;
        this.watchType = watchType;
        this.authorizeHeader = authorizeHeader;
        this.codeVerifier = codeVerifier;
        this.state = state;
    }
}