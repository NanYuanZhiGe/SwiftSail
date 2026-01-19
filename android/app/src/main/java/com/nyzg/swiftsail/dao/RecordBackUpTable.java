package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nyzg.swiftsail.dbobj.RecordBackUp;

import java.util.List;

@Dao
public interface RecordBackUpTable {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecordBackUp(RecordBackUp recordBackUp);

    @Query("SELECT * FROM `recordBackUpTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    List<RecordBackUp> selectTodayData(long epochDay, long userId);

    @Query("SELECT * FROM `recordBackUpTable` WHERE `sync`=0 AND `userId`=:userId;")
    List<RecordBackUp> selectAllUnSync(long userId);

    @Query("UPDATE `recordBackUpTable` SET `sync`=1 WHERE `recordId` IN (:recordIds);")
    void updateDataAsSync(List<String> recordIds);
}
