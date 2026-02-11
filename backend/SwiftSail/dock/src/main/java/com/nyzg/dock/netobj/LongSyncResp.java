package com.nyzg.dock.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class LongSyncResp {
    List<DayRecord> dayRecordList;

    public LongSyncResp(List<DayRecord> dayRecordList) {
        this.dayRecordList = dayRecordList;
    }
}
