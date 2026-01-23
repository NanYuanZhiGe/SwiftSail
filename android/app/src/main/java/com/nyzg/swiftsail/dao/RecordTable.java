package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.nyzg.swiftsail.obj.Pair;

import java.util.List;

@Dao
public abstract class RecordTable {
    @Query("SELECT `exposeValue` FROM `recordTable` WHERE `userId`=:userId AND `type`=:type AND `epochDay`=:epochDay;")
    public abstract Long getExposeValue(long userId, String type, long epochDay);

    @Query("SELECT `exposeValue` FROM `recordTable` " +
            "WHERE `userId`=:userId AND `type`=:type AND `epochDay`>=:startDay AND `epochDay`<=:endDay " +
            "ORDER BY `epochDay`;")
    public abstract List<Long> getDayRangeExposeValue(long userId, String type, long startDay, long endDay);

    @Query("SELECT AVG(exposeValue) FROM `recordTable` " +
            "WHERE `userId`=:userId AND `type`=:type AND `epochWeek` BETWEEN :startWeek AND :endWeek " +
            "GROUP BY `epochWeek` ORDER BY `epochWeek`;")
    public abstract List<Double> getWeekRangeExposeAverage(long userId, String type, long startWeek, long endWeek);

    @Query("SELECT `epochDay` FROM `recordTable` " +
            "WHERE `userId`=:userId AND `type`=:type AND `epochWeek`>=:startWeek " +
            "ORDER BY `epochWeek` LIMIT 1;")
    public abstract Long getWeekOffset(long userId, String type, long startWeek);

    @Transaction
    public Pair<Long, List<Double>> getWeekRangeAndOffset(long userId, String type, long startWeek, long endWeek) {
        List<Double> queryList = getWeekRangeExposeAverage(userId, type, startWeek, endWeek);
        Long offset = getWeekOffset(userId, type, startWeek);
        return new Pair<>(offset, queryList);
    }

    @Query("SELECT AVG(exposeValue) FROM `recordTable` " +
            "WHERE `userId`=:userId AND `type`=:type AND `epochMonth`>=:startMonth AND `epochMonth`<=:endMonth " +
            "GROUP BY `epochMonth` ORDER BY `epochMonth`;")
    public abstract List<Double> getMonthRangeExposeAverage(long userId, String type, long startMonth, long endMonth);

    @Query("SELECT `epochDay` FROM `recordTable` " +
            "WHERE `userId`=:userId AND `type`=:type AND `epochMonth`>=:startMonth " +
            "ORDER BY `epochMonth` LIMIT 1;")
    public abstract Long getMonthOffset(long userId, String type, long startMonth);

    @Transaction
    public Pair<Long, List<Double>> getMonthRangeAndOffset(long userId, String type, long startMonth, long endMonth) {
        List<Double> queryList = getMonthRangeExposeAverage(userId, type, startMonth, endMonth);
        Long offset = getMonthOffset(userId, type, startMonth);
        return new Pair<>(offset, queryList);
    }

    @Query("SELECT AVG(exposeValue) FROM `recordTable` " +
            "WHERE `userId`=:userId AND `type`=:type " +
            "GROUP BY `epochYear` ORDER BY `epochYear`;")
    public abstract List<Double> getYearRangeExposeValueAverage(long userId, String type);

    @Query("SELECT `epochYear` FROM `recordTable` " +
            "WHERE `userId`=:userId AND `type`=:type " +
            "ORDER BY `epochYear` LIMIT 1;")
    public abstract Long getYearOffset(long userId, String type);

    @Transaction
    public Pair<Long, List<Double>> getYearRangeAndOffset(long userId, String type) {
        List<Double> queryList = getYearRangeExposeValueAverage(userId, type);
        Long offset = getYearOffset(userId, type);
        return new Pair<>(offset + 1970, queryList);
    }
}
