package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "watchTable",
        indices = {
                @Index(value = "clientId", unique = true)
        }
)
public class Watch {
    @PrimaryKey(autoGenerate = true)
    public long id;//主键，不要理
    public long userId;//和
    public String name;//用户给自己手表起的名字

    public String clientId;//手表的client_id
    public String type;//手表类型，比如fitbit
    public String authorizeHeader;//授权的头部信息，base64编码，client_id:client_secret
}