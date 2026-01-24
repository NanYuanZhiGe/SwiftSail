package com.nyzg.swiftsail.netobj.report;

import java.util.List;

public class WatchSyncCheckReq {
    public long userId;
    public List<String> clientIdList;
    public WatchSyncCheckReq(){}

    public WatchSyncCheckReq(long userId, List<String> clientIdList) {
        this.userId = userId;
        this.clientIdList = clientIdList;
    }
}
