package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface RecordHeartRateTable {
    @Query("SELECT `exposeValue` FROM `recordHeartRateTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    Long getExposeValue(long userId,long epochDay);
}
