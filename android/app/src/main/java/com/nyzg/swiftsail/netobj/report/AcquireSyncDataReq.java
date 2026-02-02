package com.nyzg.swiftsail.netobj.report;

import com.nyzg.swiftsail.obj.Pair;

import java.util.List;

public class AcquireSyncDataReq {
    public String clientId;
    public List<Pair<Long,Long>> leakDataList;

    public AcquireSyncDataReq(){}

    public AcquireSyncDataReq(String clientId, List<Pair<Long, Long>> leakDataList) {
        this.clientId = clientId;
        this.leakDataList = leakDataList;
    }
}
