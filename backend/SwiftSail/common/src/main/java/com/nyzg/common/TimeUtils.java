package com.nyzg.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class TimeUtils {
    /**
     * 输入LocalDateTime，获取至1970年来的天数
     */
    public static long toEpochDays(LocalDateTime ldt) {
        // 将 LocalDateTime 转为带时区的时间（使用系统默认时区）
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        // 转为 Instant（UTC 时间戳）
        Instant instant = zdt.toInstant();
        // 计算从 1970-01-01T00:00:00Z 到该时刻的天数（向下取整）
        return ChronoUnit.DAYS.between(Instant.EPOCH, instant);
    }
}
