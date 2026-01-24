package com.nyzg.swiftsail.netobj.report;

public class WatchDeleteReq {
    public long userId;
    public String clientId;
    public WatchDeleteReq(){}

    public WatchDeleteReq(long userId, String clientId) {
        this.userId = userId;
        this.clientId = clientId;
    }
}