package com.nyzg.dock.netobj;


import com.nyzg.common.Pair;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AcquireSyncDataReq {
    List<Pair<Long, Long>> containedDataList;

    public AcquireSyncDataReq(List<Pair<Long, Long>> containedDataList) {
        this.containedDataList = containedDataList;
    }
}
