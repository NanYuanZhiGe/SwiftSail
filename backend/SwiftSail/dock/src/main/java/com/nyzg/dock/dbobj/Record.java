package com.nyzg.dock.dbobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Record {
    public long id;//auto gen
    public long userId;
    public String recordId;//uuid 36字符串由-分割
    public String type;
    public long exposeValue;
    public String detailValue;
    public long epochDay;
    public long epochWeek;
    public long epochMonth;
    public long epochYear;
}