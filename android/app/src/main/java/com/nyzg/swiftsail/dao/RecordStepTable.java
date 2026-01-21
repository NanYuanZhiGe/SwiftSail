package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.nyzg.swiftsail.obj.Pair;

import java.util.List;

@Dao
public abstract class RecordStepTable extends RecordTableBase{
    @Query("SELECT `exposeValue` FROM `recordStepTable` WHERE `userId`=:userId AND `epochDay`=:epochDay;")
    public abstract Long getExposeValue(long userId, long epochDay);

    @Query("SELECT `exposeValue` FROM `recordStepTable` " +
            "WHERE `userId`=:userId AND `epochDay`>=:startDay AND `epochDay`<=:endDay " +
            "ORDER BY `epochDay`;")
    public abstract List<Long> getDayRangeExposeValue(long userId, long startDay, long endDay);

    @Query("SELECT AVG(exposeValue) FROM `recordStepTable` " +
            "WHERE `userId`=:userId AND `epochWeek` BETWEEN :startWeek AND :endWeek " +
            "GROUP BY `epochWeek` ORDER BY `epochWeek`;")
    public abstract List<Double> getWeekRangeExposeAverage(long userId, long startWeek, long endWeek);

    @Query("SELECT `epochDay` FROM `recordStepTable` " +
            "WHERE `userId`=:userId AND `epochWeek`>=:startWeek " +
            "ORDER BY `epochWeek` LIMIT 1;")
    public abstract Long getWeekOffset(long userId, long startWeek);

    @Transaction
    public Pair<Long, List<Double>> getWeekRangeAndOffset(long userId, long startWeek, long endWeek) {
        List<Double> queryList = getWeekRangeExposeAverage(userId, startWeek, endWeek);
        Long offset = getWeekOffset(userId, startWeek);
        return new Pair<>(offset, queryList);
    }

    @Query("SELECT AVG(exposeValue) FROM `recordStepTable` " +
            "WHERE `userId`=:userId AND `epochMonth`>=:startMonth AND `epochMonth`<=:endMonth " +
            "GROUP BY `epochMonth` ORDER BY `epochMonth`;")
    public abstract List<Double> getMonthRangeExposeAverage(long userId, long startMonth, long endMonth);

    @Query("SELECT `epochDay` FROM `recordStepTable` " +
            "WHERE `userId`=:userId AND `epochMonth`>=:startMonth " +
            "ORDER BY `epochMonth` LIMIT 1;")
    public abstract Long getMonthOffset(long userId, long startMonth);

    @Transaction
    public Pair<Long, List<Double>> getMonthRangeAndOffset(long userId, long startMonth, long endMonth) {
        List<Double> queryList = getMonthRangeExposeAverage(userId, startMonth, endMonth);
        Long offset = getMonthOffset(userId, startMonth);
        return new Pair<>(offset, queryList);
    }

    @Query("SELECT AVG(exposeValue) FROM `recordStepTable` " +
            "WHERE `userId`=:userId " +
            "GROUP BY `epochYear` ORDER BY `epochYear`;")
    public abstract List<Double> getYearRangeExposeValueAverage(long userId);

    @Query("SELECT `epochYear` FROM `recordStepTable` " +
            "WHERE `userId`=:userId " +
            "ORDER BY `epochYear` LIMIT 1;")
    public abstract Long getYearOffset(long userId);

    @Transaction
    public Pair<Long, List<Double>> getYearRangeAndOffset(long userId) {
        List<Double> queryList = getYearRangeExposeValueAverage(userId);
        Long offset = getYearOffset(userId);
        return new Pair<>(offset + 1970, queryList);
    }
}
