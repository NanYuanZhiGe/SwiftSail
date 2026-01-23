package com.nyzg.dock.obj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MySleep {
    //总共的睡眠时间
    long totalSleepTime;//exposeValue
    long totalTimeInBed;
    long mainStartEpoch;
    long mainEndEpoch;
    long mainDeepTime;
    long mainLightTime;
    long mainRemTime;
    long mainWakeTime;
    List<SleepLine> sleepLines;

    @Data
    @NoArgsConstructor
    public static class SleepLine {
        long startEpoch;
        long endEpoch;
        boolean isMainSleep;
        long duration;
        long timeInBed;
    }
}