package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface RecordSleepTable {
    @Query("SELECT `exposeValue` FROM `recordSleepTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    Long getExposeValue(long userId,long epochDay);
}
