package com.nyzg.swiftsail.bean;

import java.time.LocalDate;

public class DateUtils {

    public static final long MIN_CREATE_DATE=LocalDate.of(2025,11,1).toEpochDay();
    public static long getEpochWeek(long epochDay) {
        return 1L + (epochDay - 4L) / 7L;
    }

    public static long getEpochMonth(LocalDate date){
        return (date.getYear() - 1970) * 12L + (date.getMonthValue() - 1);
    }
    public static long getEpochYear(LocalDate nowDate){
        return nowDate.getYear()-1970;
    }
}
