package com.nyzg.dock.dbobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class RecordManual {
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

    public boolean isValid() {
        if (userId <= 0L ||
                type == null || type.isBlank() ||
                detailValue == null) {
            return false;
        }
        if (recordId == null) {
            recordId = UUID.randomUUID().toString();
        }
        return true;
    }

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