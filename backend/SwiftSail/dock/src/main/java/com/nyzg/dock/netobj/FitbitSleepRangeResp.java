package com.nyzg.dock.netobj;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FitbitSleepRangeResp {
    @Nullable List<FitbitSleepResp> sleep;
}
