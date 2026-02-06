package com.nyzg.dock.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FitbitSleepRangeResp {
    List<FitbitSleepResp> sleep;
}
