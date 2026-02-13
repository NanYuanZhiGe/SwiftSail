package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recordTable",
        indices = {
                @Index(value = {"userId", "type", "epochDay"}, unique = true),
                @Index({"userId", "type", "epochDay", "exposeValue"}),
                @Index({"userId", "type", "epochWeek", "epochDay", "exposeValue"}),
                @Index({"userId", "type", "epochMonth", "epochDay", "exposeValue"}),
                @Index({"userId", "type", "epochYear", "epochDay", "exposeValue"}),
                @Index({"userId", "recordId"})
        }
)
public class Record {
    @PrimaryKey(autoGenerate = true)
    public long id;
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