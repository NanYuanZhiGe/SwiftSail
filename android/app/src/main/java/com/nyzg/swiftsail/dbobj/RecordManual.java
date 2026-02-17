package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recordManualTable",
        indices = {
                @Index(value = {"userId", "type", "recordId"}, unique = true),
                @Index(value = {"epochYear", "epochMonth", "epochWeek", "epochDay", "startEpochSecond", "endEpochSecond", "exposeValue"}),
        }
)
public class RecordManual {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String recordId;
    public long userId;
    public String type;
    public long exposeValue;
    public String detailValue;
    public short sync;
    public long startEpochSecond;
    public long endEpochSecond;
    public long epochDay;
    public long epochWeek;
    public long epochMonth;
    public long epochYear;

    public RecordManual() {
    }

    @Ignore
    public RecordManual(
            short sync, String type, long userId, String recordId,
            long exposeValue, String detailValue,
            long startEpochSecond, long endEpochSecond,
            long epochDay, long epochWeek, long epochMonth, long epochYear) {
        this.recordId = recordId;
        this.detailValue = detailValue;
        this.endEpochSecond = endEpochSecond;
        this.epochDay = epochDay;
        this.epochMonth = epochMonth;
        this.epochWeek = epochWeek;
        this.epochYear = epochYear;
        this.exposeValue = exposeValue;
        this.startEpochSecond = startEpochSecond;
        this.sync = sync;
        this.type = type;
        this.userId = userId;
    }
}