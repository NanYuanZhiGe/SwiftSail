package com.nyzg.dock.netobj;

import com.nyzg.common.Pair;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AcquireSyncDataReq {
    String clientId;
    List<Pair<Long,Long>> leakDataList;
}
