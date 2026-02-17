package com.nyzg.swiftsail.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nyzg.swiftsail.dbobj.RecordManual;

@Dao
public interface RecordManualTable {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecordManual(RecordManual recordManual);

    @Query("UPDATE `recordManualTable` SET `sync`=1 WHERE `userId`=:userId AND `type`=:type AND `recordId`=:recordId;")
    void updateSyncHasDone(long userId, @NonNull String type, @NonNull String recordId);
}
