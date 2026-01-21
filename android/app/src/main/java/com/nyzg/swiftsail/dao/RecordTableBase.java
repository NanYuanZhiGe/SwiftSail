package com.nyzg.swiftsail.dao;

import com.nyzg.swiftsail.obj.Pair;

import java.util.List;

public abstract class RecordTableBase {
    public abstract Long getExposeValue(long userId, long epochDay);
    public abstract List<Long> getDayRangeExposeValue(long userId, long startDay, long endDay);
    public abstract Pair<Long, List<Double>> getWeekRangeAndOffset(long userId, long startWeek, long endWeek);
    public abstract Pair<Long, List<Double>> getMonthRangeAndOffset(long userId, long startMonth, long endMonth);
    public abstract Pair<Long, List<Double>> getYearRangeAndOffset(long userId);
}
