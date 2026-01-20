package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface RecordConsumptionTable {
    @Query("SELECT `exposeValue` FROM `recordConsumptionTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    Long getExposeValue(long userId, long epochDay);
}
