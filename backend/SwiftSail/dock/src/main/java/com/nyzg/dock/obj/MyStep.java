package com.nyzg.dock.obj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MyStep {
    int steps;//exposeValue;
    //爬楼步数
    int floors;
    //久坐时间
    int sedentaryMinutes;
}
