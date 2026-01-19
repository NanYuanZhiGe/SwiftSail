package com.nyzg.swiftsail.dbobj;


import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "userTable")
public class User{
    @PrimaryKey
    public long id;
    public String nickName;
    public String email;

    public String token;
    public long createTime;
}