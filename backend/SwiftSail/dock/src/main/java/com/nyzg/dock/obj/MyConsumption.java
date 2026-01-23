package com.nyzg.dock.obj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MyConsumption {
    //运动消耗的能量
    int activityCalories;
    //总消耗的能量
    int caloriesOut;//exposeValue
    int lightlyActiveMinutes;
    int fairlyActiveMinutes;
    int veryActiveMinutes;
}
