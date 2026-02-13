package com.nyzg.swiftsail.obj;


import java.util.List;

/*
{
  "sleepLines": [
    {
      "duration": 30720000,
      "endEpoch": 1770697590000,
      "mainSleep": true,
      "timeInBed": 34080000,
      "startEpoch": 1770663480000
    }
  ],
  "mainRemTime": 7380000,
  "mainDeepTime": 7320000,
  "mainEndEpoch": 1770697590000,
  "mainWakeTime": 3360000,
  "mainLightTime": 16020000,
  "mainStartEpoch": 1770663480000,
  "totalSleepTime": 30720000,
  "totalTimeInBed": 34080000
}
 */
public class MySleep {
    //总共的睡眠时间
    //如果你睡了很多次，这个就是总时间
    public long totalSleepTime;//exposeValue
    public long totalTimeInBed;
    public long mainStartEpoch;
    public long mainEndEpoch;
    public long mainDeepTime;
    public long mainLightTime;
    public long mainRemTime;
    public long mainWakeTime;
    public List<SleepLine> sleepLines;


    public static class SleepLine {
        public long startEpoch; //mySleep.setMainStartEpoch(startDateTime.atZone(ZONE_ID).toEpochSecond() * 1000L);
        public long endEpoch;//ZONE_ID=public static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
        public boolean isMainSleep;
        public long duration;
        public long timeInBed;
    }
}