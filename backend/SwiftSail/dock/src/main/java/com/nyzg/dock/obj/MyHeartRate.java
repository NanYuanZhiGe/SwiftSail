package com.nyzg.dock.obj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MyHeartRate {
    long low;//exposeValue
    long high;//exposeValue
    long rest;//exposeValue
    /*
    Out of Range
    Fat Burn
    Cardio
    Peak
     */
    float[] caloriesOut = new float[4];
}
