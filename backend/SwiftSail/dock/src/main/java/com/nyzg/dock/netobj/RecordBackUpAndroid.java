package com.nyzg.dock.netobj;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecordBackUpAndroid {
    long id;
    long userId;
    String recordId;
    long exposeValue;
    String detailValue;
    short sync;
    long epochDay;
    long epochWeek;
    long epochMonth;
    long epochYear;
}