package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recordStepTable",
        indices = {
                @Index(value = {"userId", "epochDay", "exposeValue"}),
                @Index(value = {"userId", "epochYear", "epochMonth", "epochWeek", "exposeValue"}),
                @Index(value = {"userId", "recordId"})
        }
)
public class RecordStep {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public String recordId;
    public long exposeValue;
    public String detailValue;
    public long epochDay;
    public long epochWeek;
    public long epochMonth;
    public long epochYear;
}