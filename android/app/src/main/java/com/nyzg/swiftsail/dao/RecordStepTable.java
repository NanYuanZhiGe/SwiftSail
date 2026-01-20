package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface RecordStepTable {
    @Query("SELECT `exposeValue` FROM `recordStepTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    Long getExposeValue(long userId, long epochDay);
}
