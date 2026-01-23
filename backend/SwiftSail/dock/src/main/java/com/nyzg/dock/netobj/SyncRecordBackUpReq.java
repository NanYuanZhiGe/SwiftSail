package com.nyzg.dock.netobj;

import com.nyzg.dock.netobj.RecordBackUpAndroid;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SyncRecordBackUpReq {
    List<RecordBackUpAndroid> recordList;
}