package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nyzg.swiftsail.dbobj.Watch;

import java.util.List;

@Dao
public interface WatchTable {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWatch(Watch watch);

    @Query("DELETE FROM `watchTable` WHERE `id`=:id;")
    void deleteWatch(long id);

    @Query("SELECT * FROM `watchTable` WHERE `userId`=:userId;")
    List<Watch> selectWatchByUserId(long userId);

    @Query("SELECT * FROM `watchTable` WHERE `userId`=:userId AND `activate`=1 LIMIT 1;")
    Watch selectWatchActivated(long userId);

    @Query("SELECT `clientId` FROM `watchTable` WHERE `userId`=:userId AND `activate`=1 LIMIT 1;")
    String getActivateWatchClientId(long userId);

}
