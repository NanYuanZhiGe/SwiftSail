package com.nyzg.swiftsail.dao;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.nyzg.swiftsail.dbobj.Record;

import java.util.List;

@Dao
public interface RecordTable {
    @Insert
    void insertRecord(Record record);

    @Query("UPDATE `recordTable` SET sync=1 WHERE `id` in (:ids) and `userId`=:userId;")
    void updateSync(List<String> ids,long userId);

    @Query("SELECT * FROM `recordTable` WHERE `userId`=:userId and `recordDate`=:today;")
    List<Record> selectTodayData(long today,long userId);

}
