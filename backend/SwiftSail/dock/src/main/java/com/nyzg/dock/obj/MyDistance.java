package com.nyzg.dock.obj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MyDistance {
    double distance;//exposeValue
    double walkDistance;
    double lightlyActiveDistance;
    double moderateActiveDistance;
    double veryActiveDistance;
}
