package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public abstract class ShareTable {

    @Query("SELECT `value` FROM `shareTable` WHERE `userId`=:userId AND `key`=:key")
    abstract public String getHeadIcon(long userId, String key);

    @Query("UPDATE `shareTable` SET `value`=:value WHERE `userId`=:userId AND `key`=:key")
    abstract public void updateHeadIcon(long userId, String key, String value);

    @Query("INSERT INTO `shareTable` (`userId`,`key`,`value`) VALUES(:userId,:key,:value)")
    abstract public void insert(long userId, String key, String value);

    @Transaction
    public void insertIfNotExist(long userId, String key, String value) {
        String val = getHeadIcon(userId, key);
        if (val == null) {
            insert(userId, key, value);
        } else {
            updateHeadIcon(userId, key, value);
        }
    }

}
