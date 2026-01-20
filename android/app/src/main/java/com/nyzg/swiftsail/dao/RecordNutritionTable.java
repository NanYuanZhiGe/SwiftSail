package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface RecordNutritionTable {
    @Query("SELECT `exposeValue` FROM `recordNutritionTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    Long getExposeValue(long userId,long epochDay);
}
