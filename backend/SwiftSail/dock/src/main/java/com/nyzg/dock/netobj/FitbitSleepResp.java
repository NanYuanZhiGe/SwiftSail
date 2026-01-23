package com.nyzg.dock.netobj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FitbitSleepResp {
    List<SleepRecord> sleep;
    Summary summary;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class SleepRecord {
        String dateOfSleep;// formatter yyyy-MM-dd
        int duration;
        int efficiency;
        String endTime;// formatter yyyy-MM-ddTHH:mm:ss.zzz
        int infoCode;
        @JsonProperty("isMainSleep")
        boolean isMainSleep;
        SleepRecordLevel levels;
        long logId;
        int minutesAfterWakeup;
        int minutesAsleep;
        int minutesAwake;
        int minutesToFallAsleep;
        String logType;
        String startTime;// formatter yyyy-MM-ddTHH:mm:ss.zzz
        int timeInBed;
        String type;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class SleepRecordLevel {
        List<SleepRecordLevelData> data;
        List<SleepRecordLevelData> shortData;
        SleepRecordLevelSummary summary;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class SleepRecordLevelSummary {
        SleepRecordLevelSummaryRecord deep;
        SleepRecordLevelSummaryRecord light;
        SleepRecordLevelSummaryRecord rem;
        SleepRecordLevelSummaryRecord wake;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class SleepRecordLevelSummaryRecord {
        int count;
        int minutes;
        int thirtyDayAvgMinutes;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class SleepRecordLevelData {
        String dateTime;// formatter yyyy-MM-ddTHH:mm:ss.zzz
        String level;
        int seconds;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Summary {
        SummaryStages stages;
        int totalMinutesAsleep;
        int totalSleepRecords;
        int totalTimeInBed;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class SummaryStages {
        int deep;
        int light;
        int rem;
        int wake;
    }
}