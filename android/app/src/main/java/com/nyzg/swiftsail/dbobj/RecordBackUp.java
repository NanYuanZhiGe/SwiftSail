package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recordBackUpTable",
        indices = {
                @Index(value = {"userId", "recordId", "sync"}),
        }
)
public class RecordBackUp {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public String recordId;
    public long exposeValue;
    public String detailValue;
    public short sync;
    public long epochDay;
    public long epochWeek;
    public long epochMonth;
    public long epochYear;
}