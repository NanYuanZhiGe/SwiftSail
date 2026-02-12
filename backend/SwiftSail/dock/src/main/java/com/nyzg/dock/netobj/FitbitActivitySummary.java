package com.nyzg.dock.netobj;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FitbitActivitySummary {
    @Nullable Summary summary;
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary{
        //由于运动消耗的能量
        int activityCalories;
        //总能量消耗
        int caloriesOut;
        List<HeartRateZones> heartRateZones;
        List<Distances> distances;
        //轻微活动时间时间
        int lightlyActiveMinutes;
        int fairlyActiveMinutes;
        //活跃活动时间
        int veryActiveMinutes;
        int restingHeartRate;
        //久坐时间
        int sedentaryMinutes;
        int steps;
        int floors;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeartRateZones{
        double caloriesOut;
        int max;
        int min;
        int minutes;
        /*
        Out of Range
        Fat Burn
        Cardio
        Peak
         */
        String name;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Distances{
        String activity;
        double distance;
    }
}
