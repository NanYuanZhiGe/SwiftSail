package com.nyzg.swiftsail.dbobj;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recordTable",
        indices = {
                @Index(value = "userId"),
                @Index(value = "recordDate"),
                @Index(value = "startTime"),
                @Index(value = "record")
        }
)
public class Record {
    @PrimaryKey
    @NonNull
    public byte[] id;
    public long userId;
    public String record;
    public long recordDate;//自1970年来的天数，避免时区问题
    public long startTime;//自1970年来的毫秒数，避免时区问题
    public byte[] sync;

    public long createTime;//自1970年来的毫秒数，避免时区问题
    public long modifyTime;//自1970年来的毫秒数，避免时区问题
}
