package com.nyzg.dock.netobj;

import com.nyzg.dock.dbobj.Record;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class DayRecord {
    List<Record> recordList;

    /**
     * 默认构造有初始化
     */
    public DayRecord() {
        recordList = new ArrayList<>(6);
    }

    public DayRecord(List<Record> recordList) {
        this.recordList = recordList;
    }
}
