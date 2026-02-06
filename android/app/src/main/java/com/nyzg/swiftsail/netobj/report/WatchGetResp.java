package com.nyzg.swiftsail.netobj.report;

import java.util.List;

public class WatchGetResp {
    public List<Watch> watchList;

    public WatchGetResp(){}

    public WatchGetResp(List<Watch> watchList) {
        this.watchList = watchList;
    }
}
