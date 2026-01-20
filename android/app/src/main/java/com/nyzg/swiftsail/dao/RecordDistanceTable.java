package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface RecordDistanceTable {
    @Query("SELECT `exposeValue` FROM `recordDistanceTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    Long getExposeValue(long userId,long epochDay);
}
