package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

import com.nyzg.swiftsail.dbobj.Watch;

@Dao
public interface WatchTable {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWatch(Watch watch);
}
