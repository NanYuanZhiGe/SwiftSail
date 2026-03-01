package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "shareTable",
        indices = {
                @Index(unique = true, value = {"userId", "key"})
        }
)
public class Share {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public String key;
    public String value;
}
