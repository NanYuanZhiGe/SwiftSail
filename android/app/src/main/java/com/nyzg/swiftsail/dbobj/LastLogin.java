package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "lastLoginTable")
public class LastLogin {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;

    public LastLogin(){}
    @Ignore
    public LastLogin(long userId) {
        this.userId = userId;
    }
}