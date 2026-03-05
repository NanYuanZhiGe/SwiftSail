package com.nyzg.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateUtils {

    public static final ZoneOffset ZONE_OFFSET=ZoneOffset.of("Asia/Asia/Shanghai");

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
